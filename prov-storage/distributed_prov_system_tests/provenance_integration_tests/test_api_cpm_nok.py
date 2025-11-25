import base64
import copy
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

import helpers
from provenance.constants import CPM_BACKWARD_CONNECTOR, CPM_FORWARD_CONNECTOR, CPM_MAIN_ACTIVITY, PAV_VERSION, PAV, \
    CPM_TRUSTED_PARTY, CPM_TOKEN, CPM_TOKEN_GENERATION, CPM, CPM_REFERENCED_META_BUNDLE_ID, CPM_REFERENCED_BUNDLE_ID, \
    CPM_REFERENCED_BUNDLE_HASH_VALUE, CPM_HASH_ALG, CPM_SENDER_AGENT, CPM_HAS_ID, CPM_ID, CPM_EXTERNAL_ID, \
    CPM_EXTERNAL_ID_TYPE
from certificate_helpers import generate_certificate, parse_certificate
from provenance_integration_tests.api_test_helpers import provenance_storage_hospital_name, fqdn_pathology, \
    provenance_storage_pathology_name, provenance_storage_hospital_name, fqdn_hospital, trusted_party_url, \
    provenance_storage_pathology_url, provenance_storage_hospital_url, create_json_for_doc_storing, TestDataCreator, \
    register_org_to_storage


class MyTestCase(TestCase):
    prov_data = TestDataCreator()

    @classmethod
    def setUpClass(cls):
        # register org, upload first documents... - as in basic tests - cpm depend on it  - connectors reference basic saved documents
        register_org_to_storage(cls.prov_data.org_id, provenance_storage_hospital_url)
        json_data = create_json_for_doc_storing(cls.prov_data.doc)

        requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{cls.prov_data.org_id}/documents/test_{cls.prov_data.timestamp}_bundle",
            json.dumps(json_data))
        register_org_to_storage(cls.prov_data.org_id2, provenance_storage_pathology_url,
                                cert_location="./test_cert2.pem", key_location="./test_sign2.pem")

        json_data = create_json_for_doc_storing(cls.prov_data.doc2, private_key_location='test_sign2.pem')

        requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{cls.prov_data.org_id2}/documents/test_{cls.prov_data.timestamp}_bundle_w_backward_connector",
            json.dumps(json_data))

        json_data = create_json_for_doc_storing(cls.prov_data.doc_end)

        requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{cls.prov_data.org_id}/documents/test_{cls.prov_data.timestamp}_bundle_end",
            json.dumps(json_data))

    # these tests should run after each other at once - they depend on data written to the running applications

    def test_1200_store_graph_with_basic_backbone_nok_missing_main_activity_type(self):
        meta_ns = Namespace(
            "meta", f"http://prov-storage-{provenance_storage_hospital_name}:8000/api/v1/documents/meta/"
        )
        main_activity_attributes_ok = {
            # PROV_TYPE: CPM_MAIN_ACTIVITY, missing prov type
            CPM_REFERENCED_META_BUNDLE_ID: meta_ns[
                # id of meta bundle - used when requesting meta provenance
                f"test_{self.prov_data.timestamp}_bundle_meta"
            ],
        }
        doc, bundle, meta_bundle_id, backbone_basic, prev_cpm_bundle_info, _ = helpers.create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=main_activity_attributes_ok,
            timestamp=self.prov_data.timestamp, bundle_suffix="_wrong")

        json_data = create_json_for_doc_storing(doc)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_wrong",
            json.dumps(json_data))

        self.assertEqual(400, res.status_code)
        self.assertEqual(
            {
                "error": f"No 'mainActivity' activity specified inside of bundle [test_{self.prov_data.timestamp}_bundle_wrong]"},
            json.loads(res.content))

    def test_1201_store_graph_with_basic_backbone_nok_missing_referenced_meta_bundle_id(self):
        main_activity_attributes_ok = {
            PROV_TYPE: CPM_MAIN_ACTIVITY,
            # CPM_REFERENCED_META_BUNDLE_ID: ...    - missing referenced meta bundle id
        }
        doc, bundle, meta_bundle_id, backbone_basic, prev_cpm_bundle_info, _ = helpers.create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=main_activity_attributes_ok,
            timestamp=self.prov_data.timestamp, bundle_suffix="_wrong")

        json_data = create_json_for_doc_storing(doc)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_wrong",
            json.dumps(json_data))

        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Main activity missing required attribute 'cpm:referencedMetaBundleId'."},
                         json.loads(res.content))

    def test_1202_store_graph_with_basic_backbone_nok_entity_which_is_not_connector_generated_by_main_activity(self):
        # without type, the connector is not connector and main activity cannot create non connectors
        meta_ns = Namespace(
            "meta", f"http://prov-storage-{provenance_storage_hospital_name}:8000/api/v1/documents/meta/"
        )
        main_activity_attributes_ok = {
            PROV_TYPE: CPM_MAIN_ACTIVITY,
            CPM_REFERENCED_META_BUNDLE_ID: meta_ns[
                # id of meta bundle - used when requesting meta provenance
                f"test_{self.prov_data.timestamp}_bundle_meta"
            ],
        }
        doc, bundle, meta_bundle_id, backbone_basic, prev_cpm_bundle_info, _ = helpers.create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=main_activity_attributes_ok,
            timestamp=self.prov_data.timestamp, bundle_suffix="_wrong", forward_connector_attributes={})

        json_data = create_json_for_doc_storing(doc)

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_wrong",
            json.dumps(json_data))

        self.assertEqual(400, res.status_code)
        self.assertEqual(
            {"error": 'CPM problem: Main activity generated entity that is not forward '
                      'connector'},
            json.loads(res.content))

    def test_1203store_graph_with_backbone_with_backwards_connector_nok_missing_referencedBundleId_in_backward_connector(
            self):
        # store document - create 2nd doc here - remove attribute from backward connector, store throws error
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix="_w_backward_connector_wrong")

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            # CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace[self.prev_cpm_bundle_info[1]],        # missing - should throw error
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Backward connector(s) is/are missing mandatory attributes."},
                         json.loads(res.content))

    def test_1204store_graph_with_backbone_with_backwards_connector_nok_missing_CPM_REFERENCED_META_BUNDLE_ID_in_backward_connector(
            self):
        # store document - create 2nd doc here - remove attribute from backward connector, store throws error
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix="_w_backward_connector_wrong")

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            # missing - should throw error
            # CPM_REFERENCED_META_BUNDLE_ID: self.remote_meta_bundle_namespace[
            #    self.prev_meta_bundle_info[1]
            # ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Backward connector(s) is/are missing mandatory attributes."},
                         json.loads(res.content))

    def test_1205store_graph_with_backbone_with_backwards_connector_nok_missing_CPM_REFERENCED_BUNDLE_HASH_VALUE_in_backward_connector(
            self):
        # store document - create 2nd doc here - remove attribute from backward connector, store throws error
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix="_w_backward_connector_wrong")

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            # missing - should throw error
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],

            # CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prev_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Backward connector(s) is/are missing mandatory attributes."},
                         json.loads(res.content))

    def test_1206store_graph_with_backbone_with_backwards_connector_nok_missing_CPM_HASH_ALG_in_backward_connector(
            self):
        # store document - create 2nd doc here - remove attribute from backward connector, store throws error
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix="_w_backward_connector_wrong")

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            # CPM_HASH_ALG: "SHA256" missing
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Backward connector(s) is/are missing mandatory attributes."},
                         json.loads(res.content))

    def test_1207store_graph_with_backbone_with_backwards_connector_nok_unresolvable_CPM_REFERENCED_BUNDLE_ID_in_bw_namespace(
            self):
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix="_w_backward_connector_wrong")

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: Namespace("prov", "http://www.wxsaxsa3.org/ns/prov#")[
                self.prov_data.first_cpm_bundle_info[1] + "wrongbundle"],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong",
            json.dumps(json_data))

        self.assertEqual(500, res.status_code)

    def test_1208store_graph_with_backbone_with_backwards_connector_nok_unresolvable_CPM_REFERENCED_BUNDLE_ID_in_bw_bundle(
            self):
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix="_w_backward_connector_wrong")

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[
                self.prov_data.first_cpm_bundle_info[1] + "wrongbundle"],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong",
            json.dumps(json_data))

        self.assertEqual(400, res.status_code)
        self.assertEqual(
            {"error": "Referenced bundle URI of connector [pathology:e001_sample_backwards_connector] not found."},
            json.loads(res.content))

    def test_1209_store_graph_with_backbone_with_backwards_connector_nok_unresolvable_CPM_REFERENCED_META_BUNDLE_ID_in_bw_bundle(
            self):
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix="_w_backward_connector_wrong")

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[  # wrong bundle
                self.prov_data.first_meta_bundle_info[1] + "wrong_meta_bundle"
                ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong",
            json.dumps(json_data))

        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'Referenced meta bundle URI of connector '
                                   '[pathology:e001_sample_backwards_connector] not found.'},
                         json.loads(res.content))

    def test_1210_store_graph_with_backbone_with_forward_connector_nok_unresolvable_CPM_REFERENCED_BUNDLE_ID_in_forward_connector_bundle(
            self):
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace_2[
                self.prov_data.next_cpm_bundle_info[1] + "wrong_uri"],
            # unresolvable
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _, _ = helpers.create_cpm_with_forward_connector(
            self.prov_data.existential_variable_prefix,
            provenance_storage_hospital_name,
            org_name=self.prov_data.org_id, main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            receiver_org_name=provenance_storage_pathology_name, timestamp=self.prov_data.timestamp,
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign.pem')

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'Referenced bundle URI of connector [hospital:e001_connector_s1] not found.'},
                         json.loads(res.content))

    def test_1211_store_graph_with_backbone_with_forward_connector_nok_unresolvable_CPM_REFERENCED_META_BUNDLE_ID_in_forward_connector_bundle(
            self):
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace_2[self.prov_data.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[  # unresolvable
                self.prov_data.first_meta_bundle_info[1] + "wrong_uri"
                ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _, _ = helpers.create_cpm_with_forward_connector(
            self.prov_data.existential_variable_prefix,
            provenance_storage_hospital_name,
            org_name=self.prov_data.org_id, main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            receiver_org_name=provenance_storage_pathology_name, timestamp=self.prov_data.timestamp,
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign.pem')

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'Referenced meta bundle URI of connector '
                                   '[hospital:e001_connector_s1] not found.'},
                         json.loads(res.content))

    def test_1212_store_graph_with_backbone_with_backwards_connector_nok_unresolvable_CPM_REFERENCED_META_BUNDLE_ID_in_bw_namespace(
            self):
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix="_w_backward_connector_wrong")

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: Namespace("prov", "http://www.wxsaxsa3.org/ns/prov#")[
                # wrong namespace- will not resolve
                self.prov_data.first_cpm_bundle_info[1] + "wrongbundle"],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_w_backward_connector_wrong",
            json.dumps(json_data))

        self.assertEqual(500, res.status_code)

    def test_1213_store_graph_with_backbone_with_forward_connector_nok_unresolvable_CPM_REFERENCED_BUNDLE_ID_in_forward_connector_namespace(
            self):
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: Namespace("prov", "http://www.wxsaxsa3.org/ns/prov#")[
                # wrong namespace- will not resolve
                self.prov_data.first_cpm_bundle_info[1] + "wrongbundle"],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _, _ = helpers.create_cpm_with_forward_connector(
            self.prov_data.existential_variable_prefix,
            provenance_storage_hospital_name,
            org_name=self.prov_data.org_id, main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            receiver_org_name=provenance_storage_pathology_name, timestamp=self.prov_data.timestamp,
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign.pem')

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(500, res.status_code)

    def test_1214_store_graph_with_backbone_with_forward_connector_nok_unresolvable_CPM_REFERENCED_META_BUNDLE_ID_in_forward_connector_namespace(
            self):
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace_2[self.prov_data.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: Namespace("prov", "http://www.wxsaxsa3.org/ns/prov#")[
                self.prov_data.first_cpm_bundle_info[1] + "wrongbundle"],  # wrong  namespace
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _, _ = helpers.create_cpm_with_forward_connector(
            self.prov_data.existential_variable_prefix,
            provenance_storage_hospital_name,
            org_name=self.prov_data.org_id, main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            receiver_org_name=provenance_storage_pathology_name, timestamp=self.prov_data.timestamp,
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign.pem')

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(500, res.status_code)

    def test_1215_store_graph_with_backbone_with_backwards_connector_nok_wrong_hash_in_backward_connector(
            self):
        id_suffix = "_w_backward_connector_wrong"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2] + "wrong",
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'Hash of bundle [pathology:e001_sample_backwards_connector] has '
                                   'wrong value.'},
                         json.loads(res.content))

    def test_1216_store_graph_with_backbone_with_backwards_connector_nok_wrong_hash_algorithm_in_backward_connector(
            self):
        id_suffix = "_w_backward_connector_wrong2"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "MD5"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'Hash of bundle [pathology:e001_sample_backwards_connector] has '
                                   'wrong value.'},
                         json.loads(res.content))

    def test_1217_store_graph_with_backbone_with_backwards_connector_nok_missing_relation_to_main_activity_from_forward_connector(
            self):
        id_suffix = "_w_backward_connector_wrong3"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        gen_connector = [x for x in backbone_with_backwards_connector if isinstance(x, ProvGeneration)][0]
        records_all = bundle._records
        records_all.remove(gen_connector)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({'error': 'CPM problem: Forward connector [pathology:example:e003_connector] '
                                   'not generated by main activity and not derived from other forward '
                                   'connector.'},
                         json.loads(res.content))

    def test_1218_store_graph_with_backbone_with_backwards_connector_nok_missing_relation_to_main_activity_from_backward_connector(
            self):
        id_suffix = "_w_backward_connector_wrong4"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        usage_connector = [x for x in backbone_with_backwards_connector if isinstance(x, ProvUsage)][0]
        records_all = bundle._records
        records_all.remove(usage_connector)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({'error': 'CPM problem: Backward connector '
                                   '[remote_bundle:pathology:e001_sample_backwards_connector] not used '
                                   'by main activity and no other backward connector derived from it.'},
                         json.loads(res.content))

    def test_1219_store_graph_with_backbone_with_backwards_connector_nok_missing_relation_to_main_activity_from_forward_connector(
            self):
        id_suffix = "_w_backward_connector_wrong5"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        generation_connectors = [x for x in backbone_with_backwards_connector if isinstance(x, ProvGeneration)]
        main_activity_generation_connector = [x for x in generation_connectors if str(first(
            x.get_attribute(PROV_ATTR_ACTIVITY))) == "pathology:test_main_activity"][0]
        records_all = bundle._records
        records_all.remove(main_activity_generation_connector)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({'error': 'CPM problem: Forward connector [pathology:example:e003_connector] '
                                   'not generated by main activity and not derived from other forward '
                                   'connector.'},
                         json.loads(res.content))

    def test_1220_store_graph_with_backbone_with_backwards_connector_nok_added_relation_to_main_activity_usage_of_not_backward_connector(
            self):
        id_suffix = "_w_backward_connector_wrong6"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        placeholder_time = datetime(2025, 1, 6, 15, 8, 24, 78915)
        entity_to_use = first(bundle.get_record("e003"))
        backbone_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]
        bundle.used(main_activity, entity_to_use, identifier="usage_main_act_2", time=placeholder_time,
                    other_attributes=[])

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'CPM problem: Main activity used entity that is not backward connector'},
                         json.loads(res.content))

    def test_1221_store_graph_with_backbone_with_backwards_connector_nok_added_relation_to_main_activity_generation_of_not_forward_connector(
            self):
        id_suffix = "_w_backward_connector_wrong7"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        placeholder_time = datetime(2025, 1, 6, 15, 8, 24, 78915)
        entity_to_generate = first(bundle.get_record("e003"))
        backbone_entities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_entities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]
        bundle.wasGeneratedBy(entity_to_generate, main_activity, placeholder_time, other_attributes=[])

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'CPM problem: Main activity generated entity that is not forward connector'},
                         json.loads(res.content))

    def test_1222_store_graph_with_backbone_with_backwards_connector_nok_missing_relation_to_sender_agent_from_backward_connector(
            self):
        id_suffix = "_w_backward_connector_wrong8"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        attribution_connector = [x for x in backbone_with_backwards_connector if isinstance(x, ProvAttribution)][0]
        records_all = bundle._records
        records_all.remove(attribution_connector)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'CPM problem: Sender agent is not attributed to backward connector'},
                         json.loads(res.content))

    def test_1223_store_graph_with_backbone_with_backwards_connector_nok_missing_sender_agent_for_backward_connector(
            self):
        id_suffix = "_w_backward_connector_wrong9"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        attribution_connector = [x for x in backbone_with_backwards_connector if isinstance(x, ProvAttribution)][0]
        backbone_agents = [x for x in backbone_with_backwards_connector if isinstance(x, ProvAgent)]
        sender_agent = [x for x in backbone_agents if first(x.get_attribute(PROV_TYPE)) == CPM_SENDER_AGENT][0]
        records_all = bundle._records
        records_all.remove(attribution_connector)
        records_all.remove(sender_agent)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'CPM problem: Backward connector does not have agent attributed'},
                         json.loads(res.content))

    def test_1224_store_graph_with_backbone_with_backwards_connector_nok_added_second_main_activity(
            self):
        id_suffix = "_w_backward_connector_wrong10"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        bundle.activity(
            identifier=f"pathology:test_main_activity_2",
            startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
            endTime=datetime(2025, 1, 7, 16, 8, 24, 78915).isoformat(" "),
            other_attributes=main_activity_attributes_2
        )

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Multiple 'mainActivity' activities specified inside of bundle "
                                   f'[test_{self.prov_data.timestamp}_bundle{id_suffix}]'},
                         json.loads(res.content))

    def test_1225_store_graph_with_backbone_with_backwards_connector_nok_added_connector_between_backbone_and_ds(
            self):
        id_suffix = "_w_backward_connector_wrong11"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        # add usage of domain entity by main activity - wrong
        placeholder_time = datetime(2025, 1, 6, 15, 8, 24, 78915)
        bb_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in bb_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]
        entity_to_connect = first(bundle.get_record("example:e003"))
        bundle.usage(entity_to_connect, main_activity, time=placeholder_time, identifier="example:usage_main_act_wrong",
                     other_attributes=[])

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "some error from app"},
                         json.loads(res.content))

    def test_1226_store_graph_with_backbone_with_backwards_connector_nok_backwards_connector_connected_to_same_bundle(
            self):
        # error is that the id was not found - probably ok since the bundle is not saved yet when it is being checked
        id_suffix = "_w_backward_connector_wrong12"
        main_activity_attributes_2 = self.create_main_activity_attributes(id_suffix=id_suffix)

        document_with_backwards_connector = ProvDocument()

        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle{id_suffix}"
        bundle_with_backwards_conn = document_with_backwards_connector.bundle(
            self.prov_data.bundle_namespace_with_backwards_connector[bundle_identifier])

        remote_bundle_namespace_2 = bundle_with_backwards_conn.add_namespace("remote_bundle_2",
                                                                             bundle_with_backwards_conn.identifier.namespace.uri)
        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_2[bundle_with_backwards_conn.identifier.localpart],
            # change referenced bundle id to the bundle being saved now
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn,
            self.prov_data.existential_variable_prefix,
            prev_cpm_bundle_info=self.prov_data.first_cpm_bundle_info,
            prev_meta_bundle_info=[self.prov_data.meta_bundle_id.namespace.uri,
                                   self.prov_data.meta_bundle_id.localpart],
            sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_2,
            backward_connector_attributes=backward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle{id_suffix}",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'Referenced bundle URI of connector '
                                   '[pathology:e001_sample_backwards_connector] not found.'},
                         json.loads(res.content))

    def test_135_store_graph_with_backbone_with_forward_connector_nok_missing_CPM_REFERENCED_META_BUNDLE_ID_in_forward_connector(
            self):
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace_2[self.prov_data.next_cpm_bundle_info[1]],
            # missing - should throw error
            # CPM_REFERENCED_META_BUNDLE_ID: self.remote_meta_bundle_namespace[
            #    self.prev_meta_bundle_info[1]
            # ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _, _ = helpers.create_cpm_with_forward_connector(
            self.prov_data.existential_variable_prefix,
            provenance_storage_hospital_name,
            org_name=self.prov_data.org_id, main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            receiver_org_name=provenance_storage_pathology_name, timestamp=self.prov_data.timestamp,
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign.pem')

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Forward connector(s) is/are missing mandatory attributes."},
                         json.loads(res.content))

    def test_136_store_graph_with_backbone_with_forward_connector_nok_missing_CPM_REFERENCED_BUNDLE_ID_in_forward_connector(
            self):
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            # CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace_2[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _, _ = helpers.create_cpm_with_forward_connector(
            self.prov_data.existential_variable_prefix,
            provenance_storage_hospital_name,
            org_name=self.prov_data.org_id, main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            receiver_org_name=provenance_storage_pathology_name, timestamp=self.prov_data.timestamp,
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign.pem')

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Forward connector(s) is/are missing mandatory attributes."},
                         json.loads(res.content))

    def test_137_store_graph_with_backbone_with_forward_connector_nok_missing_CPM_REFERENCED_BUNDLE_HASH_VALUE_in_forward_connector(
            self):
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace_2[self.prov_data.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace_2[
                self.prov_data.first_meta_bundle_info[1]
            ],
            # CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _, _ = helpers.create_cpm_with_forward_connector(
            self.prov_data.existential_variable_prefix,
            provenance_storage_hospital_name,
            org_name=self.prov_data.org_id, main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            receiver_org_name=provenance_storage_pathology_name, timestamp=self.prov_data.timestamp,
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign.pem')

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Forward connector(s) is/are missing mandatory attributes."},
                         json.loads(res.content))

    def test_138_store_graph_with_backbone_with_forward_connector_nok_missing_CPM_HASH_ALG_in_forward_connector(
            self):
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.prov_data.remote_bundle_namespace_2[self.prov_data.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.prov_data.remote_meta_bundle_namespace_2[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.next_cpm_bundle_info[2],
            # CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _, _ = helpers.create_cpm_with_forward_connector(
            self.prov_data.existential_variable_prefix,
            provenance_storage_hospital_name,
            org_name=self.prov_data.org_id, main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            receiver_org_name=provenance_storage_pathology_name, timestamp=self.prov_data.timestamp,
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign.pem')

        res = requests.post(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_with_forward_connector_wrong",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": "Forward connector(s) is/are missing mandatory attributes."},
                         json.loads(res.content))

    def test_1393_edit_graph_with_basic_backbone_nok_backward_connector_cycle(
            self):
        # prepare document - same as first document, then add backward connector
        doc_updated_2_nok, bundle_updated_2_nok, meta_bundle_id_updated_2_nok, backbone_basic_updated_2_nok, _, existential_variable_generator = helpers.create_cpm_provenance_basic(
            self.prov_data.existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=self.prov_data.org_id,
            main_activity_attributes=self.prov_data.main_activity_attributes_ok,
            timestamp=self.prov_data.timestamp,
            bundle_suffix="_updated_2")
        # add backward connector referencing middle component
        to_connect = f'hospital:e002'
        remote_bundle_namespace_2 = bundle_updated_2_nok.add_namespace("remote_bundle",
                                                                       self.prov_data.middle_cpm_bundle_info[0])
        remote_meta_bundle_namespace_2 = bundle_updated_2_nok.add_namespace(
            "remote_meta_bundle", self.prov_data.middle_meta_bundle_info[0]
        )
        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_2[self.prov_data.middle_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace_2[
                self.prov_data.middle_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.middle_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }
        main_activity = None
        for activity in bundle_updated_2_nok.get_records(ProvActivity):
            if activity.bundle.valid_qualified_name("cpm:mainActivity") in activity.get_asserted_types():
                main_activity = activity

        helpers.add_backward_connector(
            backward_connector_attributes, bundle_updated_2_nok, main_activity,
            provenance_storage_pathology_name,
            to_connect, remote_bundle_namespace="remote_bundle", backbone_parts=backbone_basic_updated_2_nok)

        json_data = create_json_for_doc_storing(doc_updated_2_nok)

        res = requests.put(
            # put - meaning this doc is update of previous, different bundle id in bundle but not in request
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))
        self.assertEqual(401, res.status_code)
        result = json.loads(res.content)  # add assert for error from app

    def test_1410_store_graph_with_basic_backbone_nok_added_external_ids_missing_alternateof(
            self):
        # create document - needs id different from previous
        doc2, bundle, _, _, _, _ = helpers.create_cpm_provenance_basic(self.prov_data.existential_variable_prefix,
                                                                       provenance_storage_pathology_name,
                                                                       self.prov_data.org_id2, self.prov_data.timestamp,
                                                                       self.prov_data.main_activity_attributes_pathology_meta,
                                                                       bundle_suffix="_with_ids_nok")

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
        # identifier_entity_1.alternateOf(entity_2) - removed

        json_data = create_json_for_doc_storing(doc2, private_key_location='test_sign2.pem')

        res = requests.post(
            f"http://{provenance_storage_pathology_url}/api/v1/organizations/{self.prov_data.org_id2}/documents/test_{self.prov_data.timestamp}_bundle_with_ids_nok",
            json.dumps(json_data))

        self.assertEqual(401, res.status_code)
        # add message test after adding check to app

    def test_1500_store_graph_with_backbone_end_update_nok_two_backward_connectors_missing_derivation_relation(
            self):
        # prepare fresh document same way as for ok data - to not change the right one because of other tests (in other tests similarly)
        document_with_backwards_connector_updated = ProvDocument()

        bundle_namespace = Namespace(
            provenance_storage_hospital_name,
            f"http://prov-storage-{provenance_storage_hospital_name}:8000/api/v1/organizations/{self.prov_data.org_id}/documents/",
        )
        bundle_identifier = f"test_{self.prov_data.timestamp}_bundle_end_updated_nok"
        bundle = document_with_backwards_connector_updated.bundle(bundle_namespace[bundle_identifier])
        bundle.add_namespace("example", "http://example.com#")

        remote_bundle_namespace_1 = bundle.add_namespace("remote_bundle_1",
                                                         self.prov_data.first_cpm_bundle_info[0])
        remote_meta_bundle_namespace_1 = bundle.add_namespace(
            "remote_meta_bundle_1", self.prov_data.first_meta_bundle_info[0]
        )
        remote_bundle_namespace_3 = bundle.add_namespace("remote_bundle_2",
                                                         self.prov_data.next_cpm_bundle_info[0])
        remote_meta_bundle_namespace_3 = bundle.add_namespace(
            "remote_meta_bundle_2", self.prov_data.next_meta_bundle_info[0]
        )

        backward_connector_attributes_1 = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_1[self.prov_data.first_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace_1[
                self.prov_data.first_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.first_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        backward_connector_attributes_2 = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_3[self.prov_data.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace_3[
                self.prov_data.next_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prov_data.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        document, bundle, meta_bundle_id_end, backbone_doc_with_backwards_connector_updated, last_cpm_bundle_info_3 = (
            helpers.create_cpm_provenance_basic_without_fw_connector_with_two_bw_connectors(
                document_with_backwards_connector_updated, bundle,
                self.prov_data.existential_variable_prefix,
                storage_name=provenance_storage_hospital_name,
                main_activity_attributes=self.prov_data.main_activity_attributes_ok_2,
                # add backward connectors to reference basic cpm doc, doc with both connectors
                sender_org_name_1=provenance_storage_hospital_name,
                backward_connector_attributes_1=backward_connector_attributes_1,
                sender_org_name_2=provenance_storage_pathology_name,
                backward_connector_attributes_2=backward_connector_attributes_2))

        # delete derivation relation
        derivation_connector = \
            [x for x in backbone_doc_with_backwards_connector_updated if isinstance(x, ProvDerivation)][0]
        records_all = bundle._records
        records_all.remove(derivation_connector)

        json_data = create_json_for_doc_storing(document)

        res = requests.put(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle_end",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'CPM problem: Backward connector '
                                   '[remote_bundle_1:hospital:e001_sample_backwards_connector] has many '
                                   'usages or is missing one or nothing was derived from it.'},
                         json.loads(res.content))

    def test_1501_store_graph_with_backbone_first_update_two_forward_connectors_missing_derivation_relation(
            self):
        doc_two_forward_conns, bundle, _, backbone_parts, first_with_two_conns_cpm_bundle_info = (
            helpers.create_cpm_with_forward_connector(
                "ex_",
                storage_name="hospital",
                org_name="org_id", main_activity_attributes=self.prov_data.main_activity_attributes_ok,
                receiver_org_name="pathology",
                timestamp=self.prov_data.timestamp, bundle_suffix="_with_both_forward_connectors",
                forward_connector_attributes=self.prov_data.forward_connector_attributes,
                add_second_forward_connector=True,
                second_forward_connector_attributes=self.prov_data.forward_connector_attributes_end,
                second_receiver_org_name="provenance_storage_hospital"))

        # delete derivation relation
        derivation_connector = [x for x in backbone_parts if isinstance(x, ProvDerivation)][0]
        records_all = bundle._records
        records_all.remove(derivation_connector)

        json_data = create_json_for_doc_storing(doc_two_forward_conns)

        res = requests.put(
            f"http://{provenance_storage_hospital_url}/api/v1/organizations/{self.prov_data.org_id}/documents/test_{self.prov_data.timestamp}_bundle",
            json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual({"error": 'CPM problem: Forward connector [hospital:hospital:e002_connector] '
                                   'has many generations or is missing one, or is not derived from '
                                   'other connector.'},
                         json.loads(res.content))

    def create_main_activity_attributes(self, id_suffix="", meta_ns=prov_data.meta_ns_pathology):
        """Creates attributes for main activity

        :param id_suffix: suffix of identifier of meta bundle that is to be deifned in main activity attrs
        :param meta_ns: meta namespace - Namespace of meta buncle identifier
        :returns: main_activity_attributes: dictionary with main activity attributes as defined in CPM
        """
        main_activity_attributes = {
            PROV_TYPE: CPM_MAIN_ACTIVITY,
            CPM_REFERENCED_META_BUNDLE_ID: meta_ns[
                f"test_{self.prov_data.timestamp}_bundle_meta{id_suffix}"
            ],
        }
        return main_activity_attributes
