import base64
from base64 import b64decode
from datetime import datetime
from pathlib import Path
from unittest import TestCase

import jcs
import requests
import json

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives._serialization import Encoding, PrivateFormat, NoEncryption
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509 import load_pem_x509_certificate
from prov.constants import PROV_TYPE, PROV_ATTR_BUNDLE, PROV_ATTR_SPECIFIC_ENTITY, \
    PROV_ATTR_GENERAL_ENTITY, PROV_ATTR_ACTIVITY, PROV_ATTR_ENTITY, PROV_ATTR_AGENT, PROV_ATTR_GENERATED_ENTITY, \
    PROV_ATTR_USED_ENTITY
from prov.identifier import Namespace, QualifiedName
from prov.model import ProvDocument, ProvBundle, ProvAttribution, ProvSpecialization, \
    ProvAgent, ProvActivity, ProvUsage, ProvAssociation, ProvGeneration, ProvDerivation, first, ProvInfluence, \
    ProvEntity
from prov.serializers import provjson, provn

import provenance_tests.helpers as helpers
from constants import CPM_BACKWARD_CONNECTOR, CPM_FORWARD_CONNECTOR, CPM_MAIN_ACTIVITY, PAV_VERSION, PAV, \
    CPM_TRUSTED_PARTY, CPM_TOKEN, CPM_TOKEN_GENERATION, CPM, CPM_REFERENCED_META_BUNDLE_ID, CPM_REFERENCED_BUNDLE_ID, \
    CPM_REFERENCED_BUNDLE_HASH_VALUE, CPM_HASH_ALG, CPM_SENDER_AGENT, CPM_HAS_ID, CPM_ID, CPM_EXTERNAL_ID, \
    CPM_EXTERNAL_ID_TYPE
from certificate_helpers import generate_certificate, parse_certificate
from provenance_integration_tests.api_test_helpers import provenance_storage_hospital_name, fqdn_pathology, \
    provenance_storage_pathology_name, provenance_storage_hospital_name, fqdn_hospital, trusted_party_url, \
    provenance_storage_pathology_url, provenance_storage_hospital_url, create_json_for_doc_storing, TestDataCreator, \
    register_org1_to_hospital_storage, register_org_2_to_pathology_storage


class MyTestCase(TestCase):
    prov_data = TestDataCreator()

    # these tests should run after each other at once - they depend on data written to the running applications

    @classmethod
    def setUpClass(cls):
        # register org, upload first document... - as in basic tests - cpm depend on it  - connectors reference basic saved documents
        register_org1_to_hospital_storage(cls.prov_data)
        json_data = create_json_for_doc_storing(cls.prov_data.doc)

        requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{cls.prov_data.org_id}/documents/test_{cls.prov_data.timestamp}_bundle",
            json.dumps(json_data))
        register_org_2_to_pathology_storage(cls.prov_data)

        json_data = create_json_for_doc_storing(cls.prov_data.doc2, private_key_location='test_sign2.pem')

        requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{cls.prov_data.org_id2}/documents/test_{cls.prov_data.timestamp}_bundle_w_backward_connector",
            json.dumps(json_data))


    def test_130store_graph_with_backbone_with_concrete_forward_connector_ok(
            self):

        json_data = create_json_for_doc_storing(self.prov_data.doc_fw)

        res = requests.put(
            # put - im basically updating the basic document according to bacward connector form second doc
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_with_forward_connector")
        self.assertEqual(self.prov_data.cpm_bundle_info_forward_conn_doc[2], token["data"]["documentDigest"])
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_131_retrieve_graph_with_backbone_with_forward_connector_ok(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector")

        self.assertEqual(200, res.status_code)
        json_data = json.loads(res.content)

        doc_returned = b64decode(json_data["document"])
        prov_doc = ProvDocument.deserialize(
            content=doc_returned.decode("utf-8"), format="json"
        )
        self.assertEqual(doc_returned, jcs.canonicalize(provjson.encode_json_document(self.prov_data.doc_fw)))
        self.assertEqual(self.prov_data.doc_fw, prov_doc)

        token = json_data["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_with_forward_connector")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_132retrieve_domain_from_graph_with_forward_connector_ok(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector/domain-specific",
            {"format": "json"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["document"].encode("utf-8")), format="json"
        )

        # check that no backbone records are in result
        bundle_returned = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            bundle_returned = b
        bundle_self = ProvBundle()
        for b in self.prov_data.doc_fw.bundles:
            bundle_self = b

        for record in bundle_self.records:

            if record not in self.prov_data.backbone_basic_w_forward:
                self.assertTrue(record in bundle_returned.records)
            else:
                self.assertTrue(record not in bundle_returned.records)

        # test namespaces
        self.assertTrue(CPM in bundle_returned.namespaces)
        self.assertTrue(next(iter(self.prov_data.doc_fw.namespaces)) in bundle_returned.namespaces)

        # test token
        token = result["token"]
        self.check_token(result, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_with_forward_connector")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    # test second time - app saves them...
    def test_133_retrieve_domain_from_graph_with_forward_connector_2nd_ok(self):
        self.test_132retrieve_domain_from_graph_with_forward_connector_ok()

    def test_134_retrieve_backbone_from_graph_with_forward_connector_ok(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector/backbone",
            {"format": "json"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["document"].encode("utf-8")), format="json"
        )

        bundle = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            bundle = b

        bundle_self = ProvBundle()
        for b in self.prov_data.doc_fw.bundles:
            bundle_self = b
        # check that all the records from backbone are returned and no more
        for record in bundle_self.records:
            if record in self.prov_data.backbone_basic_w_forward:
                self.assertTrue(record in bundle.records)
            else:
                self.assertTrue(record not in bundle.records)

        # test namespaces
        self.assertTrue(CPM in bundle.namespaces)
        self.assertTrue(Namespace(
            provenance_storage_hospital_name,
            f"http://prov-storage-{provenance_storage_hospital_name}:8000/api/v1/organizations/{self.prov_data.org_id}/documents/",
        ) in bundle.namespaces)

        # test token
        token = result["token"]
        self.check_token(result, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_with_forward_connector")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    # tests regarding multiple connectors:
    def test_1390_store_graph_with_backbone_end_ok(
            self):
        json_data = create_json_for_doc_storing(self.prov_data.doc_end)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_end",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_end")
        self.assertEqual(self.prov_data.last_cpm_bundle_info[2], token["data"]["documentDigest"])
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

        # test retrieval of document
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_end")

        self.assertEqual(200, res.status_code)
        json_data_returned = json.loads(res.content)

        doc_returned = b64decode(json_data_returned["document"])
        prov_doc = ProvDocument.deserialize(
            content=doc_returned.decode("utf-8"), format="json"
        )
        self.assertEqual(doc_returned, jcs.canonicalize(provjson.encode_json_document(self.prov_data.doc_end)))
        self.assertEqual(self.prov_data.doc_end, prov_doc)

        token = json_data_returned["token"]
        self.check_token(json_data_returned, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_end")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_13910_store_graph_with_backbone_with_both_connectors_ok(
            self):
        # store document, asserts
        json_data = create_json_for_doc_storing(self.prov_data.doc_with_both_connectors,
                                                private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_both_connectors",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id2, provenance_storage_pathology_name,
                         "_w_both_connectors")
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

        # test retrieval of document
        res = requests.get(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_both_connectors")

        self.assertEqual(200, res.status_code)
        json_data_returned = json.loads(res.content)

        doc_returned = b64decode(json_data_returned["document"])
        prov_doc = ProvDocument.deserialize(
            content=doc_returned.decode("utf-8"), format="json"
        )
        self.assertEqual(doc_returned,
                         jcs.canonicalize(provjson.encode_json_document(self.prov_data.doc_with_both_connectors)))
        self.assertEqual(self.prov_data.doc_with_both_connectors, prov_doc)

        token = json_data_returned["token"]
        self.check_token(json_data_returned, token, self.prov_data.org_id2, provenance_storage_pathology_name,
                         bundle_suffix="_w_both_connectors")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_13911_retrieve_backbone_from_graph_with_both_connectors_ok(self):
        res = requests.get(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_both_connectors/backbone",
            {"format": "json"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["document"].encode("utf-8")), format="json"
        )

        bundle = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            bundle = b

        bundle_self = ProvBundle()
        for b in self.prov_data.doc_with_both_connectors.bundles:
            bundle_self = b
        # check that all the records from backbone are returned and no more
        for record in bundle_self.records:
            if record in self.prov_data.backbone_with_both_connectors:
                self.assertTrue(record in bundle.records)
            else:
                self.assertTrue(record not in bundle.records)

        # test namespaces
        self.assertTrue(CPM in bundle.namespaces)
        self.assertTrue(Namespace(
            provenance_storage_pathology_name,
            f"http://prov-storage-{provenance_storage_pathology_name}:8000/api/v1/organizations/{self.prov_data.org_id2}/documents/",
        ) in bundle.namespaces)

        # test token
        token = result["token"]
        self.check_token(result, token, self.prov_data.org_id2, provenance_storage_pathology_name,
                         bundle_suffix="_w_both_connectors")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_1392_store_graph_with_backbone_end_ok_update_add_backward_connectors(
            self):
        json_data = create_json_for_doc_storing(self.prov_data.document_with_backwards_connector_updated)

        res = requests.put(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_end",
            json.dumps(json_data))

        # asserts
        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_end_updated")
        self.assertEqual(self.prov_data.last_cpm_bundle_info_3[2], token["data"]["documentDigest"])
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )
        # test retrieval of document
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_end_updated")

        self.assertEqual(200, res.status_code)
        json_data_returned = json.loads(res.content)

        doc_returned = b64decode(json_data_returned["document"])
        prov_doc = ProvDocument.deserialize(
            content=doc_returned.decode("utf-8"), format="json"
        )
        self.assertEqual(doc_returned, jcs.canonicalize(provjson.encode_json_document(self.prov_data.document_with_backwards_connector_updated)))
        self.assertEqual(self.prov_data.document_with_backwards_connector_updated, prov_doc)

        token = json_data_returned["token"]
        self.check_token(json_data_returned, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_end_updated")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_1393_store_graph_with_backbone_first_ok_update_add_two_forward_connectors(
            self):
        json_data = create_json_for_doc_storing(self.prov_data.doc_two_forward_conns)

        res = requests.put(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector",
            json.dumps(json_data))

        # asserts
        print(res.content)
        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_with_both_forward_connectors")
        self.assertEqual(self.prov_data.first_with_two_conns_cpm_bundle_info[2], token["data"]["documentDigest"])
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )
        # test retrieval of document
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_both_forward_connectors")

        self.assertEqual(200, res.status_code)
        json_data_returned = json.loads(res.content)

        doc_returned = b64decode(json_data_returned["document"])
        prov_doc = ProvDocument.deserialize(
            content=doc_returned.decode("utf-8"), format="json"
        )
        self.assertEqual(doc_returned, jcs.canonicalize(provjson.encode_json_document(self.prov_data.doc_two_forward_conns)))
        self.assertEqual(self.prov_data.doc_two_forward_conns, prov_doc)

        token = json_data_returned["token"]
        self.check_token(json_data_returned, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_with_both_forward_connectors")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_1400_store_graph_with_basic_backbone_ok_added_external_id(
            self):
        # create document - needs id different from previous
        doc2, bundle, _, _, _, _ = helpers.create_cpm_provenance_basic(self.prov_data.existential_variable_prefix,
                                                                       provenance_storage_pathology_name, self.prov_data.org_id2, self.prov_data.timestamp,
                                                                       self.prov_data.main_activity_attributes_pathology_meta, bundle_suffix="_with_ids")

        # retrieve entities form backbone parts, add them ids, test save
        # entity 2 derived from entity 1
        entity_1 = first(bundle.get_record(f"{provenance_storage_pathology_name}:e001"))
        entity_2 = first(bundle.get_record(f"{provenance_storage_pathology_name}:e002"))

        identifier_entity_2 = bundle.entity(f"{provenance_storage_pathology_name}:external_id_derived_entity",
                                                       other_attributes={PROV_TYPE: CPM_ID,
                                                                         CPM_EXTERNAL_ID:
                                                                             "pathology:external_id",
                                                                         CPM_EXTERNAL_ID_TYPE: "example:id_type"})
        identifier_entity_2.alternateOf(entity_2)
        identifier_entity_1 = bundle.entity(f"{provenance_storage_pathology_name}:external_id_used_entity",
                                                        other_attributes={PROV_TYPE: CPM_ID,
                                                                          CPM_EXTERNAL_ID:
                                                                              self.prov_data.remote_bundle_namespace[
                                                                                  "external_id_2"],
                                                                          CPM_EXTERNAL_ID_TYPE: "example:id_type"})
        identifier_entity_1.alternateOf(entity_1)
        identifier_entity_1.alternateOf(entity_2)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_with_ids",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id2, provenance_storage_pathology_name,
                         "_with_ids")
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def check_token(self, json_data, token, org_id, storage_name, bundle_suffix=""):
        self.assertEqual(org_id, token["data"]["originatorId"])
        self.assertEqual("Trusted_Party", token["data"]["authorityId"])
        self.assertIsNotNone(token["data"]["tokenTimestamp"])
        self.assertTrue(datetime.timestamp(datetime.now()) > token["data"]["tokenTimestamp"])
        digest = hashes.Hash(hashes.SHA256())
        digest.update(base64.b64decode(json_data["document"]))
        hash = digest.finalize().hex()
        self.assertEqual(hash, token["data"]["documentDigest"])
        self.assertEqual(
            f"http://prov-storage-{storage_name}:8000/api/v1/organizations/{org_id}/documents/test_{self.prov_data.timestamp}_bundle{bundle_suffix}",
            token["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", token["data"]["additionalData"]["hashFunction"])
        self.assertEqual("trusted-party:8020", token["data"]["additionalData"]["trustedPartyUri"])
        self.assertIsNotNone(token["data"]["additionalData"]["trustedPartyCertificate"])
        self.assertIsNotNone(token["signature"])
