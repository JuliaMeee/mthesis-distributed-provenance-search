import base64
import random
import string
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

    @classmethod
    def setUpClass(cls):
        # register org, upload first document... - as in basic tests - some tests depend on it
        register_org1_to_hospital_storage(cls.prov_data)
        json_data = create_json_for_doc_storing(cls.prov_data.doc)

        requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{cls.prov_data.org_id}/documents/test_{cls.prov_data.timestamp}_bundle",
            json.dumps(json_data))

    def test_02_register_org_nok_already_registered(self):
        res = requests.post(
            "http://" + provenance_storage_hospital_url + "/api/v1/organizations/" + self.prov_data.org_id,
            json.dumps({
                "clientCertificate": "wrong_cert", "intermediateCertificates": ["cert..."]
            }))
        self.assertEqual(409, res.status_code)
        self.assertEqual({
            "error": f"Organization with id [{self.prov_data.org_id}] is already registered. If you want to modify it, send PUT request!"},
            json.loads(res.content))

    def test_030_register_org_nok_missing_field_intermediateCertificates(self):
        res = requests.post("http://" + provenance_storage_hospital_url + "/api/v1/organizations/" + ''.join(
            random.choices(string.ascii_uppercase + string.digits, k=8)),
                            json.dumps({
                                "clientCertificate": "wrong_cert"
                            }))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": f"Mandatory field [intermediateCertificates] not present in request!"},
                         json.loads(res.content))

    def test_031_register_org_nok_missing_field_clientCertificate(self):
        res = requests.post("http://" + provenance_storage_hospital_url + "/api/v1/organizations/" + ''.join(
            random.choices(string.ascii_uppercase + string.digits, k=8)),
                            json.dumps({
                                "intermediateCertificates": ["cert..."]
                            }))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": f"Mandatory field [clientCertificate] not present in request!"},
                         json.loads(res.content))

    def test_04_register_org_nok_wrong_certificates(self):
        with open('int1.pem', 'r') as file:
            intermediate_cert_1 = file.read()

        with open("./test_cert.pem", "r") as file:
            client_cert = file.read()

        json_data = {}
        json_data["clientCertificate"] = client_cert
        # missing intermediate certificate - certificate chain unverifiable
        json_data["intermediateCertificates"] = [intermediate_cert_1]
        res = requests.post("http://" + provenance_storage_hospital_url + "/api/v1/organizations/" + ''.join(
            random.choices(string.ascii_uppercase + string.digits, k=8)),
                            json.dumps(json_data))
        self.assertEqual(401, res.status_code)
        self.assertEqual({"error": "Trusted party was unable to verify certificate chain!"},
                         json.loads(res.content))

    def test_051store_graph_with_basic_backbone_nok_encoded_json(
            self):
        document_json = provjson.encode_json_document(self.prov_data.doc)
        # added string to json - wrong json created
        document_json = b"mistak" + jcs.canonicalize(document_json)
        document_b64 = base64.b64encode(document_json)

        with open('test_sign.pem', 'rb') as file:
            private_ke = file.read()
        private_key = serialization.load_pem_private_key(private_ke, password=None)
        signature = private_key.sign(data=document_json, signature_algorithm=ec.ECDSA(hashes.SHA256()))

        json_data = {}
        json_data["document"] = document_b64.decode("utf-8")
        json_data["documentFormat"] = "json"
        json_data["signature"] = base64.b64encode(signature).decode("utf-8")
        json_data["createdOn"] = datetime.timestamp(datetime(2025, 1, 21, 2, 2, 4))

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_test_nok_bundle",
            json.dumps(json_data))
        self.assertEqual(500,
                         res.status_code)  # right? self._prov_document = provm.ProvDocument.deserialize ... in prov_validators throws json error

    def test_052store_graph_with_basic_backbone_nok_encoded_b64(
            self):
        document_json = provjson.encode_json_document(self.prov_data.doc)
        document_json = jcs.canonicalize(document_json)
        document_b64 = base64.b64encode(document_json)

        with open('test_sign.pem', 'rb') as file:
            private_ke = file.read()
        private_key = serialization.load_pem_private_key(private_ke, password=None)
        signature = private_key.sign(data=document_json, signature_algorithm=ec.ECDSA(hashes.SHA256()))

        json_data = {}
        json_data["document"] = document_json.decode("utf-8")
        json_data["documentFormat"] = "json"
        json_data["signature"] = base64.b64encode(signature).decode("utf-8")
        json_data["createdOn"] = datetime.timestamp(datetime(2025, 1, 21, 2, 2, 4))

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_test_nok2_bundle",
            json.dumps(json_data))
        self.assertTrue(res.status_code in (
            500,
            401))  # right? elf._graph = base64.b64decode(graph) ... in prov_validators throws incorrect padding error

    def test_053store_graph_with_basic_backbone_nok_signature(
            self):
        document_json = provjson.encode_json_document(self.prov_data.doc)
        document_json = jcs.canonicalize(document_json)
        document_b64 = base64.b64encode(document_json)

        with open('test_sign.pem', 'rb') as file:
            private_ke = file.read()
        private_key = serialization.load_pem_private_key(private_ke, password=None)
        signature = private_key.sign(data=document_json + b"wrong data", signature_algorithm=ec.ECDSA(hashes.SHA256()))

        json_data = {}
        json_data["document"] = document_b64.decode("utf-8")
        json_data["documentFormat"] = "json"
        json_data["signature"] = base64.b64encode(signature).decode("utf-8")
        json_data["createdOn"] = datetime.timestamp(datetime(2025, 1, 21, 2, 2, 4))

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_test_nok3_bundle",
            json.dumps(json_data))
        self.assertEqual(401,
                         res.status_code)
        self.assertEqual(
            {"error": "Unverifiable signature. Make sure to register your certificate with trusted party first."},
            json.loads(res.content))

    def test_054store_graph_with_basic_backbone_nok_format(
            self):
        #the sub-function serializes document to json, but sets rfd as a format in request
        json_data = create_json_for_doc_storing(self.prov_data.doc, doc_format="rfd")

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_test_nok3_bundle",
            json.dumps(json_data))
        print(res.content)
        self.assertEqual(500,
                         res.status_code)  # right? - error thrown at self._prov_document = provm.ProvDocument.deserialize   in  prov_validators.py

    def test_055store_graph_with_basic_backbone_nok_doc_already_stored(
            self):
        json_data = create_json_for_doc_storing(self.prov_data.doc)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))
        self.assertEqual(409,
                         res.status_code)
        self.assertEqual(
            {
                "error": f"Document with id [test_{self.prov_data.timestamp}_bundle] already exists under organization [{self.prov_data.org_id}]."},
            json.loads(res.content))

    def test_056store_graph_with_basic_backbone_nok_missing_signature(
            self):
        json_data = create_json_for_doc_storing(self.prov_data.doc)
        json_data.pop("signature")

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))
        self.assertEqual(401,
                         res.status_code)
        self.assertEqual(
            {"error": "Unverifiable signature. Make sure to register your certificate with trusted party first."},
            json.loads(res.content))

    def test_057store_graph_with_basic_backbone_nok_unregistered_org(
            self):
        doc, bundle, meta_bundle_id, backbone_basic, _, _ = helpers.create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id + "wrong",
            main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            timestamp=self.prov_data.timestamp)

        json_data = create_json_for_doc_storing(doc)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}wrong/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))
        self.assertEqual(404,
                         res.status_code)
        self.assertEqual(
            {
                "error": f"Organization with id [{self.prov_data.org_id}wrong] is not registered! Please register your organization first."},
            json.loads(res.content))

    def test_058store_graph_with_basic_backbone_nok_no_cpm(
            self):
        document = ProvDocument()
        bundle_namespace = Namespace(
            "org",
            f"http://prov-storage-{provenance_storage_hospital_name}:8000/api/v1/organizations/{self.prov_data.org_id}/documents/",
        )

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle"
        bundle = document.bundle(bundle_namespace[bundle_identifier])
        example_namespace = bundle.add_namespace("example", "http://example.com")

        existential_variable_generator = helpers.ExistentialVariablesGenerator(
            self.prov_data.existential_variable_prefix)

        helpers.create_domain_prov(bundle,
                                   existential_variable_generator, "org")

        json_data = create_json_for_doc_storing(document)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))
        self.assertEqual(400,
                         res.status_code)
        self.assertEqual(
            {
                "error": f"No 'mainActivity' activity specified inside of bundle [test_{self.prov_data.timestamp}_bundle]"},
            json.loads(res.content))

    def test_0590store_graph_with_basic_backbone_nok_different_id_in_query(
            self):
        doc, bundle, meta_bundle_id, backbone_basic, _, _ = helpers.create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            timestamp=self.prov_data.timestamp)

        json_data = create_json_for_doc_storing(doc)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle2",
            json.dumps(json_data))
        self.assertEqual(400,
                         res.status_code)
        self.assertEqual(
            {'error': f'The bundle id [test_{self.prov_data.timestamp}_bundle] does not match the '
                      f'specified id [test_{self.prov_data.timestamp}_bundle2] from query.'},
            json.loads(res.content))

    def test_0591store_graph_with_basic_backbone_nok_different_org_in_doc(
            self):
        doc, bundle, meta_bundle_id, backbone_basic, _, _ = helpers.create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id + "wrong",
            main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            timestamp=self.prov_data.timestamp, bundle_suffix="3")

        json_data = create_json_for_doc_storing(doc)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle3",
            json.dumps(json_data))
        self.assertEqual(400,
                         res.status_code)
        self.assertEqual(
            {'error': 'The bundle identifier '
                      f'[http://prov-storage-hospital:8000/api/v1/organizations/{self.prov_data.org_id}wrong/documents/test_{self.prov_data.timestamp}_bundle3] '
                      f'has not the same query parameters called to save it. [/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle3]'},
            json.loads(res.content))

    def test_111_edit_graph_with_basic_backbone_nok_same_bundle_id(
            self):
        json_data = create_json_for_doc_storing(self.prov_data.doc)

        res = requests.put(
            # put - meaning this doc is update of previous, different bundle id in bundle but not in request
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))

        self.assertEqual(409, res.status_code)
        self.assertEqual({
            "error": f"Document with id [test_{self.prov_data.timestamp}_bundle] already exists under organization [{self.prov_data.org_id}]."},
            json.loads(res.content))
