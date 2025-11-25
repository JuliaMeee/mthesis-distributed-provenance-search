import json
from datetime import datetime
from functools import wraps
from unittest import TestCase
from unittest.mock import patch, MagicMock, NonCallableMagicMock

from OpenSSL.crypto import X509StoreContextError, X509
from cryptography.exceptions import InvalidSignature
from django.core.exceptions import ObjectDoesNotExist
from django.db.models.query_utils import select_related_descend
from requests import Request, Response
from urllib3 import request

from non_repudiation.controller import IsNotSubgraph
from non_repudiation.models import Document
from non_repudiation.views import info, organizations, specific_organization, store_cert, certs, retrieve_all_certs, \
    update_certificate, retrieve_document, retrieve_all_tokens, specific_token, issue_token, verify_signature
from trusted_party.settings import config


class MyTestCase(TestCase):

    def test_info_returns_info(self):
        req = Request()
        req.method = "GET"

        result = info(req)

        self.assertEqual(200, result.status_code)
        self.assertEqual({"id": config.id,
                          "certificate": config.cert}, json.loads(result.content))

    @patch('non_repudiation.controller.retrieve_organizations')
    def test_organizations_gets_returns_organizations(self, MockGetOrgs):
        req = Request()
        req.method = "GET"
        MockGetOrgs.return_value = ["org_test"]

        result = organizations(req)

        self.assertEqual(200, result.status_code)
        self.assertEqual(["org_test"], json.loads(result.content))

    @patch('non_repudiation.controller.retrieve_organizations')
    def test_organizations_ok_returns_no_org_response(self, MockGetOrgs):
        req = Request()
        req.method = "GET"
        MockGetOrgs.return_value = []

        result = organizations(req)

        self.assertEqual(200, result.status_code)
        self.assertEqual({"info": "No organizations registered yet."}, json.loads(result.content))

    @patch('non_repudiation.controller.retrieve_organization')
    def test_specific_organization_get_returns_org(self, MockGetOrg):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        org = {"org": "organization"}
        MockGetOrg.return_value = org

        result = specific_organization(req, org_id)

        MockGetOrg.assert_called_with(org_id)
        self.assertEqual(200, result.status_code)
        self.assertEqual(org, json.loads(result.content))

    @patch('non_repudiation.controller.retrieve_organization')
    def test_specific_organization_get_org_does_not_exist_returns_nok_response(self, MockGetOrg):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        MockGetOrg.side_effect = ObjectDoesNotExist("error")

        result = specific_organization(req, org_id)

        MockGetOrg.assert_called_with(org_id)
        self.assertEqual(404, result.status_code)
        self.assertEqual({"error": f"Organization with id [{org_id}] does not exist!"}, json.loads(result.content))

    @patch('non_repudiation.views.store_cert')
    def test_specific_organization_post_calls_store_returns_result(self, MockStoreOrg):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        MockStoreOrg.return_value = "test_val"

        result = specific_organization(req, org_id)

        MockStoreOrg.assert_called_with(req, org_id)
        self.assertEqual("test_val", result)

    def test_store_cert_missing_json_field_returns_nok_response(self):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        data = {"organizationId": "org_id",
                "clientCertificate": "certificate"}  # missing intermediateCertificates field
        req.body = json.dumps(data)

        result = store_cert(req, org_id)

        self.assertEqual({"error": f"Mandatory field [intermediateCertificates] not present in request!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    def test_store_cert_wrong_id_returns_nok_response(self):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        data = {"organizationId": "org_id_wrong", "clientCertificate": "certificate",
                "intermediateCertificates": "certs_test"}
        req.body = json.dumps(data)

        result = store_cert(req, org_id)

        self.assertEqual(
            {"error": f"Org ID from URI [{org_id}] does not match the one from request [{data['organizationId']}]!"},
            json.loads(result.content))
        self.assertEqual(400, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    def test_store_cert_org_already_registered_returns_nok_response(self, MockOrgs):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        data = {"organizationId": "org_id", "clientCertificate": "certificate",
                "intermediateCertificates": "certs_test"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = None

        result = store_cert(req, org_id)

        MockOrgs.get.assert_called_with(org_name=org_id)
        self.assertEqual({"error": f"Organization with id [{org_id}] is already registered!"},
                         json.loads(result.content))
        self.assertEqual(409, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_chain_of_trust')
    def test_store_cert_org_chain_of_trust_nok_returns_nok_response(self, MockChainOfTrustVerification, MockOrgs):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        data = {"organizationId": "org_id", "clientCertificate": "certificate",
                "intermediateCertificates": "certs_test"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = ObjectDoesNotExist()
        MockChainOfTrustVerification.side_effect = X509StoreContextError(message="message", errors=["error1"],
                                                                         certificate=X509())

        result = store_cert(req, org_id)

        MockOrgs.get.assert_called_with(org_name=org_id)
        self.assertEqual({"error": f"Could not verify the chain of trust!"},
                         json.loads(result.content))
        self.assertEqual(401, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_chain_of_trust')
    @patch('non_repudiation.controller.store_organization')
    def test_store_cert_org_ok_calls_store_cert_returns_ok_response(self, MockStoreOrg, MockChainOfTrustVerification, MockOrgs):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        data = {"organizationId": "org_id", "clientCertificate": "certificate",
                "intermediateCertificates": "certs_test"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = ObjectDoesNotExist()
        MockChainOfTrustVerification.side_effect = None

        result = store_cert(req, org_id)

        MockOrgs.get.assert_called_with(org_name=org_id)
        MockStoreOrg.assert_called_with(org_id, data['clientCertificate'], data['intermediateCertificates'])
        self.assertEqual(201, result.status_code)

    @patch('non_repudiation.views.retrieve_all_certs')
    def test_certs_get_calls_retrieve_certs_retuens_result(self, MockGetCerts):
        req = Request()
        req.method = "GET"
        MockGetCerts.return_value = "cert_test"
        org_id = "org_id"

        result = certs(req, org_id)

        MockGetCerts.assert_called_with(req, org_id)
        self.assertEqual("cert_test", result)

    @patch('non_repudiation.views.update_certificate')
    def test_certs_put_calls_update_cert_returns_result(self, MockUpdateCert):
        req = Request()
        req.method = "PUT"
        MockUpdateCert.return_value = "cert_test"
        org_id = "org_id"

        result = certs(req, org_id)

        MockUpdateCert.assert_called_with(req, org_id)
        self.assertEqual("cert_test", result)

    @patch('non_repudiation.controller.retrieve_organization')
    def test_retrieve_all_certs_nok_returns_nok_response(self, MockRetrieveOrgs):
        req = Request()
        req.method = "PUT"
        MockRetrieveOrgs.side_effect = ObjectDoesNotExist()
        org_id = "org_id"

        result = retrieve_all_certs(req, org_id)

        MockRetrieveOrgs.assert_called_with(org_id, True)
        self.assertEqual(404, result.status_code)
        self.assertEqual({"error": f"Organization with id [{org_id}] does not exist!"},
                         json.loads(result.content))

    @patch('non_repudiation.controller.retrieve_organization')
    def test_retrieve_all_certs_ok_calls_retirve_org_returns_result(self, MockRetrieveOrgs):
        req = Request()
        req.method = "PUT"
        MockRetrieveOrgs.return_value = {"sample_org": "org1"}
        org_id = "org_id"

        result = retrieve_all_certs(req, org_id)

        MockRetrieveOrgs.assert_called_with(org_id, True)
        self.assertEqual(200, result.status_code)
        self.assertEqual(MockRetrieveOrgs.return_value,
                         json.loads(result.content))

    def test_update_certificate_missing_field_returns_nok_response(self):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        data = {"clientCertificate": "certificate"}  # missing intermediateCertificates field
        req.body = json.dumps(data)

        result = update_certificate(req, org_id)

        self.assertEqual({"error": f"Mandatory field [intermediateCertificates] not present in request!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    def test_update_certificate_org_not_exists_returns_nok_response(self, MockOrgs):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        data = {"clientCertificate": "certificate", "intermediateCertificates": "certs_test"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = ObjectDoesNotExist()

        result = update_certificate(req, org_id)

        MockOrgs.get.assert_called_with(org_name=org_id)
        self.assertEqual({"error": f"Organization with id [{org_id}] does not exist!"},
                         json.loads(result.content))
        self.assertEqual(404, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_chain_of_trust')
    def test_update_certificate_org_not_exists_returns_nok_response(self, MockChainOfTrustVerification, MockOrgs):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        data = {"clientCertificate": "certificate", "intermediateCertificates": "certs_test"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = None
        MockChainOfTrustVerification.side_effect = X509StoreContextError(message="message", errors=["error1"],
                                                                         certificate=X509())

        result = update_certificate(req, org_id)

        MockOrgs.get.assert_called_with(org_name=org_id)
        MockChainOfTrustVerification.assert_called_with(data['clientCertificate'], data['intermediateCertificates'])
        self.assertEqual({"error": f"Could not verify the chain of trust!"},
                         json.loads(result.content))
        self.assertEqual(401, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_chain_of_trust')
    @patch('non_repudiation.controller.update_certificate')
    def test_update_certificate_ok_calls_update_cert_returns_certs(self, MockUpdateCert, MockChainOfTrustVerification, MockOrgs):
        req = Request()
        req.method = "POST"
        org_id = "org_id"
        data = {"clientCertificate": "certificate", "intermediateCertificates": "certs_test"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = None
        MockChainOfTrustVerification.side_effect = None

        result = update_certificate(req, org_id)

        MockOrgs.get.assert_called_with(org_name=org_id)
        MockChainOfTrustVerification.assert_called_with(data['clientCertificate'], data['intermediateCertificates'])
        MockUpdateCert.assert_called_with(org_id, data['clientCertificate'], data['intermediateCertificates'])
        self.assertEqual(201, result.status_code)

    @patch('non_repudiation.controller.retrieve_document')
    def test_retrieve_document_does_not_exist_returns_nok_response(self, MockGetDoc):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        MockGetDoc.side_effect = ObjectDoesNotExist("error")

        result = retrieve_document(req, org_id, doc_id, "json")

        MockGetDoc.assert_called_with(org_id, doc_id, doc_format="json")
        self.assertEqual(404, result.status_code)
        self.assertEqual({"error": f"No document wih id [{doc_id}] in format [json] exists for organization [{org_id}]"},
                         json.loads(result.content))

    @patch('non_repudiation.controller.retrieve_document')
    def test_retrieve_document_ok_returns_doc(self, MockGetDoc):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        doc = Document()
        doc.document_text = "doc_sample_text"
        doc.signature = "doc_sample_signature"

        MockGetDoc.return_value = doc

        result = retrieve_document(req, org_id, doc_id, "json")

        MockGetDoc.assert_called_with(org_id, doc_id, doc_format="json")
        self.assertEqual(200, result.status_code)
        self.assertEqual({"document": doc.document_text, "signature": doc.signature},
                         json.loads(result.content))

    @patch('non_repudiation.controller.retrieve_tokens')
    def test_retrieve_all_tokens_org_doesnt_exist_returns_nok_response(self, MockGetTokens):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        MockGetTokens.side_effect = ObjectDoesNotExist("error")

        result = retrieve_all_tokens(req, org_id)

        MockGetTokens.assert_called_with(org_id)
        self.assertEqual(404, result.status_code)
        self.assertEqual({"error": f"Organization with id [{org_id}] does not exist!"},
                         json.loads(result.content))

    @patch('non_repudiation.controller.retrieve_tokens')
    def test_retrieve_all_tokens_no_tokens_returns_nok_response(self, MockGetTokens):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        MockGetTokens.return_value = []

        result = retrieve_all_tokens(req, org_id)

        MockGetTokens.assert_called_with(org_id)
        self.assertEqual(404, result.status_code)
        self.assertEqual({"error": f"No tokens have been issued for organization with id [{org_id}]"},
                         json.loads(result.content))

    @patch('non_repudiation.controller.retrieve_tokens')
    def test_retrieve_all_tokens_ok_calls_retrieve_tokens_returns_result(self, MockGetTokens):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        MockGetTokens.return_value = ["token1"]

        result = retrieve_all_tokens(req, org_id)

        MockGetTokens.assert_called_with(org_id)
        self.assertEqual(200, result.status_code)
        self.assertEqual(MockGetTokens.return_value,
                         json.loads(result.content))

    @patch('non_repudiation.models.Organization.objects')
    def test_specific_token_org_not_exists_returns_nok_response(self, MockOrgs):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = ObjectDoesNotExist()

        result = specific_token(req, org_id, doc_id, "json")

        MockOrgs.get.assert_called_with(org_name=org_id)
        self.assertEqual(404, result.status_code)
        self.assertEqual({"error": f"Organization with id [{org_id}] does not exist!"},
                         json.loads(result.content))

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.retrieve_specific_token')
    def test_specific_token_doc_not_exists_returns_nok_response(self, MockGetToken, MockOrgs):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = None
        MockGetToken.side_effect = ObjectDoesNotExist()

        result = specific_token(req, org_id, doc_id, "json")

        MockOrgs.get.assert_called_with(org_name=org_id)
        MockGetToken.assert_called_with(org_id, doc_id, doc_format="json")
        self.assertEqual(404, result.status_code)
        self.assertEqual({"error": f"No document found with id [{doc_id}] in format [json] under organization [{org_id}]!"},
                         json.loads(result.content))

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.retrieve_specific_token')
    def test_specific_token_ok_calls_retrieve_token_returns_result(self, MockGetToken, MockOrgs):
        req = Request()
        req.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = None
        MockGetToken.return_value = {"token_id": "id..."}

        result = specific_token(req, org_id, doc_id, "json")

        MockOrgs.get.assert_called_with(org_name=org_id)
        MockGetToken.assert_called_with(org_id, doc_id, doc_format="json")
        self.assertEqual(200, result.status_code)
        self.assertEqual(MockGetToken.return_value,
                         json.loads(result.content))

    def test_issue_token_missing_field_returns_nok_response(self):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", 'documentFormat': "json", 'type': "meta"}  # missing createdOn field
        req.body = json.dumps(data)

        result = issue_token(req)

        self.assertEqual({"error": f"Mandatory field [createdOn] not present in request!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    def test_issue_token_wrong_type_returns_nok_response(self):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", 'documentFormat': "json", 'type': "wrong", "createdOn": "time..."}
        req.body = json.dumps(data)

        result = issue_token(req)

        self.assertEqual({"error": f"Incorrect type [{data['type']}, must be one of [subgraph|meta|graph]!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    def test_issue_token_missing_signature_returns_nok_response(self):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", 'documentFormat': "json", 'type': "graph",
                "createdOn": "time..."}  # type - graph - tp expects signature
        req.body = json.dumps(data)

        result = issue_token(req)

        self.assertEqual({"error": f"Mandatory field [\"signature\"] not present in request!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    def test_issue_token_wrong_time_returns_nok_response(self):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", 'documentFormat': "json", 'type': "graph",
                "createdOn": datetime.timestamp(datetime(2199, 1, 21, 2, 2, 4)),
                "signature": "sample_signature"} # wrong time - in future
        req.body = json.dumps(data)

        result = issue_token(req)

        self.assertEqual({"error": f"Incorrect timestamp for the document!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    def test_issue_token_organization_not_exists_returns_nok_response(self, MockOrgs):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", 'documentFormat': "json", 'type': "graph",
                "createdOn": datetime.timestamp(datetime.now()),
                "signature": "sample_signature"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = ObjectDoesNotExist()

        result = issue_token(req)

        MockOrgs.get.assert_called_with(org_name=data['organizationId'])
        self.assertEqual({"error": f"Organization with id [{data['organizationId']}] does not exist!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_signature')
    def test_issue_token_wrong_signature_returns_nok_response(self, MockVerifySignature, MockOrgs):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", 'documentFormat': "json", 'type': "graph",
                "createdOn": datetime.timestamp(datetime.now()),
                "signature": "sample_signature"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = None
        MockVerifySignature.side_effect = InvalidSignature()

        result = issue_token(req)

        MockOrgs.get.assert_called_with(org_name=data['organizationId'])
        MockVerifySignature.assert_called_with(data)
        self.assertEqual({"error": f"Invalid signature to the graph!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_signature')
    @patch('non_repudiation.controller.issue_token_and_store_doc')
    def test_issue_token_certificate_does_not_exist_returns_nok_response(self, MockIssueToken, MockVerifySignature, MockOrgs):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", 'documentFormat': "json", 'type': "graph",
                "createdOn": datetime.timestamp(datetime.now()),
                "signature": "sample_signature"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = None
        MockVerifySignature.side_effect = ObjectDoesNotExist("error")

        result = issue_token(req)

        MockOrgs.get.assert_called_with(org_name=data['organizationId'])
        MockVerifySignature.assert_called_with(data)
        self.assertEqual({'error': 'error'},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_signature')
    @patch('non_repudiation.controller.issue_token_and_store_doc')
    def test_issue_token_certificate_is_not_subgraph_returns_nok_response(self, MockIssueToken, MockVerifySignature, MockOrgs):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", 'documentFormat': "json", 'type': "graph",
                "createdOn": datetime.timestamp(datetime.now()),
                "signature": "sample_signature"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = None
        MockVerifySignature.side_effect = None
        MockIssueToken.side_effect = IsNotSubgraph("error")

        result = issue_token(req)

        MockOrgs.get.assert_called_with(org_name=data['organizationId'])
        MockIssueToken.assert_called_with(data)
        MockVerifySignature.assert_called_with(data)
        self.assertEqual({'error': 'error'},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_signature')
    @patch('non_repudiation.controller.issue_token_and_store_doc')
    def test_issue_token_certificate_calls_verify_sign_issue_token_returns_result(self, MockIssueToken, MockVerifySignature, MockOrgs):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", 'documentFormat': "json", 'type': "graph",
                "createdOn": datetime.timestamp(datetime.now()),
                "signature": "sample_signature"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = None
        MockVerifySignature.side_effect = None
        MockIssueToken.return_value = "token..."

        result = issue_token(req)

        MockOrgs.get.assert_called_with(org_name=data['organizationId'])
        MockIssueToken.assert_called_with(data)
        MockVerifySignature.assert_called_with(data)
        self.assertEqual(MockIssueToken.return_value,
                         json.loads(result.content))
        self.assertEqual(200, result.status_code)

    def test_verify_signature_missing_field_returns_nok_response(self):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc"}  # missing signature field
        req.body = json.dumps(data)

        result = verify_signature(req)

        self.assertEqual({"error": f"Mandatory field [signature] not present in request!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    def test_verify_signature_org_does_not_exist_returns_nok_response(self, MockOrgs):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", "signature": "sign"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockOrgs.get.side_effect = ObjectDoesNotExist()

        result = verify_signature(req)

        MockOrgs.get.assert_called_with(org_name=data["organizationId"])
        self.assertEqual({"error": f"Organization with id [{data['organizationId']}] does not exist!"},
                         json.loads(result.content))
        self.assertEqual(404, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_signature')
    def test_verify_signature_invalid_signature_returns_nok_response(self, MockVerifySignature, MockOrgs):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", "signature": "sign"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockVerifySignature.side_effect = InvalidSignature()

        result = verify_signature(req)

        MockOrgs.get.assert_called_with(org_name=data["organizationId"])
        MockVerifySignature.assert_called_with(data)
        self.assertEqual({"error": f"Invalid signature to the graph!"},
                         json.loads(result.content))
        self.assertEqual(400, result.status_code)

    @patch('non_repudiation.models.Organization.objects')
    @patch('non_repudiation.controller.verify_signature')
    def test_verify_signature_ok_calls_verify_sign_returns_ok_response(self, MockVerifySignature, MockOrgs):
        req = Request()
        req.method = "POST"
        data = {"organizationId": "org_id",
                "document": "doc", "signature": "sign"}
        req.body = json.dumps(data)
        MockOrgs.get = MagicMock()
        MockVerifySignature.side_effect = None

        result = verify_signature(req)

        MockOrgs.get.assert_called_with(org_name=data["organizationId"])
        MockVerifySignature.assert_called_with(data)
        self.assertEqual(200, result.status_code)
