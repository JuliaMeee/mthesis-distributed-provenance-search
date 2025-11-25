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
from prov.serializers import provjson

from helpers import *
from provenance.constants import CPM_BACKWARD_CONNECTOR, CPM_FORWARD_CONNECTOR, CPM_MAIN_ACTIVITY, PAV_VERSION, PAV, \
    CPM_TRUSTED_PARTY, CPM_TOKEN, CPM_TOKEN_GENERATION, CPM, CPM_REFERENCED_META_BUNDLE_ID, CPM_REFERENCED_BUNDLE_ID, \
    CPM_REFERENCED_BUNDLE_HASH_VALUE, CPM_HASH_ALG, CPM_SENDER_AGENT, CPM_HAS_ID, CPM_ID, CPM_EXTERNAL_ID, \
    CPM_EXTERNAL_ID_TYPE
from certificate_helpers import generate_certificate, parse_certificate
from api_test_helpers import provenance_storage_hospital_name, fqdn_pathology, \
    provenance_storage_pathology_name, provenance_storage_hospital_name, fqdn_hospital, trusted_party_url, \
    provenance_storage_pathology_url, provenance_storage_hospital_url, create_json_for_doc_storing, TestDataCreator, \
    register_org_to_storage


class MyTestCase(TestCase):
    prov_data = TestDataCreator()

    # these tests should run after each other at once - they depend on data written to the running applications

    def test_010register_org_1_ok(self):
        res = register_org_to_storage(self.prov_data.org_id, provenance_storage_hospital_url)
        self.assertEqual(201, res.status_code)

    def test_011_check_org_registerd_in_tp(self):
        res = requests.get("http://" + trusted_party_url + "/api/v1/organizations/" + self.prov_data.org_id + "/certs")
        self.assertEqual(200, res.status_code)

        with open('test_cert.pem', 'r') as file:
            org_certificate = file.read()

        result = json.loads(res.content)
        self.assertIsNotNone(result["certificate"])
        self.assertEqual(org_certificate, result["certificate"])

    def test_0500_store_graph_with_basic_backbone_ok(
            self):
        json_data = create_json_for_doc_storing(self.prov_data.doc)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name)
        self.assertEqual(self.prov_data.first_cpm_bundle_info[2], token["data"]["documentDigest"])
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_0501_store_graph_with_basic_backbone_ok_in_rdf(
            self):
        # this needs to have different meta bundle id to work, and different document id, since same doc cannot be saved twice
        main_activity_attributes_ok = {
            PROV_TYPE: CPM_MAIN_ACTIVITY,
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.meta_ns[
                # id of meta bundle - used when requesting meta provenance
                f"test_{self.prov_data.timestamp}_bundle_meta_rdf"
            ],
        }
        doc, bundle, meta_bundle_id, backbone_basic, prev_cpm_bundle_info, _ = create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=main_activity_attributes_ok,
            timestamp=self.prov_data.timestamp, bundle_suffix="_rdf", smaller_prov=False)
        document_rdf = doc.serialize(format="rdf").encode("utf-8")
        json_data = create_json_for_doc_storing(document_rdf, doc_format="rdf", different_format=True)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_rdf",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_rdf")
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

        res2 = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_rdf")

        self.assertEqual(200, res2.status_code)
        json_data = json.loads(res2.content)

        doc_returned = b64decode(json_data["document"])
        prov_doc = ProvDocument.deserialize(
            content=doc_returned.decode("utf-8"), format="rdf"
        )
        self.assertEqual(doc_returned, document_rdf)
        self.assertEqual(doc, prov_doc)

        token2 = json_data["token"]
        self.assertEqual(token, token2)

    def test_0502_store_graph_with_basic_backbone_ok_in_xml(
            self):
        # needed to create document with namespaces used in all the entities
        # this needs to have different meta bundle id to work, and different document id, since same doc cannot be saved twice
        main_activity_attributes_ok = {
            PROV_TYPE: CPM_MAIN_ACTIVITY,
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.meta_ns[
                # id of meta bundle - used when requesting meta provenance
                f"test_{self.prov_data.timestamp}_bundle_meta_xml"
            ],
        }
        doc, bundle, meta_bundle_id, backbone_basic, prev_cpm_bundle_info, _ = create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=main_activity_attributes_ok,
            timestamp=self.prov_data.timestamp, bundle_suffix="_xml", smaller_prov=False)
        document_xml = doc.serialize(format="xml").encode("utf-8")
        json_data = create_json_for_doc_storing(document_xml, doc_format="xml", different_format=True)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_xml",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_xml")
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )
        res2 = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_xml")

        self.assertEqual(200, res2.status_code)
        json_data = json.loads(res2.content)

        doc_returned = b64decode(json_data["document"])
        prov_doc = ProvDocument.deserialize(
            content=doc_returned.decode("utf-8"), format="xml"
        )
        self.assertEqual(doc_returned, document_xml)
        self.assertEqual(doc, prov_doc)

        token2 = json_data["token"]
        self.assertEqual(token, token2)

    def test_06retrieve_graph_basic(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle")

        self.assertEqual(200, res.status_code)
        json_data = json.loads(res.content)

        doc_returned = b64decode(json_data["document"])
        prov_doc = ProvDocument.deserialize(
            content=doc_returned.decode("utf-8"), format="json"
        )
        self.assertEqual(doc_returned, jcs.canonicalize(provjson.encode_json_document(self.prov_data.doc)))
        self.assertEqual(self.prov_data.doc, prov_doc)

        token = json_data["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name)
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_07retrieve_graph_basic_domain(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle/domain-specific",
            {"format": "json"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["document"].encode("utf-8")), format="json"
        )

        # check that no backbone records are in result
        bundle = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            bundle = b
        bundle_self = ProvBundle()
        for b in self.prov_data.doc.bundles:
            bundle_self = b

        for record in bundle_self.records:
            if record not in self.prov_data.backbone_basic:
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
        self.check_token(result, token, self.prov_data.org_id, provenance_storage_hospital_name)
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_080_retrieve_graph_basic_backbone(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle/backbone",
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
        for b in self.prov_data.doc.bundles:
            bundle_self = b
        # check that all the records from backbone are returned and no more
        for record in bundle_self.records:
            if record in self.prov_data.backbone_basic:
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
        self.check_token(result, token, self.prov_data.org_id, provenance_storage_hospital_name)
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_081_retrieve_graph_basic_backbone_in_rdf(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle/backbone",
            {"format": "rdf"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["document"].encode("utf-8")), format="rdf"
        )

        bundle = ProvBundle()
        # ProvRecord()
        for b in prov_doc_returned_deserialized.bundles:
            bundle = b

        bundle_self = ProvBundle()
        for b in self.prov_data.doc.bundles:
            bundle_self = b
        # check that all the records from backbone are returned and no more
        for record in bundle_self.records:
            if record in self.prov_data.backbone_basic:
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
        self.check_token(result, token, self.prov_data.org_id,
                         provenance_storage_hospital_name)
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_082_retrieve_graph_basic_backbone_in_xml(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle/backbone",
            {"format": "xml"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["document"].encode("utf-8")), format="xml"
        )

        bundle = ProvBundle()
        # ProvRecord()
        for b in prov_doc_returned_deserialized.bundles:
            bundle = b

        bundle_self = ProvBundle()
        for b in self.prov_data.doc.bundles:
            bundle_self = b
        # check that all the records from backbone are returned and no more
        for record in bundle_self.records:
            if record in self.prov_data.backbone_basic:
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
        self.check_token(result, token, self.prov_data.org_id,
                         provenance_storage_hospital_name)
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_090store_graph_with_backbone_with_backward_connector_ok(
            self):
        # firstly, register second org, to different storage
        res = register_org_to_storage(self.prov_data.org_id2, provenance_storage_pathology_url,
                                      cert_location="./test_cert2.pem", key_location="./test_sign2.pem")
        self.assertEqual(201, res.status_code)

        # store document
        json_data = create_json_for_doc_storing(self.prov_data.doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id2, provenance_storage_pathology_name,
                         "_w_backward_connector")
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_091retrieve_graph_with_backward_connector(self):
        res = requests.get(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector")

        self.assertEqual(200, res.status_code)
        json_data = json.loads(res.content)

        doc_returned = b64decode(json_data["document"])
        prov_doc = ProvDocument.deserialize(
            content=doc_returned.decode("utf-8"), format="json"
        )
        self.assertEqual(doc_returned, jcs.canonicalize(provjson.encode_json_document(self.prov_data.doc2)))
        self.assertEqual(self.prov_data.doc2, prov_doc)

        token = json_data["token"]
        self.check_token(json_data, token, self.prov_data.org_id2, provenance_storage_pathology_name,
                         bundle_suffix="_w_backward_connector")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_092retrieve_domain_from_graph_with_backward_connector(self):
        res = requests.get(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector/domain-specific",
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
        for b in self.prov_data.doc2.bundles:
            bundle_self = b

        for record in bundle_self.records:
            if record not in self.prov_data.backbone_with_backwards_connector:
                self.assertTrue(record in bundle_returned.records)
            else:
                self.assertTrue(record not in bundle_returned.records)

        # test namespaces
        self.assertTrue(CPM in bundle_returned.namespaces)
        self.assertTrue(Namespace(
            "pathology",
            f"http://prov-storage-{provenance_storage_pathology_name}:8000/api/v1/organizations/{self.prov_data.org_id2}/documents/",
        ) in bundle_returned.namespaces)

        # test token
        token = result["token"]
        self.check_token(result, token, self.prov_data.org_id2, provenance_storage_pathology_name,
                         bundle_suffix="_w_backward_connector")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    # test second time - app saves them...
    def test_093retrieve_domain_from_graph_with_backward_connector_2nd(self):
        self.test_092retrieve_domain_from_graph_with_backward_connector()

    def test_094retrieve_backbone_from_graph_with_backward_connector(self):
        res = requests.get(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector/backbone",
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
        for b in self.prov_data.doc2.bundles:
            bundle_self = b
        # check that all the records from backbone are returned and no more
        for record in bundle_self.records:
            if record in self.prov_data.backbone_with_backwards_connector:
                self.assertTrue(record in bundle.records)
            else:
                self.assertTrue(record not in bundle.records)

        # test namespaces
        self.assertTrue(CPM in bundle.namespaces)
        self.assertTrue(Namespace(
            "pathology",
            f"http://prov-storage-{provenance_storage_pathology_name}:8000/api/v1/organizations/{self.prov_data.org_id2}/documents/",
        ) in bundle.namespaces)

        # test token
        token = result["token"]
        self.check_token(result, token, self.prov_data.org_id2, provenance_storage_pathology_name,
                         bundle_suffix="_w_backward_connector")
        self.assertIsNotNone(token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    # test second time - app saves them - test whether ok
    def test_095retrieve_backbone_from_graph_with_backwards_connector_2nd(self):
        self.test_094retrieve_backbone_from_graph_with_backward_connector()

    def test_10retrieve_meta_basic(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/documents/meta/test_{self.prov_data.timestamp}_bundle_meta",
            {"format": "json"})
        self.assertEqual(self.prov_data.remote_meta_bundle_namespace[
                             self.prov_data.first_meta_bundle_info[1]
                         ].localpart, f"test_{self.prov_data.timestamp}_bundle_meta")

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["graph"].encode("utf-8")), format="json"
            # json assigns default namespace to entity byt what about rdf?
        )

        # test token of response
        self.check_meta_response_token(result)

        # retrieve bundle from returned doc
        meta_bundle_returned = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            meta_bundle_returned = b
        meta_records = meta_bundle_returned.records

        # test general and concrete entity - create prov and check against it - attributes...
        document = ProvDocument()
        bundle_namespace = Namespace(
            "meta",
            f"prov-storage-{provenance_storage_hospital_name}:8000/api/v1/documents/meta/",
        )

        bundle_identifier = f"{self.prov_data.org_id}_test_{self.prov_data.timestamp}_bundle"
        test_bundle = document.bundle(bundle_namespace[bundle_identifier])
        for ns in meta_bundle_returned.namespaces:
            test_bundle.add_namespace(ns)
        meta_org_namespace = Namespace(
            self.prov_data.org_id,
            fqdn_hospital + f"/api/v1/organizations/{self.prov_data.org_id}/graphs/",
        )
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 1})
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_bundle_gen"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE)})

        first_doc = None
        gen_doc = None
        for record in test_bundle.records:
            self.assertTrue(record in meta_records)
            if record._identifier == QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"):
                first_doc = record
            if record._identifier.localpart.split("_")[-1] == "gen":
                gen_doc = record

        # check specialization relation
        specializations = list(meta_bundle_returned.get_records(ProvSpecialization))
        self.check_specialization(first_doc, gen_doc, specializations)

        # check relations between first doc and token
        bundle_token, trusted_party = self.check_meta_concrete_component_and_token(first_doc, meta_bundle_returned)

        # check concrete doc token
        self.assertEqual("Trusted_Party", trusted_party.identifier._localpart)
        tp_attributes = trusted_party.attributes
        token_attributes = bundle_token.attributes

        # check type of agent - trusted party
        self.check_prov_type_trusted_party(tp_attributes, meta_bundle_returned)
        self.check_prov_type_token(token_attributes, meta_bundle_returned)

        # test token of the meta provenance
        token = self.check_meta_component_token(prov_doc_returned_deserialized)

        # assert that the signature and hash is right for the concrete document from this meta provenance - retrieve this document...
        self.check_signature_and_hash_of_doc(token)

    def test_10retrieve_meta_basic_rdf(self):
        # this test is not passing because the identifiers used in the meta provenance for
        # token, trusted party and token creating activity etc. are missing namespaces
        # rdf serialization defines new namespaces and ads them to identifiers for now
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/documents/meta/test_{self.prov_data.timestamp}_bundle_meta",
            {"format": "rdf"})
        self.assertEqual(self.prov_data.remote_meta_bundle_namespace[
                             self.prov_data.first_meta_bundle_info[1]
                         ].localpart, f"test_{self.prov_data.timestamp}_bundle_meta")

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["graph"].encode("utf-8")), format="rdf"
            # json assigns default namespace to entity byt what about rdf?
        )

        # test token of response
        self.check_meta_response_token(result)

        # test general and concrete entity - create prov and check against it
        document = ProvDocument()
        bundle_namespace = Namespace(
            "meta",
            f"prov-storage-{provenance_storage_hospital_name}:8000/api/v1/documents/meta/",
        )

        bundle_identifier = f"{self.prov_data.org_id}_test_{self.prov_data.timestamp}_bundle"
        test_bundle = document.bundle(bundle_namespace[bundle_identifier])
        test_bundle.add_namespace(PAV)
        test_bundle.add_namespace("example", "http://example.com")
        meta_org_namespace = Namespace(
            self.prov_data.org_id,
            fqdn_hospital + f"/api/v1/organizations/{self.prov_data.org_id}/graphs/",
        )
        test_bundle.add_namespace(meta_org_namespace)
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 1})
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_bundle_gen"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE)})

        # retrieve bundle from returned doc
        meta_bundle_returned = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            meta_bundle_returned = b
        meta_records = meta_bundle_returned.records

        first_doc = None
        gen_doc = None
        for record in test_bundle.records:
            self.assertTrue(record in meta_records)
            if record._identifier == QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"):
                first_doc = record
            if record._identifier.localpart.split("_")[-1] == "gen":
                gen_doc = record

        specializations = list(meta_bundle_returned.get_records(ProvSpecialization))
        self.check_specialization(first_doc, gen_doc, specializations)

        # check relations between first doc and token
        bundle_token, trusted_party = self.check_meta_concrete_component_and_token(first_doc, meta_bundle_returned)

        # check doc token
        self.assertEqual("Trusted_Party", trusted_party.identifier._localpart)
        tp_attributes = trusted_party.attributes
        token_attributes = bundle_token.attributes

        # check type of agent - trusted party
        self.check_prov_type_trusted_party(tp_attributes, meta_bundle_returned)
        self.check_prov_type_token(token_attributes, meta_bundle_returned)

        # test token of the meta provenance
        token = self.check_meta_component_token(prov_doc_returned_deserialized)

        # assert that the signature and hash is right for the concrete document from this meta provenance - retrieve this document...
        self.check_signature_and_hash_of_doc(token)

    def check_prov_type_trusted_party(self, tp_attributes, meta_bundle_returned):
        self.assertEqual(str(CPM_TRUSTED_PARTY), list(filter(lambda x: x[0] ==
                                                                       meta_bundle_returned.valid_qualified_name(
                                                                           PROV_TYPE), tp_attributes))[0][1])

    def check_signature_and_hash_of_doc(self, token, id_suffix=""):
        res2 = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}")
        result2 = json.loads(res2.content)
        token2 = result2["token"]
        public_key = load_pem_x509_certificate(
            token['cpm:trustedPartyCertificate'].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token['cpm:signature']), jcs.canonicalize(token2["data"]), ec.ECDSA(hashes.SHA256())
        )
        self.assertEqual(token2["data"]["documentDigest"], token['cpm:documentDigest'])

    def test_110_edit_graph_with_basic_backbone_ok(
            self):
        json_data = create_json_for_doc_storing(self.prov_data.doc_updated)

        res = requests.put(
            # put - meaning this doc is update of previous, different bundle id in bundle but not in request
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))
        self.assertEqual(201, res.status_code)
        result = json.loads(res.content)
        token = result["token"]
        self.check_token(json_data, token, self.prov_data.org_id, provenance_storage_hospital_name,
                         bundle_suffix="_updated")
        self.assertEqual(json_data["createdOn"], token["data"]["documentCreationTimestamp"])

        public_key = load_pem_x509_certificate(
            token["data"]["additionalData"]["trustedPartyCertificate"].encode("utf-8"),
            backend=default_backend()).public_key()
        public_key.verify(
            base64.b64decode(token["signature"]), jcs.canonicalize(token["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_112_retrieve_meta_with_2_versions(self):
        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/documents/meta/test_{self.prov_data.timestamp}_bundle_meta",
            {"format": "json"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["graph"].encode("utf-8")), format="json"
        )

        # test token of response
        self.check_meta_response_token(result)

        # test general and concrete entity - create prov and check against it
        document = ProvDocument()
        bundle_namespace = Namespace(
            "meta",
            f"prov-storage-{provenance_storage_hospital_name}:8000/api/v1/documents/meta/",
        )

        bundle_identifier = f"{self.prov_data.org_id}_test_{self.prov_data.timestamp}_bundle"
        test_bundle = document.bundle(bundle_namespace[bundle_identifier])
        test_bundle.add_namespace(PAV)
        test_bundle.set_default_namespace("http://example.com")
        meta_org_namespace = Namespace(
            self.prov_data.org_id,
            fqdn_hospital + f"/api/v1/organizations/{self.prov_data.org_id}/graphs/",
        )
        test_bundle.add_namespace(meta_org_namespace)
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 1})
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle_updated"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 2})
        gen_doc = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_bundle_gen"),
                                     other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE)})

        # retrieve bundle from returned doc
        meta_bundle_returned = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            meta_bundle_returned = b
        meta_records = meta_bundle_returned.records

        # retrieve specific entites
        first_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"))))
        second_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle_updated"))))

        for record in test_bundle.records:
            self.assertTrue(record in meta_records)

        # checkspecialization relations
        specializations = list(meta_bundle_returned.get_records(ProvSpecialization))

        self.assertEqual(2, len(specializations))
        self.check_specialization(first_doc, gen_doc, specializations)
        self.check_specialization(second_doc, gen_doc, specializations)

        # check derivation relations
        derivation_gen_concrete_bundle = list(meta_bundle_returned.get_records(ProvDerivation))[0]
        self.assertEqual(second_doc._identifier,
                         next(iter(derivation_gen_concrete_bundle.get_attribute(PROV_ATTR_GENERATED_ENTITY))))
        self.assertEqual(first_doc._identifier,
                         next(iter(derivation_gen_concrete_bundle.get_attribute(PROV_ATTR_USED_ENTITY))))
        self.assertEqual({"prov:revisionOf"},
                         derivation_gen_concrete_bundle.get_attribute(PROV_TYPE))

        # check relations between first doc and token
        bundle_token, trusted_party = self.check_meta_concrete_component_and_token(first_doc, meta_bundle_returned)
        bundle_token2, trusted_party2 = self.check_meta_concrete_component_and_token(second_doc, meta_bundle_returned,
                                                                                     id_suffix="_updated")

        self.assertEqual(trusted_party2, trusted_party)

        # check documents token
        self.assertEqual("Trusted_Party", trusted_party.identifier._localpart)
        tp_attributes = trusted_party.attributes
        token_attributes = bundle_token.attributes
        token2_attributes = bundle_token2.attributes

        # check type of agent - trusted party
        self.check_prov_type_trusted_party(tp_attributes, meta_bundle_returned)
        self.check_prov_type_token(token_attributes, meta_bundle_returned)
        self.check_prov_type_token(token2_attributes, meta_bundle_returned)

        # test token of the meta provenance
        token_doc1 = self.check_meta_component_token(prov_doc_returned_deserialized)
        token_doc2 = self.check_meta_component_token(prov_doc_returned_deserialized, id_suffix="_updated")

        # assert that the signature and hash is right for the concrete document from this meta provenance - retrieve this document...
        self.check_signature_and_hash_of_doc(token_doc1)
        self.check_signature_and_hash_of_doc(token_doc2, id_suffix="_updated")

    def check_prov_type_token(self, token_attributes, meta_bundle_returned):
        self.assertEqual(str(CPM_TOKEN), list(
            filter(lambda x: x[0] == meta_bundle_returned.valid_qualified_name(PROV_TYPE), token_attributes))[0][1])

    def test_1130_retrieve_meta_bundle_multiple_general_docs_in_one_meta_bundle(self):
        # need to create document and store it first
        # document needs to have same meta bundle id to work, and different document id
        doc, bundle, meta_bundle_id, backbone_basic, prev_cpm_bundle_info, _ = create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            # same main activity attributes - same meta bundle id
            timestamp=self.prov_data.timestamp, bundle_suffix="newdoc", smaller_prov=False)
        json_data = create_json_for_doc_storing(doc)

        # different suffix
        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundlenewdoc",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)

        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/documents/meta/test_{self.prov_data.timestamp}_bundle_meta",
            {"format": "json"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["graph"].encode("utf-8")), format="json"
        )

        self.check_meta_response_token(result)

        # test general and concrete entity - create prov and check against it
        document = ProvDocument()
        bundle_namespace = Namespace(
            "meta",
            f"prov-storage-{provenance_storage_hospital_name}:8000/api/v1/documents/meta/",
        )

        bundle_identifier = f"{self.prov_data.org_id}_test_{self.prov_data.timestamp}_bundle"
        test_bundle = document.bundle(bundle_namespace[bundle_identifier])
        test_bundle.add_namespace(PAV)
        test_bundle.set_default_namespace("http://example.com")
        meta_org_namespace = Namespace(
            self.prov_data.org_id,
            fqdn_hospital + f"/api/v1/organizations/{self.prov_data.org_id}/graphs/",
        )
        test_bundle.add_namespace(meta_org_namespace)
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 1})
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle_updated"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 2})
        gen_doc = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_bundlenewdoc_gen"),
                                     other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE)})
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 1})
        gen_doc_2 = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_bundle_gen"),
                                       other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE)})

        # retrieve bundle from returned doc
        meta_bundle_returned = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            meta_bundle_returned = b
        meta_records = meta_bundle_returned.records

        first_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"))))
        second_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle_updated"))))
        new_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc"))))

        # check that all of the docs docs are in one bundle
        for record in test_bundle.records:
            self.assertTrue(record in meta_records, msg=record)

        # check specializations - concrete entities are specialized from the right general ones
        specializations = list(meta_bundle_returned.get_records(ProvSpecialization))

        self.assertEqual(3, len(specializations))
        self.check_specialization(new_doc, gen_doc, specializations)
        self.check_specialization(first_doc, gen_doc_2, specializations)
        self.check_specialization(second_doc, gen_doc_2, specializations)

        self.assertEqual(1, len(list(meta_bundle_returned.get_records(ProvDerivation))))

        # check relations between new doc and token
        bundle_token2, trusted_party2 = self.check_meta_concrete_component_and_token(new_doc, meta_bundle_returned,
                                                                                     id_suffix="newdoc")
        # test token of the meta provenance
        token_doc = self.check_meta_component_token(prov_doc_returned_deserialized, id_suffix="newdoc")

        # assert that the signature and hash is right for the concrete document from this meta provenance - retrieve this document...
        self.check_signature_and_hash_of_doc(token_doc, id_suffix="newdoc")

    def test_1131_retrieve_meta_bundle_multiple_general_docs_with_multiple_versions_in_one_meta_bundle(self):
        # need to create document and store it first
        # document needs to have same meta bundle id to work, and different document id
        doc, bundle, meta_bundle_id, backbone_basic, prev_cpm_bundle_info, _ = create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            # same main activity attributes - same meta bundle id
            timestamp=self.prov_data.timestamp, bundle_suffix="newdoc2", smaller_prov=False)
        json_data = create_json_for_doc_storing(doc)

        # different suffix
        res = requests.put(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundlenewdoc",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)

        doc2, bundle, meta_bundle_id, backbone_basic, prev_cpm_bundle_info, _ = create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            # same main activity attributes - same meta bundle id
            timestamp=self.prov_data.timestamp, bundle_suffix="edit2", smaller_prov=False)
        json_data = create_json_for_doc_storing(doc2)

        # different suffix, update first bundle second time - according to request url
        res = requests.put(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_updated",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)

        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/documents/meta/test_{self.prov_data.timestamp}_bundle_meta",
            {"format": "json"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["graph"].encode("utf-8")), format="json"
        )

        self.check_meta_response_token(result)

        # test general and concrete entity - create prov and check against it
        document = ProvDocument()
        bundle_namespace = Namespace(
            "meta",
            f"prov-storage-{provenance_storage_hospital_name}:8000/api/v1/documents/meta/",
        )

        bundle_identifier = f"{self.prov_data.org_id}_test_{self.prov_data.timestamp}_bundle"
        test_bundle = document.bundle(bundle_namespace[bundle_identifier])
        test_bundle.add_namespace(PAV)
        test_bundle.set_default_namespace("http://example.com")
        meta_org_namespace = Namespace(
            self.prov_data.org_id,
            fqdn_hospital + f"/api/v1/organizations/{self.prov_data.org_id}/graphs/",
        )
        test_bundle.add_namespace(meta_org_namespace)
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 1})
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle_updated"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 2})
        gen_doc = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_bundlenewdoc_gen"),
                                     other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE)})
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 1})
        gen_doc_2 = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_bundle_gen"),
                                       other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE)})
        doc_3 = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc2"),
                                   other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                                     str(PAV_VERSION): 2})
        doc_4 = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundleedit2"),
                                   other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                                     str(PAV_VERSION): 3})

        # retrieve bundle from returned doc
        meta_bundle_returned = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            meta_bundle_returned = b
        meta_records = meta_bundle_returned.records

        first_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"))))
        second_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle_updated"))))
        new_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc"))))
        new_doc_2 = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc2"))))
        third_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundleedit2"))))

        # check that all of the docs docs are in one bundle
        for record in test_bundle.records:
            self.assertTrue(record in meta_records, msg=record)

        # check specialization relations  - every concrete entity is at right general entity
        self.assertEqual(5, len(list(meta_bundle_returned.get_records(ProvSpecialization))))
        specializations = list(meta_bundle_returned.get_records(ProvSpecialization))
        self.check_specialization(new_doc, gen_doc, specializations)
        self.check_specialization(first_doc, gen_doc_2, specializations)
        self.check_specialization(second_doc, gen_doc_2, specializations)
        self.check_specialization(new_doc_2, gen_doc, specializations)
        self.check_specialization(third_doc, gen_doc_2, specializations)

        # check derivation relations - entities in meta provenance are derived from last version
        self.assertEqual(3, len(list(meta_bundle_returned.get_records(ProvDerivation))))

        derivations = list(meta_bundle_returned.get_records(ProvDerivation))
        # check that the derivation has type revision
        for derivation in derivations:
            self.assertTrue("prov:revisionOf" in derivation.get_asserted_types())

        # check derivations
        self.check_derivation(derivations, first_doc, second_doc)
        self.check_derivation(derivations, new_doc, new_doc_2)
        self.check_derivation(derivations, second_doc, third_doc)

        # check relations between new doc and token
        self.check_meta_concrete_component_and_token(new_doc_2, meta_bundle_returned,
                                                     id_suffix="newdoc2")
        # test token of the meta provenance
        token_doc = self.check_meta_component_token(prov_doc_returned_deserialized, id_suffix="newdoc2")

        # assert that the signature and hash is right for the concrete document from this meta provenance - retrieve this document...
        self.check_signature_and_hash_of_doc(token_doc, id_suffix="newdoc2")

    def test_1132_retrieve_meta_bundle_multiple_instances_derived_from_concrete_component(self):
        # need to create document and store it first
        # document needs to have same meta bundle id to work, and different document id
        doc, bundle, meta_bundle_id, backbone_basic, prev_cpm_bundle_info, _ = create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            # same main activity attributes - same meta bundle id
            timestamp=self.prov_data.timestamp, bundle_suffix="newdoc4", smaller_prov=False)
        json_data = create_json_for_doc_storing(doc)

        # update bundlenewdoc again - should pass
        res = requests.put(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundlenewdoc",
            json.dumps(json_data))

        self.assertEqual(201, res.status_code)

        res = requests.get(
            f"http://{provenance_storage_hospital_url}/api/v1/documents/meta/test_{self.prov_data.timestamp}_bundle_meta",
            {"format": "json"})

        self.assertEqual(200, res.status_code)
        result = json.loads(res.content)

        prov_doc_returned_deserialized = ProvDocument.deserialize(
            content=b64decode(result["graph"].encode("utf-8")), format="json"
        )

        self.check_meta_response_token(result)

        # test general and concrete entity - create prov and check against it
        document = ProvDocument()
        bundle_namespace = Namespace(
            "meta",
            f"prov-storage-{provenance_storage_hospital_name}:8000/api/v1/documents/meta/",
        )

        bundle_identifier = f"{self.prov_data.org_id}_test_{self.prov_data.timestamp}_bundle"
        test_bundle = document.bundle(bundle_namespace[bundle_identifier])
        test_bundle.add_namespace(PAV)
        test_bundle.set_default_namespace("http://example.com")
        meta_org_namespace = Namespace(
            self.prov_data.org_id,
            fqdn_hospital + f"/api/v1/organizations/{self.prov_data.org_id}/graphs/",
        )
        test_bundle.add_namespace(meta_org_namespace)
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 1})
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle_updated"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 2})
        gen_doc = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_bundlenewdoc_gen"),
                                     other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE)})
        test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc"),
                           other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                             str(PAV_VERSION): 1})
        gen_doc_2 = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_bundle_gen"),
                                       other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE)})
        doc_3 = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc2"),
                                   other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                                     str(PAV_VERSION): 2})
        doc_4 = test_bundle.entity(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundleedit2"),
                                   other_attributes={str(PROV_TYPE): str(PROV_ATTR_BUNDLE),
                                                     str(PAV_VERSION): 3})

        # retrieve bundle from returned doc
        meta_bundle_returned = ProvBundle()
        for b in prov_doc_returned_deserialized.bundles:
            meta_bundle_returned = b
        meta_records = meta_bundle_returned.records

        first_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle"))))
        second_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundle_updated"))))
        new_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc"))))
        new_doc_2 = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc2"))))
        new_doc_2_new = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundlenewdoc4"))))
        third_doc = first(meta_bundle_returned.get_record(
            str(QualifiedName(meta_org_namespace, f"test_{self.prov_data.timestamp}_bundleedit2"))))

        # check that all of the docs docs are in one bundle
        for record in test_bundle.records:
            self.assertTrue(record in meta_records, msg=record)

        # check specialization relations  - every concrete entity is at right general entity
        self.assertEqual(6, len(list(meta_bundle_returned.get_records(ProvSpecialization))))
        specializations = list(meta_bundle_returned.get_records(ProvSpecialization))
        self.check_specialization(new_doc, gen_doc, specializations)
        self.check_specialization(first_doc, gen_doc_2, specializations)
        self.check_specialization(second_doc, gen_doc_2, specializations)
        self.check_specialization(new_doc_2, gen_doc, specializations)
        self.check_specialization(new_doc_2_new, gen_doc, specializations)
        self.check_specialization(third_doc, gen_doc_2, specializations)

        # check derivation relations - entities in meta provenance are derived from last version
        self.assertEqual(4, len(list(meta_bundle_returned.get_records(ProvDerivation))))
        derivations = list(meta_bundle_returned.get_records(ProvDerivation))
        self.check_derivation(derivations, first_doc, second_doc)

        # check that both components are derived from new_doc
        self.check_derivation(derivations, new_doc, new_doc_2)
        self.check_derivation(derivations, new_doc, new_doc_2_new)

        self.check_derivation(derivations, second_doc, third_doc)

        # check relations between new doc and token
        self.check_meta_concrete_component_and_token(new_doc, meta_bundle_returned,
                                                     id_suffix="newdoc")
        self.check_meta_concrete_component_and_token(new_doc_2_new, meta_bundle_returned,
                                                     id_suffix="newdoc4")
        # test token of the meta provenance
        token_doc = self.check_meta_component_token(prov_doc_returned_deserialized, id_suffix="newdoc4")

        # assert that the signature and hash is right for the concrete document from this meta provenance - retrieve this document...
        self.check_signature_and_hash_of_doc(token_doc, id_suffix="newdoc4")

    def check_meta_response_token(self, result):
        """Checks the tokens fields provided with meta component.

        :param result: dictionary of responses content
        :returns: None
        """
        token = result["token"]
        self.assertEqual("Hospital_Provenance_Storage", token["data"]["originatorId"])
        self.assertEqual("Trusted_Party", token["data"]["authorityId"])
        digest = hashes.Hash(hashes.SHA256())
        digest.update(base64.b64decode(result["graph"]))
        hash1 = digest.finalize().hex()
        self.assertEqual(hash1, token["data"]["documentDigest"])
        self.assertIsNotNone(token["data"]["tokenTimestamp"])
        self.assertTrue(datetime.timestamp(datetime.now()) > token["data"]["tokenTimestamp"])

    def check_meta_component_token(self, prov_doc_returned_deserialized, id_suffix=""):
        """Checks the tokens fields of concrete CPM component from meta provenance

        :param prov_doc_returned_deserialized: returned meta component deserialized
        :param id_suffix: suffix of cpm component identifier
        :returns: token of the concrete CPM component
        """
        doc = prov_doc_returned_deserialized.serialize()
        # get token of component
        token = json.loads(doc)["bundle"][f"meta:test_{self.prov_data.timestamp}_bundle_meta"]["entity"][
            f"{self.prov_data.org_id}_test_{self.prov_data.timestamp}_bundle{id_suffix}_token"]
        self.assertEqual(self.prov_data.org_id, token["cpm:originatorId"])
        self.assertEqual("Trusted_Party", token['cpm:authorityId'])
        self.assertIsNotNone(token['cpm:tokenTimestamp'])
        self.assertIsNotNone(token['cpm:documentCreationTimestamp'])
        self.assertEqual(
            f"http://prov-storage-{provenance_storage_hospital_name}:8000/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            token['cpm:bundle'])
        self.assertEqual("SHA256", token['cpm:hashFunction'])
        self.assertEqual("trusted-party:8020", token['cpm:trustedPartyUri'])
        self.assertIsNotNone(token['cpm:trustedPartyCertificate'])
        self.assertIsNotNone(token['cpm:signature'])
        return token

    def check_meta_concrete_component_and_token(self, first_doc, meta_bundle_returned, id_suffix=""):
        """Retrieves the token of concrete CPM component from meta provenances bundle
        and checks its relations with generation activity and trusted party

        :param first_doc: returned meta component deserialized
        :param meta_bundle_returned: bundle from meta component deserialized - contains records...
        :param id_suffix: suffix of cpm component identifier
        :returns: bundle_token, trusted_party: retrieved token and trusted party
        """
        token_generation = \
            list(meta_bundle_returned.get_record(f"test_{self.prov_data.timestamp}_bundle{id_suffix}_tokenGeneration"))[
                0]

        # in case of redefinition of identifiers in created meta components change this code
        self.assertEqual(CPM_TOKEN_GENERATION._str,
                         first(token_generation.get_attribute(PROV_TYPE)))
        usage_records = list(meta_bundle_returned.get_records(ProvUsage))
        usage = [x for x in usage_records if token_generation.identifier == first(x.get_attribute(PROV_ATTR_ACTIVITY))][
            0]
        self.assertEqual(first_doc.identifier,
                         first(usage.get_attribute(PROV_ATTR_ENTITY)))

        trusted_party = list(meta_bundle_returned.get_records(ProvAgent))[0]
        association_records = list(meta_bundle_returned.get_records(ProvAssociation))
        association = \
            [x for x in association_records if
             token_generation.identifier == first(x.get_attribute(PROV_ATTR_ACTIVITY))][0]
        self.assertEqual(trusted_party.identifier,
                         first(association.get_attribute(PROV_ATTR_AGENT)))

        bundle_token = meta_bundle_returned.get_record(
            f"{self.prov_data.org_id}_test_{self.prov_data.timestamp}_bundle{id_suffix}_token")[
            0]
        generation_records = list(meta_bundle_returned.get_records(ProvGeneration))
        generation = \
            [x for x in generation_records if
             token_generation.identifier == first(x.get_attribute(PROV_ATTR_ACTIVITY))][0]
        self.assertEqual(bundle_token.identifier,
                         first(generation.get_attribute(PROV_ATTR_ENTITY)))

        attribution_tokens = list(meta_bundle_returned.get_records(ProvAttribution))
        attribution = \
            [x for x in attribution_tokens if
             bundle_token.identifier == first(x.get_attribute(PROV_ATTR_ENTITY))][0]
        self.assertEqual(trusted_party.identifier,
                         first(attribution.get_attribute(PROV_ATTR_AGENT)))
        return bundle_token, trusted_party

    def check_token(self, json_data, token, org_id, storage_name, bundle_suffix=""):
        """Checks the tokens fields provided with CPM component in request.

        :param json_data: content of request sent to storage service
        :param token: token from response
        :param org_id: identifier of organization storing the component
        :param storage_name: name of provenance storage
        :param bundle_suffix: suffix of identifier of component stored
        :returns: None
        """
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

    def check_specialization(self, first_doc, gen_doc, specializations):
        """Checks the specialization relation between entities. Used for checking relations in meta provenance.

        :param first_doc: specific entity of specialization
        :param gen_doc: general entity of specialization
        :param specializations: all specialization relations from the bundle checked
        :returns: None
        """
        specialization_gen_concrete_bundle = [x for x in specializations if
                                              first(x.get_attribute(PROV_ATTR_SPECIFIC_ENTITY)) ==
                                              first_doc._identifier]
        self.assertEqual(1, len(specialization_gen_concrete_bundle))
        self.assertEqual(gen_doc._identifier,
                         next(iter(specialization_gen_concrete_bundle[0].get_attribute(PROV_ATTR_GENERAL_ENTITY))))

    def check_derivation(self, derivations, first_doc, second_doc):
        """Checks the derivation relation between entities. Used for checking relations in meta provenance.

        :param derivations: all derivation relations from the bundle checked
        :param first_doc: used entity of derivation
        :param second_doc: generated entity of derivation
        :returns: None
        """
        derivation = [x for x in derivations if
                      first(x.get_attribute(PROV_ATTR_GENERATED_ENTITY)) ==
                      second_doc._identifier]
        self.assertEqual(1, len(derivation))
        self.assertEqual(first_doc._identifier,
                         next(iter(derivation[0].get_attribute(PROV_ATTR_USED_ENTITY))))
