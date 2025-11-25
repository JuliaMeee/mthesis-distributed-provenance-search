import random
import string
from base64 import b64encode, b64decode
from datetime import datetime
from pathlib import Path
from unittest import TestCase

import jcs
import requests
import json

from OpenSSL import crypto
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives._serialization import NoEncryption, Encoding, PrivateFormat
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509 import load_pem_x509_certificate
from prov.model import ProvDocument

from cert_helpers import generate_certificate, parse_certificate

trusted_party_url = "localhost:8020"


class MyTestCase(TestCase):
    org_id = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
    time_doc = datetime.timestamp(datetime(2025, 1, 21, 2, 2, 4))
    token = None

    # these tests should run after each other at once - they depend on data written to the running trusted party

    def test_11_get_info(self):
        res = requests.get("http://" + trusted_party_url + "/api/v1/info")
        self.assertEqual(200, res.status_code)

        result = json.loads(res.content)
        self.assertEqual("Trusted_Party", result["id"])
        self.assertIsNotNone(result["certificate"])

    def test_12_add_new_organization(self):
        with open('int1.pem', 'r') as file:
            intermediate_cert_1 = file.read()
        with open('int2.pem', 'r') as file:
            intermediate_cert_2 = file.read()

        key, client_cert = generate_certificate("SK", "test_cert", auth_key=Path("int2.key"),
                                                auth_cert=Path("int2.pem"),
                                                ca=False,
                                                path_length=None,
                                                )

        with open("test_cert.pem", "w") as file:
            file.seek(0)
            file.write(parse_certificate(client_cert, as_string=True))
        with open("test_sign.pem", "w") as file:
            file.seek(0)
            file.write(key.private_bytes(Encoding.PEM, PrivateFormat.PKCS8, NoEncryption()).decode())
        client_cert = parse_certificate(client_cert, as_string=True)

        json_data = {}
        json_data["organizationId"] = self.org_id
        json_data["clientCertificate"] = client_cert
        json_data["intermediateCertificates"] = [intermediate_cert_1, intermediate_cert_2]

        res = requests.post("http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id,
                            json.dumps(json_data))
        self.assertTrue(res.status_code == 201)

        res = requests.get("http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id + "/certs")
        self.assertEqual(200, res.status_code)

        with open('test_cert.pem', 'r') as file:
            org_certificate = file.read()

        result = json.loads(res.content)
        self.assertIsNotNone(result["certificate"])
        self.assertEqual(org_certificate, result["certificate"])

    # at least one organization should be registered
    def test_13_get_orgaizations(self):
        res = requests.get("http://" + trusted_party_url + "/api/v1/organizations")
        self.assertEqual(200, res.status_code)

        result = json.loads(res.content)
        self.assertTrue(len(result) > 0)
        self.assertIsNotNone(result[0]["id"])
        self.assertIsNotNone(result[0]["certificate"])
        crypto.load_certificate(crypto.FILETYPE_PEM, result[0]["certificate"].encode('utf-8'))

    def test_14_get_new_organization(self):
        res = requests.get("http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id)
        self.assertTrue(res.status_code == 200)
        with open('test_cert.pem', 'r') as file:
            org_certificate = file.read()

        result = json.loads(res.content)
        self.assertIsNotNone(result["id"])
        self.assertIsNotNone(result["certificate"])
        self.assertEqual(self.org_id, result["id"])
        self.assertEqual(org_certificate, result["certificate"])

    def test_15_get_certs_new_org(self):
        res = requests.get("http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id + "/certs")

        self.assertTrue(res.status_code == 200)
        with open('test_cert.pem', 'r') as file:
            org_certificate = file.read()

        result = json.loads(res.content)
        self.assertIsNotNone(result["certificate"])
        self.assertEqual(org_certificate, result["certificate"])

    def test_16_change_cert_new_org(self):
        with open('int1.pem', 'r') as file:
            intermediate_cert_1 = file.read()
        with open('int2.pem', 'r') as file:
            intermediate_cert_2 = file.read()

        key, client_cert = generate_certificate("SK", "test_cert", auth_key=Path("int2.key"),
                                                auth_cert=Path("int2.pem"),
                                                ca=False,
                                                path_length=None,
                                                )

        with open("test_cert.pem", "w") as file:
            file.seek(0)
            file.write(parse_certificate(client_cert, as_string=True))
        with open("test_sign.pem", "w") as file:
            file.seek(0)
            file.write(key.private_bytes(Encoding.PEM, PrivateFormat.PKCS8, NoEncryption()).decode())
        client_cert = parse_certificate(client_cert, as_string=True)

        json_data = {}
        json_data["organizationId"] = self.org_id
        json_data["clientCertificate"] = client_cert
        json_data["intermediateCertificates"] = [intermediate_cert_1, intermediate_cert_2]

        res = requests.put("http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id + "/certs",
                           json.dumps(json_data))
        self.assertTrue(res.status_code == 201)

    def test_17_get_certs_org(self):
        res = requests.get("http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id + "/certs")

        self.assertTrue(res.status_code == 200)
        with open('test_cert.pem', 'r') as file:
            org_certificate = file.read()

        result = json.loads(res.content)
        self.assertIsNotNone(result["certificate"])
        self.assertEqual(org_certificate, result["certificate"])
        self.assertIsNotNone(result["revokedCertificates"])
        self.assertEqual(1, len(result["revokedCertificates"]))

    def test_18_get_tokens_no_docs_stored(self):
        res = requests.get("http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id + "/tokens")

        self.assertEqual(404, res.status_code)
        self.assertEqual(f"No tokens have been issued for organization with id [{self.org_id}]",
                         json.loads(res.content)["error"])

    def test_19_issue_token_graph(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()
        with open('test_sign.pem', 'rb') as file:
            private_ke = file.read()
        private_key = serialization.load_pem_private_key(private_ke, password=None)

        json_data = {}
        json_data["organizationId"] = self.org_id
        json_data["document"] = b64encode(document.encode("utf-8")).decode("utf8")
        json_data["documentFormat"] = "json"
        json_data["type"] = "graph"
        json_data["createdOn"] = self.time_doc
        signature = private_key.sign(
            document.encode("utf8"), ec.ECDSA(hashes.SHA256())
        )
        json_data["signature"] = b64encode(signature).decode("utf-8")

        prov_document = ProvDocument.deserialize(
            content=document, format=json_data["documentFormat"]
        )

        prov_bundle = list(prov_document.bundles)[0]
        res = requests.post("http://" + trusted_party_url + "/api/v1/issueToken", json.dumps(json_data))

        self.assertEqual(200, res.status_code)
        res2 = json.loads(requests.get("http://" + trusted_party_url + "/api/v1/info").content)
        data = json.loads(res.content)

        # check data from token
        self.assertEqual(self.org_id, data["data"]["originatorId"])
        self.assertEqual("Trusted_Party", data["data"]["authorityId"])
        self.assertIsNotNone(data["data"]["tokenTimestamp"])
        time = datetime.fromtimestamp(data["data"]["tokenTimestamp"])
        self.assertIsNotNone(data["data"]["documentCreationTimestamp"])
        time2 = datetime.fromtimestamp(data["data"]["documentCreationTimestamp"])
        self.assertTrue(time < datetime.now() and time2 < datetime.now())
        digest = hashes.Hash(hashes.SHA256())
        digest.update(b64decode(json_data["document"]))
        hash = digest.finalize().hex()
        self.assertEqual(hash, data["data"]["documentDigest"])
        self.assertEqual(prov_bundle.identifier.uri, data["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", data["data"]["additionalData"]["hashFunction"])
        self.assertEqual("trusted-party:8020", data["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(res2["certificate"], data["data"]["additionalData"]["trustedPartyCertificate"])

        # verify signature
        public_key = load_pem_x509_certificate(res2["certificate"].encode("utf-8"),
                                               backend=default_backend()).public_key()
        public_key.verify(
            b64decode(data["signature"]), jcs.canonicalize(data["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_20_issue_token_graph_no_signnature(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()

        json_data = {}
        json_data["organizationId"] = self.org_id
        json_data["document"] = b64encode(document.encode("utf-8")).decode("utf8")
        json_data["documentFormat"] = "json"
        json_data["type"] = "graph"
        json_data["createdOn"] = self.time_doc

        res = requests.post("http://" + trusted_party_url + "/api/v1/issueToken", json.dumps(json_data))

        self.assertEqual(400, res.status_code)
        self.assertEqual("Mandatory field [\"signature\"] not present in request!", json.loads(res.content)["error"])

    def test_21_issue_token_graph_wrong_id(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()
        with open('test_sign.pem', 'rb') as file:
            private_ke = file.read()
        private_key = serialization.load_pem_private_key(private_ke, password=None)

        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()

        json_data = {}
        json_data["organizationId"] = self.org_id + "wrong_id"
        json_data["document"] = b64encode(document.encode("utf-8")).decode("utf8")
        json_data["documentFormat"] = "json"
        json_data["type"] = "graph"
        json_data["createdOn"] = self.time_doc
        signature = private_key.sign(
            document.encode("utf8"), ec.ECDSA(hashes.SHA256())
        )
        json_data["signature"] = b64encode(signature).decode("utf-8")

        res = requests.post("http://" + trusted_party_url + "/api/v1/issueToken", json.dumps(json_data))

        self.assertEqual(400, res.status_code)
        self.assertEqual(f"Organization with id [{json_data['organizationId']}] does not exist!",
                         json.loads(res.content)["error"])

    def test_22_issue_token_meta(self):
        json_data = {}
        json_data["organizationId"] = self.org_id
        json_data["document"] = b64encode("""{
  "prefix" : {
    "xsd" : "http://www.w3.org/2001/XMLSchema#",
    "prov" : "http://www.w3.org/ns/prov#",
    "ns_surgery" : "http://127.0.1.1:8000/api/v1/organizations/UniGrazzzz/documents/"
  },
  "bundle" : {
    "ns_surgery:01_sample_acquisition" : {
      "prefix" : {
        "cpm" : "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/",
        "dct" : "http://purl.org/dc/terms/",
        "self_connector" : "http://127.0.1.1:8000/api/v1/connectors/",
        "meta" : "http://127.0.1.1:8000/api/v1/documents/meta/",
        "xsd" : "http://www.w3.org/2001/XMLSchema#",
        "prov" : "http://www.w3.org/ns/prov#",
        "ns_surgery" : "http://127.0.1.1.com:8000/api/v1/organizations/UniGraz/documents/"
      },
      "entity" : {
        "ns_surgery:patient" : {
          "ns_surgery:bioptic-app-id" : [ "app-id-0" ],
          "cpm:externalId" : [ "patient-id-0" ]
        }
      }
    }
  }
}""".encode("utf-8")).decode("utf8")

        json_data["documentFormat"] = "json"
        json_data["type"] = "meta"
        json_data["createdOn"] = self.time_doc

        res = requests.post("http://" + trusted_party_url + "/api/v1/issueToken", json.dumps(json_data))
        self.assertEqual(200, res.status_code)
        data = json.loads(res.content)

        self.assertEqual(json_data["organizationId"], data["data"]["originatorId"])
        self.assertEqual("Trusted_Party", data["data"]["authorityId"])
        self.assertIsNotNone(data["data"]["tokenTimestamp"])
        time = datetime.fromtimestamp(data["data"]["tokenTimestamp"])
        self.assertIsNotNone(data["data"]["documentCreationTimestamp"])
        time2 = datetime.fromtimestamp(data["data"]["documentCreationTimestamp"])
        self.assertTrue(time < datetime.now() and time2 < datetime.now())
        digest = hashes.Hash(hashes.SHA256())
        digest.update(b64decode(json_data["document"]))
        hash = digest.finalize().hex()
        self.assertEqual(hash, data["data"]["documentDigest"])
        self.assertEqual("SHA256", data["data"]["additionalData"]["hashFunction"])
        self.assertEqual("trusted-party:8020", data["data"]["additionalData"]["trustedPartyUri"])

        # verify signature
        res2 = json.loads(requests.get("http://" + trusted_party_url + "/api/v1/info").content)
        data = json.loads(res.content)
        public_key = load_pem_x509_certificate(res2["certificate"].encode("utf-8"),
                                               backend=default_backend()).public_key()
        public_key.verify(
            b64decode(data["signature"]), jcs.canonicalize(data["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_23_issue_token_meta_wrong_id(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()

        json_data = {}
        json_data["organizationId"] = self.org_id + "wrong_id_does_not_matter"
        json_data["document"] = b64encode(document.encode("utf-8")).decode("utf8")
        json_data["documentFormat"] = "json"
        json_data["type"] = "meta"
        json_data["createdOn"] = self.time_doc

        res = requests.post("http://" + trusted_party_url + "/api/v1/issueToken", json.dumps(json_data))

        self.assertEqual(200, res.status_code)
        data = json.loads(res.content)

        self.assertEqual(json_data["organizationId"], data["data"]["originatorId"])
        self.assertEqual("Trusted_Party", data["data"]["authorityId"])
        self.assertIsNotNone(data["data"]["tokenTimestamp"])
        time = datetime.fromtimestamp(data["data"]["tokenTimestamp"])
        self.assertIsNotNone(data["data"]["documentCreationTimestamp"])
        time2 = datetime.fromtimestamp(data["data"]["documentCreationTimestamp"])
        self.assertTrue(time < datetime.now() and time2 < datetime.now())
        digest = hashes.Hash(hashes.SHA256())
        digest.update(b64decode(json_data["document"]))
        hash = digest.finalize().hex()
        self.assertEqual(hash, data["data"]["documentDigest"])
        self.assertEqual("SHA256", data["data"]["additionalData"]["hashFunction"])
        self.assertEqual("trusted-party:8020", data["data"]["additionalData"]["trustedPartyUri"])
        # verify signature
        res2 = json.loads(requests.get("http://" + trusted_party_url + "/api/v1/info").content)
        data = json.loads(res.content)
        public_key = load_pem_x509_certificate(res2["certificate"].encode("utf-8"),
                                               backend=default_backend()).public_key()
        public_key.verify(
            b64decode(data["signature"]), jcs.canonicalize(data["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_24_issue_token_backbone(self):
        json_data = {}
        json_data["organizationId"] = self.org_id
        json_data["document"] = b64encode("""{
          "prefix" : {
            "xsd" : "http://www.w3.org/2001/XMLSchema#",
            "prov" : "http://www.w3.org/ns/prov#",
            "ns_surgery" : "http://127.0.1.1:8000/api/v1/organizations/razzzz/documents_backbone/"
          },
          "bundle" : {
            "ns_surgery:01_sample_acquisition_backbone" : {
              "prefix" : {
                "cpm" : "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/",
                "dct" : "http://purl.org/dc/terms/",
                "self_connector" : "http://127.0.1.1:8000/api/v1/connectors/",
                "meta" : "http://127.0.1.1:8000/api/v1/documents/meta/",
                "xsd" : "http://www.w3.org/2001/XMLSchema#",
                "prov" : "http://www.w3.org/ns/prov#",
                "ns_surgery" : "http://127.0.1.1.com:8000/api/v1/organizations/UniGraz/documents/"
              },
              "entity" : {
                "ns_surgery:patient" : {
                  "ns_surgery:bioptic-app-id" : [ "app-id-0" ],
                  "cpm:externalId" : [ "patient-id-0" ]
                }
              }
            }
          }
        }""".encode("utf-8")).decode("utf8")
        json_data["documentFormat"] = "json"
        json_data["type"] = "backbone"
        json_data["createdOn"] = int(datetime.now().timestamp())

        res = requests.post("http://" + trusted_party_url + "/api/v1/issueToken", json.dumps(json_data))
        self.assertEqual(200, res.status_code)
        data = json.loads(res.content)

        self.assertEqual(200, res.status_code)
        self.assertEqual(json_data["organizationId"], data["data"]["originatorId"])
        self.assertEqual("Trusted_Party", data["data"]["authorityId"])
        self.assertIsNotNone(data["data"]["tokenTimestamp"])
        time = datetime.fromtimestamp(data["data"]["tokenTimestamp"])
        self.assertIsNotNone(data["data"]["documentCreationTimestamp"])
        time2 = datetime.fromtimestamp(data["data"]["documentCreationTimestamp"])
        self.assertTrue(time < datetime.now() and time2 < datetime.now())
        digest = hashes.Hash(hashes.SHA256())
        digest.update(b64decode(json_data["document"]))
        hash = digest.finalize().hex()
        self.assertEqual(hash, data["data"]["documentDigest"])
        self.assertEqual("SHA256", data["data"]["additionalData"]["hashFunction"])
        self.assertEqual("trusted-party:8020", data["data"]["additionalData"]["trustedPartyUri"])

        # verify signature
        res2 = json.loads(requests.get("http://" + trusted_party_url + "/api/v1/info").content)
        data = json.loads(res.content)
        public_key = load_pem_x509_certificate(res2["certificate"].encode("utf-8"),
                                               backend=default_backend()).public_key()
        public_key.verify(
            b64decode(data["signature"]), jcs.canonicalize(data["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_25_issue_token_backbone_wrong_id(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()

        json_data = {}
        json_data["organizationId"] = self.org_id + "wrong_id"
        json_data["document"] = b64encode(document.encode("utf-8")).decode("utf8")
        json_data["documentFormat"] = "json"
        json_data["type"] = "backbone"
        json_data["createdOn"] = self.time_doc

        res = requests.post("http://" + trusted_party_url + "/api/v1/issueToken", json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual(f"Organization with id [{json_data['organizationId']}] does not exist!",
                         json.loads(res.content)["error"])

    def test_26_verify_signature(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()
        with open('test_sign.pem', 'rb') as file:
            private_ke = file.read()
        private_key = serialization.load_pem_private_key(private_ke, password=None)

        json_data = {}
        json_data["organizationId"] = self.org_id
        json_data["document"] = b64encode(document.encode("utf-8")).decode("utf8")
        signature = private_key.sign(
            document.encode("utf8"), ec.ECDSA(hashes.SHA256())
        )
        json_data["signature"] = b64encode(signature).decode("utf-8")
        res = requests.post("http://" + trusted_party_url + "/api/v1/verifySignature", json.dumps(json_data))
        self.assertEqual(200, res.status_code)

    def test_27_verify_signature_wrong_signature(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()
        with open('test_sign.pem', 'rb') as file:
            private_ke = file.read()
        private_key = serialization.load_pem_private_key(private_ke, password=None)

        json_data = {}
        json_data["organizationId"] = self.org_id
        json_data["document"] = b64encode(document.encode("utf-8")).decode("utf8")
        signature = private_key.sign(
            "document - nok".encode("utf-8"), ec.ECDSA(hashes.SHA256())
        )
        json_data["signature"] = b64encode(signature).decode("utf-8")
        res = requests.post("http://" + trusted_party_url + "/api/v1/verifySignature", json.dumps(json_data))
        self.assertEqual(400, res.status_code)
        self.assertEqual("Invalid signature to the graph!", json.loads(res.content)["error"])

    def test_28_verify_signature_wrong_org_id(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()
        with open('test_sign.pem', 'rb') as file:
            private_ke = file.read()
        private_key = serialization.load_pem_private_key(private_ke, password=None)

        json_data = {}
        json_data["organizationId"] = self.org_id + "wrong_id"
        json_data["document"] = b64encode(document.encode("utf-8")).decode("utf8")
        signature = private_key.sign(
            document.encode("utf8"), ec.ECDSA(hashes.SHA256())
        )
        json_data["signature"] = b64encode(signature).decode("utf-8")
        res = requests.post("http://" + trusted_party_url + "/api/v1/verifySignature", json.dumps(json_data))
        self.assertEqual(404, res.status_code)
        self.assertEqual(f"Organization with id [{self.org_id}wrong_id] does not exist!",
                         json.loads(res.content)["error"])

    def test_29_retrieve_document(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()
        with open('test_cert.pem', 'rb') as file:
            cert = file.read()
        public_key = load_pem_x509_certificate(cert).public_key()

        prov_document = ProvDocument.deserialize(
            content=document, format="json"
        )

        prov_bundle = list(prov_document.bundles)[0]

        res = requests.get(
            "http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id + "/documents/" + prov_bundle.identifier.uri + "/json")
        self.assertEqual(200, res.status_code)

        result = json.loads(res.content)
        self.assertEqual(document.encode("utf-8"), b64decode(result["document"]))
        public_key.verify(
            b64decode(result["signature"]), b64decode(result["document"]), ec.ECDSA(hashes.SHA256())
        )

    def test_300_retrieve_token_for_doc(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()
        with open('test_cert.pem', 'rb') as file:
            cert = file.read()
        public_key = load_pem_x509_certificate(cert).public_key()

        prov_document = ProvDocument.deserialize(
            content=document, format="json"
        )

        prov_bundle = list(prov_document.bundles)[0]
        res = requests.get(
            "http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id + "/tokens/" +
            prov_bundle.identifier.uri + "/json")
        res2 = json.loads(requests.get("http://" + trusted_party_url + "/api/v1/info").content)
        self.assertEqual(200, res.status_code)

        # check tokens data
        data = json.loads(res.content)
        self.assertEqual(self.org_id, data["data"]["originatorId"])
        self.assertEqual("Trusted_Party", data["data"]["authorityId"])
        self.assertIsNotNone(data["data"]["tokenTimestamp"])
        time = datetime.fromtimestamp(data["data"]["tokenTimestamp"])
        self.assertIsNotNone(data["data"]["documentCreationTimestamp"])
        time2 = datetime.fromtimestamp(data["data"]["documentCreationTimestamp"])
        self.assertTrue(time < datetime.now() and time2 < datetime.now())
        self.assertEqual(prov_bundle.identifier.uri, data["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", data["data"]["additionalData"]["hashFunction"])
        self.assertEqual("trusted-party:8020", data["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(res2["certificate"], data["data"]["additionalData"]["trustedPartyCertificate"])

        # verify signature
        public_key = load_pem_x509_certificate(res2["certificate"].encode("utf-8"),
                                               backend=default_backend()).public_key()
        public_key.verify(
            b64decode(data["signature"]), jcs.canonicalize(data["data"]), ec.ECDSA(hashes.SHA256())
        )

    def test_301_retrieve_token_for_doc_wrong_id(self):
        res = requests.get(
            "http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id + "/tokens/" +
            "wrong_bundle_id" + "/json")

        self.assertEqual(404, res.status_code)
        self.assertEqual(f'No document found with id [wrong_bundle_id] in format [json] under '
                         f'organization [{self.org_id}]!', json.loads(res.content)["error"])

    def test_31_retrieve_token_for_org(self):
        with open('../non_repudiation_tests/01_sample_acquisition.json', 'r') as file:
            document = file.read()
        with open('test_cert.pem', 'rb') as file:
            cert = file.read()
        public_key = load_pem_x509_certificate(cert).public_key()

        prov_document = ProvDocument.deserialize(
            content=document, format="json"
        )

        prov_bundle = list(prov_document.bundles)[0]
        res = requests.get(
            "http://" + trusted_party_url + "/api/v1/organizations/" + self.org_id + "/tokens")
        res2 = json.loads(requests.get("http://" + trusted_party_url + "/api/v1/info").content)
        self.assertEqual(200, res.status_code)

        # check tokens data
        data = json.loads(res.content.decode("utf-8"))
        self.assertEqual(2, len(data))
        whole_graph = None
        backbone = None
        for x in data:
            if x["data"]["additionalData"]["bundle"] == prov_bundle.identifier.uri:
                whole_graph = x
            else:
                backbone = x

        self.assertEqual(self.org_id, whole_graph["data"]["originatorId"])
        self.assertEqual("Trusted_Party", whole_graph["data"]["authorityId"])
        self.assertIsNotNone(whole_graph["data"]["tokenTimestamp"])
        time = datetime.fromtimestamp(whole_graph["data"]["tokenTimestamp"])
        self.assertIsNotNone(whole_graph["data"]["documentCreationTimestamp"])
        time2 = datetime.fromtimestamp(whole_graph["data"]["documentCreationTimestamp"])
        self.assertTrue(time < datetime.now() and time2 < datetime.now())
        self.assertEqual(prov_bundle.identifier.uri, whole_graph["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", whole_graph["data"]["additionalData"]["hashFunction"])
        self.assertEqual("trusted-party:8020", whole_graph["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(res2["certificate"], whole_graph["data"]["additionalData"]["trustedPartyCertificate"])

        # verify signature
        public_key = load_pem_x509_certificate(res2["certificate"].encode("utf-8"),
                                               backend=default_backend()).public_key()
        public_key.verify(
            b64decode(whole_graph["signature"]), jcs.canonicalize(whole_graph["data"]), ec.ECDSA(hashes.SHA256())
        )

        self.assertEqual(self.org_id, backbone["data"]["originatorId"])
        self.assertEqual("Trusted_Party", backbone["data"]["authorityId"])
        self.assertIsNotNone(backbone["data"]["tokenTimestamp"])
        time = datetime.fromtimestamp(backbone["data"]["tokenTimestamp"])
        self.assertIsNotNone(backbone["data"]["documentCreationTimestamp"])
        time2 = datetime.fromtimestamp(backbone["data"]["documentCreationTimestamp"])
        self.assertTrue(time < datetime.now() and time2 < datetime.now())
        self.assertEqual(prov_bundle.identifier.uri + "_backbone", backbone["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", backbone["data"]["additionalData"]["hashFunction"])
        self.assertEqual("trusted-party:8020", backbone["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(res2["certificate"], backbone["data"]["additionalData"]["trustedPartyCertificate"])

        # verify signature
        public_key = load_pem_x509_certificate(res2["certificate"].encode("utf-8"),
                                               backend=default_backend()).public_key()
        public_key.verify(
            b64decode(backbone["signature"]), jcs.canonicalize(backbone["data"]), ec.ECDSA(hashes.SHA256())
        )
