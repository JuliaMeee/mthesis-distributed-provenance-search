import json
from functools import wraps
from unittest import TestCase
from unittest.mock import patch, MagicMock, NonCallableMagicMock

from requests import Request, Response

from provenance.views import *
from provenance.views import _validate_request_fields, _validate_request, _validate_update_conditions, \
    _validate_new_document_conditions, _parse_input_graph, _validate_duplicate_bundle


# using unittest, not django test, mocking django decorators - these are unit tests, api with calls tested
# in component/integration tests where whoe app will be running, in unit tests, functions are tested without rest of the app

class MyTestCase(TestCase):
    def mock_decorator(*args, **kwargs):
        def decorator(f):
            @wraps(f)
            def decorated_function(*args, **kwargs):
                return f(*args, **kwargs)

            return decorated_function

        return decorator

    def setUp(self):
        patch('django.views.decorators.csrf.csrf_exempt', lambda x: x).start()
        patch('django.views.decorators.http.require_http_methods', self.mock_decorator).start()
        patch('django.views.decorators.http.require_safe', lambda x: x).start()
        patch('django.views.decorators.http.require_GET', lambda x: x).start()

    @patch('provenance.views.register_org')
    def test_register_post(self, MockRegisterOrg):
        MockRegisterOrg.return_value = requests.Response()
        MockRegisterOrg.return_value.status_code = 201
        request = requests.Request()
        request.method = "POST"
        from provenance.views import register  # import here because of mocked decorator
        response = register(request, "org_id")

        self.assertEqual(201, response.status_code)
        MockRegisterOrg.assert_called_with(request, "org_id")

    @patch('provenance.views.modify_org')
    def test_register_put(self, MockModifyOrg):
        MockModifyOrg.return_value = requests.Response
        MockModifyOrg.return_value.status_code = 200
        request = requests.Request()
        request.method = "PUT"
        from provenance.views import register
        response = register(request, "org_id")

        self.assertEqual(200, response.status_code)
        MockModifyOrg.assert_called_with(request, "org_id")

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.send_register_request_to_tp')
    @patch('provenance.views.db')
    @patch('provenance.controller.create_and_store_organization')
    @patch('provenance.views.HttpResponse', autospec=True)
    # mocking httpresponse because of not using django - checking what it was called with and return value for it is set
    # this return value is then checked whether it was returned from function
    def test_register_org(self, MockHttpResponse, MockStoreOrg, MockDb, MockSendRegisterRequest,
                          MockGetOrganizations, MockDoesNotExistException):
        # set up to trow exception when getting org - we want to create new one
        MockGetOrganizations.get = MagicMock()
        MockGetOrganizations.get.return_value = None
        MockGetOrganizations.get.side_effect = DoesNotExist("nok")

        # mock response from trusted party by mocking function calling it
        MockSendRegisterRequest.return_value = requests.Response()
        MockSendRegisterRequest.return_value.status_code = 201

        MockDb.transaction.side_effect = None
        tp = controller.TrustedParty()
        tp.identifier = "tp"

        request = requests.Request()
        request.method = "PUT"
        # add expected json fields with test data
        request.body = json.dumps(
            {"clientCertificate": "cert", "intermediateCertificates": "certs_test", "TrustedPartyUri": "tp_uri"})
        response = Response()
        MockHttpResponse.return_value = response

        result = register_org(request, "org_id")

        MockHttpResponse.assert_called_with(status=201)
        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockSendRegisterRequest.assert_called_with(json.loads(request.body), "org_id")
        org, client_cert, interm_certs, tp = MockStoreOrg.call_args[0]
        self.assertEqual("org_id", org)
        self.assertEqual("cert", client_cert)
        self.assertEqual("certs_test", interm_certs)
        self.assertEqual("tp_uri", tp)
        self.assertEqual(response, result)

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.db')
    @patch('provenance.controller.get_tp')
    @patch('provenance.controller.store_organization')
    @patch('provenance.views.JsonResponse', autospec=True)
    def test_register_org_nok_json(self, MockJsonResponse, MockStoreOrg, MockGetTp, MockDb,
                                   MockGetOrganizations, MockDoesNotExistException):
        # set up to trow exception when getting org - we want to create new one
        MockGetOrganizations.get = MagicMock()
        MockGetOrganizations.get.return_value = None
        MockGetOrganizations.get.side_effect = DoesNotExist("nok")

        MockDb.transaction.side_effect = None
        tp = controller.TrustedParty()
        tp.identifier = "tp"
        MockGetTp.return_value = tp, False

        request = requests.Request()
        request.method = "PUT"
        # not adding all required data
        request.body = json.dumps({"intermediateCertificates": "certs_test"})
        response = Response()
        MockJsonResponse.return_value = response

        organization_id = "org_id"
        result = register_org(request, organization_id)

        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockJsonResponse.assert_called_with({"error": f"Mandatory field [clientCertificate] not present in request!"},
                                            status=400)
        self.assertEqual(response, result)

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.db')
    @patch('provenance.controller.get_tp')
    @patch('provenance.controller.store_organization')
    @patch('provenance.views.JsonResponse', autospec=True)
    def test_register_org_nok_json2(self, MockJsonResponse, MockStoreOrg, MockGetTp, MockDb, MockGetOrganizations,
                                    MockDoesNotExistException):
        # set up to trow exception when getting org - we want to create new one
        MockGetOrganizations.get = MagicMock()
        MockGetOrganizations.get.return_value = None
        MockGetOrganizations.get.side_effect = DoesNotExist("nok")

        MockDb.transaction.side_effect = None
        tp = controller.TrustedParty()
        tp.identifier = "tp"
        MockGetTp.return_value = tp, False
        MockStoreOrg.reture_value = None

        request = requests.Request()
        request.method = "PUT"
        # not adding all required data
        request.body = json.dumps({"clientCertificate": "certs_test"})
        response = Response()
        MockJsonResponse.return_value = response

        organization_id = "org_id"
        result = register_org(request, organization_id)

        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockJsonResponse.assert_called_with(
            {"error": f"Mandatory field [intermediateCertificates] not present in request!"},
            status=400)
        self.assertEqual(response, result)

    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.JsonResponse', autospec=True)
    def test_register_org_nok_org_registered(self, MockJsonResponse, MockGetOrganizations):
        # set up not to throw exception - org already registered
        MockGetOrganizations.get = MagicMock()
        MockGetOrganizations.get.return_value = None

        request = requests.Request()
        request.method = "PUT"
        # add expected json fields with test data
        request.body = json.dumps({"clientCertificate": "cert", "intermediateCertificates": "certs_test"})
        response = Response()
        MockJsonResponse.return_value = response

        organization_id = "org_id"
        result = register_org(request, organization_id)

        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockJsonResponse.assert_called_with({
            "error": f"Organization with id [{organization_id}] is already registered. "
                     f"If you want to modify it, send PUT request!"
        },
            status=409)
        self.assertEqual(response, result)

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.send_register_request_to_tp')
    @patch('provenance.views.db')
    @patch('provenance.controller.create_and_store_organization')
    @patch('provenance.views.HttpResponse', autospec=True)
    def test_register_org_new_tp(self, MockHttpResponse, MockStoreOrg, MockDb, MockSendRegisterRequest,
                                 MockGetOrganizations, MockDoesNotExistException):
        # set up to trow exception when getting org - we want to create new one
        MockGetOrganizations.get = MagicMock()
        MockGetOrganizations.get.return_value = None
        MockGetOrganizations.get.side_effect = DoesNotExist("nok")

        # mock response from trusted party by mocking function calling it
        MockSendRegisterRequest.return_value = requests.Response()
        MockSendRegisterRequest.return_value.status_code = 201

        MockDb.transaction.side_effect = None
        tp = controller.TrustedParty()
        tp.identifier = "tp"
        tp.save = MagicMock()

        request = requests.Request()
        request.method = "PUT"
        # add expected json fields with test data
        request.body = json.dumps({"clientCertificate": "cert", "intermediateCertificates": "certs_test"})
        response = Response()
        MockHttpResponse.return_value = response

        result = register_org(request, "org_id")

        MockHttpResponse.assert_called_with(status=201)
        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockSendRegisterRequest.assert_called_with(json.loads(request.body), "org_id")
        org, client_cert, interm_certs, tp = MockStoreOrg.call_args[0]
        self.assertEqual("org_id", org)
        self.assertEqual("cert", client_cert)
        self.assertEqual("certs_test", interm_certs)
        self.assertEqual(None, tp)
        self.assertEqual(response, result)

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.send_register_request_to_tp')
    @patch('provenance.views.db')
    @patch('provenance.controller.modify_organization')
    @patch('provenance.views.HttpResponse', autospec=True)
    def test_modify_org(self, MockHttpResponse, MockStoreOrg, MockDb, MockSendRegisterRequest,
                        MockGetOrganizations, MockDoesNotExistException):
        # mock get organization - organization to modify
        MockGetOrganizations.get = MagicMock()
        org = controller.Organization()
        org.identifier = "id1"
        MockGetOrganizations.get.return_value = org

        # mock response from trusted party by mocking function calling it
        MockSendRegisterRequest.return_value = requests.Response()
        MockSendRegisterRequest.return_value.status_code = 201

        MockDb.transaction.side_effect = None
        tp = controller.TrustedParty()
        tp.identifier = "tp"

        request = requests.Request()
        request.method = "PUT"
        # add expected json fields with test data
        request.body = json.dumps(
            {"clientCertificate": "cert", "intermediateCertificates": "certs_test", "TrustedPartyUri": "tp_uri"})
        response = Response()
        MockHttpResponse.return_value = response

        result = modify_org(request, "org_id")

        MockHttpResponse.assert_called_with(status=200)
        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockSendRegisterRequest.assert_called_with(json.loads(request.body), "org_id", is_post=False)
        org, client_cert, interm_certs, tp = MockStoreOrg.call_args[0]
        self.assertEqual("org_id", org)
        self.assertEqual("cert", client_cert)
        self.assertEqual("certs_test", interm_certs)
        self.assertEqual("tp_uri", tp)
        self.assertEqual(response, result)

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.db')
    @patch('provenance.controller.get_tp')
    @patch('provenance.controller.store_organization')
    @patch('provenance.views.JsonResponse', autospec=True)
    def test_modify_org_nok_json(self, MockJsonResponse, MockStoreOrg, MockGetTp, MockDb,
                                 MockGetOrganizations, MockDoesNotExistException):
        # mock get organization - organization to modify
        MockGetOrganizations.get = MagicMock()
        org = controller.Organization()
        org.identifier = "id1"
        MockGetOrganizations.get.return_value = org

        MockDb.transaction.side_effect = None
        tp = controller.TrustedParty()
        tp.identifier = "tp"
        MockGetTp.return_value = tp, False

        request = requests.Request()
        request.method = "PUT"
        # not adding all required data
        request.body = json.dumps({"intermediateCertificates": "certs_test"})
        response = Response()
        MockJsonResponse.return_value = response

        organization_id = "org_id"
        result = modify_org(request, organization_id)

        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockJsonResponse.assert_called_with({"error": f"Mandatory field [clientCertificate] not present in request!"},
                                            status=400)
        self.assertEqual(response, result)

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.db')
    @patch('provenance.controller.get_tp')
    @patch('provenance.controller.store_organization')
    @patch('provenance.views.JsonResponse', autospec=True)
    def test_modify_org_nok_json2(self, MockJsonResponse, MockStoreOrg, MockGetTp, MockDb, MockGetOrganizations,
                                  MockDoesNotExistException):
        # mock get organization - organization to modify
        MockGetOrganizations.get = MagicMock()
        org = controller.Organization()
        org.identifier = "id1"
        MockGetOrganizations.get.return_value = org

        MockDb.transaction.side_effect = None
        tp = controller.TrustedParty()
        tp.identifier = "tp"
        MockGetTp.return_value = tp, False
        MockStoreOrg.reture_value = None

        request = requests.Request()
        request.method = "PUT"
        # not adding all required data
        request.body = json.dumps({"clientCertificate": "certs_test"})
        response = Response()
        MockJsonResponse.return_value = response

        organization_id = "org_id"
        result = modify_org(request, organization_id)

        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockJsonResponse.assert_called_with(
            {"error": f"Mandatory field [intermediateCertificates] not present in request!"},
            status=400)
        self.assertEqual(response, result)

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.JsonResponse', autospec=True)
    def test_modify_org_nok_org_not_registered(self, MockJsonResponse, MockGetOrganizations, MockDoesNotExistException):
        # set up to trow exception when getting org - error
        MockGetOrganizations.get = MagicMock()
        MockGetOrganizations.get.return_value = None
        MockGetOrganizations.get.side_effect = DoesNotExist("nok")

        request = requests.Request()
        request.method = "PUT"
        # add expected json fields with test data
        request.body = json.dumps({"clientCertificate": "cert", "intermediateCertificates": "certs_test"})
        response = Response()
        MockJsonResponse.return_value = response

        organization_id = "org_id"
        result = modify_org(request, organization_id)

        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockJsonResponse.assert_called_with({
            "error": f"Organization with id [{organization_id}] is not registered!"
        },
            status=404)
        self.assertEqual(response, result)

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.views.send_register_request_to_tp')
    @patch('provenance.views.db')
    @patch('provenance.controller.get_tp')
    @patch('provenance.controller.modify_organization')
    @patch('provenance.views.HttpResponse', autospec=True)
    def test_modify_org_new_tp(self, MockHttpResponse, MockStoreOrg, MockGetTp, MockDb, MockSendRegisterRequest,
                               MockGetOrganizations, MockDoesNotExistException):
        # mock get organization - organization to modify
        MockGetOrganizations.get = MagicMock()
        org = controller.Organization()
        org.identifier = "id1"
        MockGetOrganizations.get.return_value = org

        # mock response from trusted party by mocking function calling it
        MockSendRegisterRequest.return_value = requests.Response()
        MockSendRegisterRequest.return_value.status_code = 201

        MockDb.transaction.side_effect = None
        tp = controller.TrustedParty()
        tp.identifier = "tp"
        tp.save = MagicMock()
        MockGetTp.return_value = tp, True

        request = requests.Request()
        request.method = "PUT"
        # add expected json fields with test data
        request.body = json.dumps({"clientCertificate": "cert", "intermediateCertificates": "certs_test"})
        response = Response()
        MockHttpResponse.return_value = response

        result = modify_org(request, "org_id")

        MockHttpResponse.assert_called_with(status=200)
        MockGetOrganizations.get.assert_called_with(identifier="org_id")
        MockSendRegisterRequest.assert_called_with(json.loads(request.body), "org_id", is_post=False)
        org, client_cert, interm_certs, tp = MockStoreOrg.call_args[0]
        self.assertEqual("org_id", org)
        self.assertEqual("cert", client_cert)
        self.assertEqual("certs_test", interm_certs)
        self.assertEqual(None, tp)
        self.assertEqual(response, result)

    @patch('provenance.views.requests')
    def test_send_register_request_to_tp_post(self, MockRequests):
        payload = {"TrustedPartyUri": "tp_uri", "organizationId": "org_id"}
        MockRequests.post = MagicMock()
        MockRequests.post.return_value = 4032  # just random number to check later

        result = send_register_request_to_tp(payload, "org_id")
        self.assertEqual(MockRequests.post.return_value, result)
        MockRequests.post.assert_called_with("http://tp_uri/api/v1/organizations/org_id", json.dumps(payload))

    @patch('provenance.views.requests')
    def test_send_register_request_to_tp_put(self, MockRequests):
        payload = {"organizationId": "org_id"}
        MockRequests.put = MagicMock()
        MockRequests.put.return_value = 4032  # just random number to check later

        result = send_register_request_to_tp(payload, "org_id", is_post=False)
        self.assertEqual(MockRequests.put.return_value, result)
        MockRequests.put.assert_called_with("http://" + config.tp_fqdn + "/api/v1/organizations/org_id",
                                            json.dumps(payload))

    @patch('provenance.views.store_graph')
    def test_document_post(self, MockStoreGraph):
        MockStoreGraph.return_value = requests.Response()
        MockStoreGraph.return_value.status_code = 201
        request = requests.Request()
        request.method = "POST"
        response = document(request, "org_id", "doc_id")

        self.assertEqual(201, response.status_code)
        MockStoreGraph.assert_called_with(request, "org_id", "doc_id")

    @patch('provenance.views.store_graph')
    def test_document_put(self, MockStoreGraph):
        MockStoreGraph.return_value = requests.Response()
        MockStoreGraph.return_value.status_code = 201
        request = requests.Request()
        request.method = "PUT"
        response = document(request, "org_id", "doc_id")

        self.assertEqual(201, response.status_code)
        MockStoreGraph.assert_called_with(request, "org_id", "doc_id", is_update=True)

    @patch('provenance.controller.bundle_exists')
    @patch('provenance.views.HttpResponse', autospec=True)
    def test_document_head(self, MockHttpResponse, MockBundleExists):
        MockBundleExists.return_value = True
        request = requests.Request()
        request.method = "HEAD"
        response = document(request, "org_id", "doc_id")

        self.assertIsInstance(response, NonCallableMagicMock)
        MockBundleExists.assert_called_with("org_id_doc_id")
        MockHttpResponse.assert_called_with(200)

    @patch('provenance.controller.bundle_exists')
    @patch('provenance.views.HttpResponseNotFound', autospec=True)
    def test_document_head_false(self, MockHttpResponse, MockBundleExists):
        MockBundleExists.return_value = False
        request = requests.Request()
        request.method = "HEAD"
        response = document(request, "org_id", "doc_id")

        self.assertIsInstance(response, NonCallableMagicMock)
        MockBundleExists.assert_called_with("org_id_doc_id")
        MockHttpResponse.assert_called_once()

    @patch('provenance.views.get_graph')
    def test_document_get(self, MockGetGraph):
        graph = controller.Document()
        MockGetGraph.return_value = graph
        request = requests.Request()
        request.method = "GET"
        response = document(request, "org_id", "doc_id")

        self.assertEqual(graph, response)
        MockGetGraph.assert_called_with(request, "org_id", "doc_id")

    @patch("provenance.views.InputGraphChecker")
    @patch("provenance.controller.check_connectors")
    @patch("provenance.views._validate_request")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.controller.send_token_request_to_tp")
    @patch("provenance.views.import_graph")
    @patch("provenance.controller.get_token_to_store_into_db")
    @patch('provenance.views.db')
    @patch("provenance.controller.store_token_into_db")
    @patch('provenance.views.JsonResponse', autospec=True)
    @patch('provenance.views.PROV_VALIDATOR')
    def test_store_graph(self, MockValidityChecker, MockJsonResponse, MockStoreToken, MockDb, MockGetToken, MockImportGraph,
                         MockTokenRequest, MockGetTpUrlByOrg,
                         MockValidateRequest, MockCheckConnectors, MockGraphChecker):
        request = requests.Request()
        request.get_full_path = MagicMock()
        request.get_full_path.return_value = "https://full-path"
        request.method = "PUT"
        # add expected json fields with test data
        data = {"documentFormat": "json", "document": "doc_example"}
        request.body = json.dumps(data)

        MockCheckConnectors.return_value = True
        MockValidateRequest.return_value = None

        # tp is allowed, therefore:
        MockGetTpUrlByOrg.return_value = "https://tp_url"
        token = get_dummy_token()
        MockTokenRequest.return_value = token

        validator_mock = MagicMock()
        validator_mock.get_document.return_value = "document_sample"
        validator_mock.get_meta_provenance_id.return_value = "meta_id_sample"
        validator_mock.get_bundle_id.return_value = "bundle_id"
        MockGraphChecker.return_value = validator_mock

        MockGetToken.return_value = "token_test", "neo_doc_test", "trusted_party_test"
        MockDb.transaction.side_effect = None
        response = Response()
        MockJsonResponse.return_value = response


        result = store_graph(request, "org_id", "doc_id")


        MockGraphChecker.assert_called_with(data["document"], data["documentFormat"], 'https://full-path', MockValidityChecker)
        MockValidateRequest.assert_called_with(json.loads(request.body), validator_mock, "doc_id", "org_id", False,
                                               False)  # MockGraphChecker probably wrong
        MockGetTpUrlByOrg.assert_called_with("org_id")
        payload = data.copy()
        payload["organizationId"] = "org_id"
        payload["type"] = "graph"
        payload["graphId"] = "doc_id"
        MockTokenRequest.assert_called_with(payload, "https://tp_url")
        MockImportGraph.assert_called_with("document_sample", data, token, "meta_id_sample", "doc_id", False)
        MockGetToken.assert_called_with(token, "bundle_id")
        MockStoreToken.assert_called_with("token_test", "neo_doc_test", "trusted_party_test")
        MockJsonResponse.assert_called_with({"token": token},
                                            status=201)
        self.assertEqual(response, result)

    @patch("provenance.views.InputGraphChecker")
    @patch("provenance.controller.check_connectors")
    @patch("provenance.views._validate_request")
    @patch("provenance.views.import_graph")
    @patch('provenance.views.db')
    @patch('provenance.views.JsonResponse', autospec=True)
    @patch('provenance.views.PROV_VALIDATOR')
    def test_store_graph_disabled_tp(self, MockValidityChecker, MockJsonResponse, MockDb, MockImportGraph,
                                     MockValidateRequest, MockCheckConnectors, MockGraphChecker):
        request = requests.Request()
        request.method = "PUT"
        request.get_full_path = MagicMock()
        request.get_full_path.return_value = "https://full-path"
        # add expected json fields with test data
        data = {"documentFormat": "json", "document": "doc_example"}
        request.body = json.dumps(data)

        # disable_tp
        config.disable_tp = True

        MockCheckConnectors.return_value = True
        MockValidateRequest.return_value = None

        validator_mock = MagicMock()
        validator_mock.get_document.return_value = "document_sample"
        validator_mock.get_meta_provenance_id.return_value = "meta_id_sample"
        validator_mock.get_bundle_id.return_value = "bundle_id"
        MockGraphChecker.return_value = validator_mock

        MockDb.transaction.side_effect = None
        response = Response()
        MockJsonResponse.return_value = response

        result = store_graph(request, "org_id", "doc_id")

        MockGraphChecker.assert_called_with(data["document"], data["documentFormat"], "https://full-path", MockValidityChecker)
        MockValidateRequest.assert_called_with(json.loads(request.body), validator_mock, "doc_id", "org_id", False,
                                               True)  # MockGraphChecker probably wrong
        payload = data.copy()
        payload["organizationId"] = "org_id"
        payload["type"] = "graph"
        payload["graphId"] = "doc_id"
        MockImportGraph.assert_called_with("document_sample", data, get_dummy_token("org_id"), "meta_id_sample", "doc_id", False)
        MockJsonResponse.assert_called_with(
            {"info": "Trusted party is disabled therefore no token has been issued, however graph has been stored."},
            status=201)
        config.disable_tp = False
        self.assertEqual(response, result)

    @patch("provenance.views.InputGraphChecker")
    @patch("provenance.controller.check_connectors")
    @patch("provenance.views._validate_request")
    def test_store_graph_validation_error(self,
                                          MockValidateRequest, MockCheckConnectors, MockGraphChecker):
        request = requests.Request()
        request.method = "PUT"
        # add expected json fields with test data
        data = {"documentFormat": "json", "document": "doc_example"}
        request.body = json.dumps(data)

        MockCheckConnectors.return_value = True
        MockValidateRequest.return_value = "error"

        result = store_graph(request, "org_id", "doc_id")

        MockGraphChecker.assert_called_with(data["document"], data["documentFormat"])

        self.assertEqual("error", result)

    @patch("provenance.views.InputGraphChecker")
    @patch("provenance.controller.check_connectors")
    @patch("provenance.views._validate_request")
    @patch('provenance.views.PROV_VALIDATOR')
    def test_store_graph_validation_error(self, MockValidityChecker,
                                          MockValidateRequest, MockCheckConnectors, MockGraphChecker):
        request = requests.Request()
        request.method = "PUT"
        # add expected json fields with test data
        data = {"documentFormat": "json", "document": "doc_example"}
        request.body = json.dumps(data)
        request.get_full_path = MagicMock()
        request.get_full_path.return_value = "https://full-path"

        MockCheckConnectors.return_value = True
        MockValidateRequest.return_value = "error"

        result = store_graph(request, "org_id", "doc_id")

        MockGraphChecker.assert_called_with(data["document"], data["documentFormat"], "https://full-path", MockValidityChecker)

        self.assertEqual("error", result)

    @patch("provenance.controller.get_token")
    @patch("provenance.controller.get_provenance")
    @patch('provenance.views.JsonResponse', autospec=True)
    def test_get_graph(self, MockJsonResponse, MockGetProv, MockGetToken):
        doc_test = controller.Document()
        doc_test.graph = "graph_test"
        MockGetProv.return_value = doc_test
        MockGetToken.return_value = "token_test"

        org_id = "org_id"
        doc_id = "doc_id"

        response = Response()
        MockJsonResponse.return_value = response

        result = get_graph("nothing", org_id, doc_id)

        MockGetProv.assert_called_with(org_id, doc_id)
        MockGetToken.assert_called_with(org_id, doc_id, doc_test)
        MockJsonResponse.assert_called_with({"document": doc_test.graph, "token": "token_test"})
        self.assertEqual(response, result)

    @patch("provenance.controller.get_provenance")
    @patch('provenance.views.JsonResponse', autospec=True)
    def test_get_graph_disabled_tp(self, MockJsonResponse, MockGetProv):
        # disable_tp
        config.disable_tp = True

        doc_test = controller.Document()
        doc_test.graph = "graph_test"
        MockGetProv.return_value = doc_test

        org_id = "org_id"
        doc_id = "doc_id"

        response = Response()
        MockJsonResponse.return_value = response

        result = get_graph("nothing", org_id, doc_id)

        MockGetProv.assert_called_with(org_id, doc_id)
        MockJsonResponse.assert_called_with({"document": doc_test.graph})
        self.assertEqual(response, result)
        config.disable_tp = False

    @patch("provenance.controller.get_provenance")
    @patch('provenance.views.JsonResponse', autospec=True)
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    def test_get_graph_does_not_exist(self, MockDoesNotExistException, MockJsonResponse, MockGetProv):
        doc_test = controller.Document()
        doc_test.graph = "graph_test"
        MockGetProv.return_value = doc_test
        MockGetProv.side_effect = DoesNotExist("nok")

        org_id = "org_id"
        doc_id = "doc_id"

        response = Response()
        MockJsonResponse.return_value = response

        result = get_graph("nothing", org_id, doc_id)

        MockGetProv.assert_called_with(org_id, doc_id)
        MockJsonResponse.assert_called_with({"error": f"Document with id [{doc_id}] does not "
                                                      f"exist under organization [{org_id}]."}, status=404)
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    def test__validate_request_fields(self, MockJsonResponse):
        request_json = {"test_field": "val"}
        mandatory_fields = ("test_field",)

        result = _validate_request_fields(request_json, mandatory_fields)

        self.assertEqual(None, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    def test__validate_request_fields_nok(self, MockJsonResponse):
        request_json = {}
        mandatory_fields = ("test_field",)
        response = Response()
        MockJsonResponse.return_value = response

        result = _validate_request_fields(request_json, mandatory_fields)

        MockJsonResponse.assert_called_with({"error": f"Mandatory field [test_field] not present in request!"},
                                            status=400)
        self.assertEqual(response, result)

    @patch("provenance.views.check_organization_is_registered")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.views.send_signature_verification_request")
    @patch("provenance.views._validate_duplicate_bundle")
    def test__validate_request(self, MockValudateDuplicateBundle, MockSendSignatureVerif, MockGetTpUrl,
                               MockCheckOrgRegistered):
        data = {"documentFormat": "json", "document": "doc_example", "signature": "sig_test",
                "createdOn": "now_test"}  # add fields because of expected fields when tp active
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        MockGetTpUrl.return_value = "https://tp_url_test"
        resp = Response()
        resp.status_code = 200
        MockSendSignatureVerif.return_value = resp  # for response to be "ok"
        MockCheckOrgRegistered.return_value = None
        validator.parse_graph.side_effect = None
        validator.check_ids_match.side_effect = None
        MockValudateDuplicateBundle.return_value = None
        validator.validate_graph.side_effect = None

        result = _validate_request(data, validator, doc_id, org_id, False, False)

        MockGetTpUrl.assert_called_with(org_id)
        MockSendSignatureVerif.assert_called_with(data, org_id, MockGetTpUrl.return_value)
        validator.parse_graph.assert_called_once()
        validator.check_ids_match.assert_called_with(doc_id)
        MockValudateDuplicateBundle.assert_called_with(validator, doc_id, org_id)
        MockCheckOrgRegistered.assert_called_with(org_id)
        validator.validate_graph.assert_called_once()
        self.assertEqual(None, result)

    @patch("provenance.views.check_organization_is_registered")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.views.send_signature_verification_request")
    @patch("provenance.views._validate_duplicate_bundle")
    @patch("provenance.views._validate_update_conditions")
    def test__validate_request_update(self, MockValidateUpdate, MockValudateDuplicateBundle, MockSendSignatureVerif,
                                      MockGetTpUrl,
                                      MockCheckOrgRegistered):
        data = {"documentFormat": "json", "document": "doc_example", "signature": "sig_test",
                "createdOn": "now_test"}  # add fields because of expected fields when tp active
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        MockGetTpUrl.return_value = "https://tp_url_test"
        resp = Response()
        resp.status_code = 200
        MockSendSignatureVerif.return_value = resp  # for response to be "ok"
        MockCheckOrgRegistered.return_value = None
        validator.parse_graph.side_effect = None
        MockValidateUpdate.return_value = None  # update - mock update conditions check
        MockValudateDuplicateBundle.return_value = None
        validator.validate_graph.side_effect = None

        result = _validate_request(data, validator, doc_id, org_id, True, False)

        MockGetTpUrl.assert_called_with(org_id)
        MockSendSignatureVerif.assert_called_with(data, org_id, MockGetTpUrl.return_value)
        validator.parse_graph.assert_called_once()
        MockValudateDuplicateBundle.assert_called_with(validator, doc_id, org_id)
        validator.validate_graph.assert_called_once()
        MockValidateUpdate.assert_called_with(validator, doc_id, org_id)
        MockCheckOrgRegistered.assert_called_with(org_id)
        self.assertEqual(None, result)

    @patch("provenance.views._validate_duplicate_bundle")
    @patch("provenance.views._validate_update_conditions")
    def test__validate_request_disable_tp(self, MockValidateUpdate, MockValudateDuplicateBundle):
        data = {"documentFormat": "json", "document": "doc_example"}
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        resp = Response()
        resp.status_code = 200
        validator.parse_graph.side_effect = None
        MockValidateUpdate.return_value = None  # update - mock update conditions check
        MockValudateDuplicateBundle.return_value = None
        validator.validate_graph.side_effect = None

        result = _validate_request(data, validator, doc_id, org_id, True, True)

        validator.parse_graph.assert_called_once()
        MockValudateDuplicateBundle.assert_called_with(validator, doc_id, org_id)
        validator.validate_graph.assert_called_once()
        MockValidateUpdate.assert_called_with(validator, doc_id, org_id)
        self.assertEqual(None, result)

    @patch("provenance.views.check_organization_is_registered")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.views.send_signature_verification_request")
    @patch("provenance.views._validate_duplicate_bundle")
    @patch("provenance.views._validate_update_conditions")
    @patch('provenance.views.JsonResponse', autospec=True)
    def test__validate_request_update_nok_graph(self, MockJsonResponse, MockValidateUpdate,
                                                MockValudateDuplicateBundle, MockSendSignatureVerif,
                                                MockGetTpUrl,
                                                MockCheckOrgRegistered):
        data = {"documentFormat": "json", "document": "doc_example", "signature": "sig_test",
                "createdOn": "now_test"}  # add fields because of expected fields when tp active
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        MockGetTpUrl.return_value = "https://tp_url_test"
        resp = Response()
        resp.status_code = 200
        MockSendSignatureVerif.return_value = resp  # for response to be "ok"
        MockCheckOrgRegistered.return_value = None
        validator.parse_graph.side_effect = None
        MockValidateUpdate.return_value = None  # update - mock update conditions check
        MockValudateDuplicateBundle.return_value = None
        validator.validate_graph.side_effect = DocumentError("nok")  # error when validating graph
        response = Response()
        MockJsonResponse.return_value = response

        result = _validate_request(data, validator, doc_id, org_id, True, False)

        MockGetTpUrl.assert_called_with(org_id)
        MockSendSignatureVerif.assert_called_with(data, org_id, MockGetTpUrl.return_value)
        validator.parse_graph.assert_called_once()
        MockValudateDuplicateBundle.assert_called_with(validator, doc_id, org_id)
        validator.validate_graph.assert_called_once()
        MockValidateUpdate.assert_called_with(validator, doc_id, org_id)
        MockCheckOrgRegistered.assert_called_with(org_id)
        MockJsonResponse.assert_called_with(
            {"error": "nok"},
            status=400)
        self.assertEqual(response, result)

    @patch("provenance.views.check_organization_is_registered")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.views.send_signature_verification_request")
    @patch("provenance.views._validate_duplicate_bundle")
    @patch("provenance.views._validate_update_conditions")
    def test__validate_request_update_duplicate_bundle(self, MockValidateUpdate,
                                                       MockValudateDuplicateBundle, MockSendSignatureVerif,
                                                       MockGetTpUrl,
                                                       MockCheckOrgRegistered):
        data = {"documentFormat": "json", "document": "doc_example", "signature": "sig_test",
                "createdOn": "now_test"}  # add fields because of expected fields when tp active
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        MockGetTpUrl.return_value = "https://tp_url_test"
        resp = Response()
        resp.status_code = 200
        MockSendSignatureVerif.return_value = resp  # for response to be "ok"
        MockCheckOrgRegistered.return_value = None
        validator.parse_graph.side_effect = None
        MockValidateUpdate.return_value = None  # update - mock update conditions check
        # set return value
        response = Response()
        response.status_code = 409
        MockValudateDuplicateBundle.return_value = response

        result = _validate_request(data, validator, doc_id, org_id, True, False)

        MockGetTpUrl.assert_called_with(org_id)
        MockSendSignatureVerif.assert_called_with(data, org_id, MockGetTpUrl.return_value)
        validator.parse_graph.assert_called_once()
        MockValudateDuplicateBundle.assert_called_with(validator, doc_id, org_id)
        MockValidateUpdate.assert_called_with(validator, doc_id, org_id)
        MockCheckOrgRegistered.assert_called_with(org_id)
        self.assertEqual(response, result)

    @patch("provenance.views.check_organization_is_registered")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.views.send_signature_verification_request")
    @patch("provenance.views._validate_update_conditions")
    def test__validate_request_update_conditions_unmet(self, MockValidateUpdate,
                                                       MockSendSignatureVerif,
                                                       MockGetTpUrl,
                                                       MockCheckOrgRegistered):
        data = {"documentFormat": "json", "document": "doc_example", "signature": "sig_test",
                "createdOn": "now_test"}  # add fields because of expected fields when tp active
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        MockGetTpUrl.return_value = "https://tp_url_test"
        resp = Response()
        resp.status_code = 200
        MockSendSignatureVerif.return_value = resp  # for response to be "ok"
        MockCheckOrgRegistered.return_value = None
        validator.parse_graph.side_effect = None

        response = Response()
        response.status_code = 409
        MockValidateUpdate.return_value = response  # returns ns error

        result = _validate_request(data, validator, doc_id, org_id, True, False)

        MockGetTpUrl.assert_called_with(org_id)
        MockSendSignatureVerif.assert_called_with(data, org_id, MockGetTpUrl.return_value)
        validator.parse_graph.assert_called_once()
        MockValidateUpdate.assert_called_with(validator, doc_id, org_id)
        MockCheckOrgRegistered.assert_called_with(org_id)
        self.assertEqual(response, result)

    @patch("provenance.views.check_organization_is_registered")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.views.send_signature_verification_request")
    @patch("provenance.views._validate_new_document_conditions")
    def test__validate_request_conditions_unmet(self, MockValidateUpdate,
                                                MockSendSignatureVerif,
                                                MockGetTpUrl,
                                                MockCheckOrgRegistered):
        data = {"documentFormat": "json", "document": "doc_example", "signature": "sig_test",
                "createdOn": "now_test"}  # add fields because of expected fields when tp active
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        MockGetTpUrl.return_value = "https://tp_url_test"
        resp = Response()
        resp.status_code = 200
        MockSendSignatureVerif.return_value = resp  # for response to be "ok"
        MockCheckOrgRegistered.return_value = None
        validator.parse_graph.side_effect = None

        response = Response()
        response.status_code = 409
        MockValidateUpdate.return_value = response  # returns ns error

        result = _validate_request(data, validator, doc_id, org_id, False, False)

        MockGetTpUrl.assert_called_with(org_id)
        MockSendSignatureVerif.assert_called_with(data, org_id, MockGetTpUrl.return_value)
        validator.parse_graph.assert_called_once()
        MockValidateUpdate.assert_called_with(validator, doc_id)
        MockCheckOrgRegistered.assert_called_with(org_id)
        self.assertEqual(response, result)

    @patch("provenance.views.check_organization_is_registered")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.views.send_signature_verification_request")
    @patch('provenance.views.JsonResponse', autospec=True)
    def test__validate_request_graph_parse_error(self,
                                                 MockJsonResponse,
                                                 MockSendSignatureVerif,
                                                 MockGetTpUrl,
                                                 MockCheckOrgRegistered):
        data = {"documentFormat": "json", "document": "doc_example", "signature": "sig_test",
                "createdOn": "now_test"}  # add fields because of expected fields when tp active
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        MockGetTpUrl.return_value = "https://tp_url_test"
        resp = Response()
        resp.status_code = 200
        MockSendSignatureVerif.return_value = resp  # for response to be "ok"
        MockCheckOrgRegistered.return_value = None
        validator.parse_graph.side_effect = DocumentError("nok")  # added error
        response = Response()
        MockJsonResponse.return_value = response

        result = _validate_request(data, validator, doc_id, org_id, False, False)

        MockGetTpUrl.assert_called_with(org_id)
        MockSendSignatureVerif.assert_called_with(data, org_id, MockGetTpUrl.return_value)
        validator.parse_graph.assert_called_once()
        MockCheckOrgRegistered.assert_called_with(org_id)
        self.assertEqual(response, result)
        MockJsonResponse.assert_called_with({"error": "nok"},
                                            status=400)

    @patch("provenance.views.check_organization_is_registered")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.views.send_signature_verification_request")
    @patch('provenance.views.JsonResponse', autospec=True)
    def test__validate_request_reques_fields_nok(self,
                                                 MockJsonResponse,
                                                 MockSendSignatureVerif,
                                                 MockGetTpUrl,
                                                 MockCheckOrgRegistered):
        data = {"documentFormat": "json"}  # missing field document
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        MockGetTpUrl.return_value = "https://tp_url_test"
        resp = Response()
        resp.status_code = 200
        MockSendSignatureVerif.return_value = resp  # for response to be "ok"
        MockCheckOrgRegistered.return_value = None
        response = Response()
        MockJsonResponse.return_value = response

        result = _validate_request(data, validator, doc_id, org_id, False, False)

        MockGetTpUrl.assert_called_with(org_id)
        MockSendSignatureVerif.assert_called_with(data, org_id, MockGetTpUrl.return_value)
        MockCheckOrgRegistered.assert_called_with(org_id)
        self.assertEqual(response, result)
        MockJsonResponse.assert_called_with({"error": f"Mandatory field [document] not present in request!"},
                                            status=400)

    @patch("provenance.views.check_organization_is_registered")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.views.send_signature_verification_request")
    @patch('provenance.views.JsonResponse', autospec=True)
    def test__validate_request_reques_signature_nok(self,
                                                    MockJsonResponse,
                                                    MockSendSignatureVerif,
                                                    MockGetTpUrl,
                                                    MockCheckOrgRegistered):
        data = {"documentFormat": "json"}  # missing field document
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        MockGetTpUrl.return_value = "https://tp_url_test"
        resp = Response()
        resp.status_code = 409
        MockSendSignatureVerif.return_value = resp  # for response to be not ok
        MockCheckOrgRegistered.return_value = None
        response = Response()
        MockJsonResponse.return_value = response

        result = _validate_request(data, validator, doc_id, org_id, False, False)

        MockGetTpUrl.assert_called_with(org_id)
        MockSendSignatureVerif.assert_called_with(data, org_id, MockGetTpUrl.return_value)
        MockCheckOrgRegistered.assert_called_with(org_id)
        self.assertEqual(response, result)
        MockJsonResponse.assert_called_with({"error": "Unverifiable signature."
                                                      " Make sure to register your certificate with trusted party first."},
                                            status=401)

    @patch("provenance.views.check_organization_is_registered")
    @patch('provenance.views.JsonResponse', autospec=True)
    def test__validate_request_org_not_registered(self,
                                                  MockJsonResponse,
                                                  MockCheckOrgRegistered):
        data = {"documentFormat": "json"}  # missing field document
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        resp = Response()
        resp.status_code = 409
        MockCheckOrgRegistered.side_effect = OrganizationNotRegistered("org not registered")
        response = Response()
        MockJsonResponse.return_value = response

        result = _validate_request(data, validator, doc_id, org_id, False, False)

        MockCheckOrgRegistered.assert_called_with(org_id)
        self.assertEqual(response, result)
        MockJsonResponse.assert_called_with({"error": "org not registered"},
                                            status=404)

    @patch("provenance.views.check_graph_id_belongs_to_meta")
    @patch("provenance.views.graph_exists")
    def test__validate_update_conditions_ok(self, MockGraphExists, MockCheckGraphIdMeta):
        MockCheckGraphIdMeta.side_effect = None  # does not throw error
        MockGraphExists.return_value = True

        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        validator.get_meta_provenance_id.return_value = "meta_id"

        result = _validate_update_conditions(validator, doc_id, org_id)

        MockCheckGraphIdMeta.assert_called_with("meta_id", doc_id, org_id)
        MockGraphExists.assert_called_with(org_id, doc_id)
        self.assertEqual(None, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch("provenance.views.check_graph_id_belongs_to_meta")
    @patch("provenance.views.graph_exists")
    def test__validate_update_conditions_graph_does_not_exist(self, MockGraphExists, MockCheckGraphIdMeta,
                                                              MockJsonResponse):
        response = Response()
        MockJsonResponse.return_value = response
        MockCheckGraphIdMeta.side_effect = None  # does not throw error
        MockGraphExists.return_value = False

        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        validator.get_meta_provenance_id.return_value = "meta_id"

        result = _validate_update_conditions(validator, doc_id, org_id)

        MockCheckGraphIdMeta.assert_called_with("meta_id", doc_id, org_id)
        MockGraphExists.assert_called_with(org_id, doc_id)
        MockJsonResponse.assert_called_with({"error": f"Document with id [{doc_id}] does not exist."
                                                      "Please check whether the ID you have given is correct."},
                                            status=404)
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch("provenance.views.check_graph_id_belongs_to_meta")
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    def test__validate_update_conditions_graph_not_under_org(self, MockDoesNotExist, MockCheckGraphIdMeta,
                                                             MockJsonResponse):
        response = Response()
        MockJsonResponse.return_value = response
        MockCheckGraphIdMeta.side_effect = DoesNotExist("test")  # throws error

        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        validator.get_meta_provenance_id.return_value = "meta_id"

        result = _validate_update_conditions(validator, doc_id, org_id)

        MockCheckGraphIdMeta.assert_called_with("meta_id", doc_id, org_id)
        MockJsonResponse.assert_called_with({"error": f"Document with id [{doc_id}] does not "
                                                      f"exist under organization [{org_id}]."},
                                            status=404)
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch("provenance.views.check_graph_id_belongs_to_meta")
    def test__validate_update_conditions_document_error(self, MockCheckGraphIdMeta,
                                                        MockJsonResponse):
        response = Response()
        MockJsonResponse.return_value = response
        MockCheckGraphIdMeta.side_effect = DocumentError("test")  # throws error

        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        validator.get_meta_provenance_id.return_value = "meta_id"

        result = _validate_update_conditions(validator, doc_id, org_id)

        MockCheckGraphIdMeta.assert_called_with("meta_id", doc_id, org_id)
        MockJsonResponse.assert_called_with({"error": "test"},
                                            status=400)
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    def test__validate_new_document_conditions_nok(self, MockJsonResponse):
        response = Response()
        MockJsonResponse.return_value = response

        validator = MagicMock()
        doc_id = "doc_id"
        validator.check_ids_match.side_effect = DocumentError("test")

        result = _validate_new_document_conditions(validator, doc_id)

        validator.check_ids_match.assert_called_with(doc_id)
        MockJsonResponse.assert_called_with({"error": "test"},
                                            status=400)
        self.assertEqual(response, result)

    def test__validate_new_document_conditions_ok(self):
        validator = MagicMock()
        doc_id = "doc_id"
        validator.check_ids_match.side_effect = None

        result = _validate_new_document_conditions(validator, doc_id)

        validator.check_ids_match.assert_called_with(doc_id)
        self.assertEqual(None, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    def test__parse_input_graph_nok(self, MockJsonResponse):
        response = Response()
        MockJsonResponse.return_value = response

        validator = MagicMock()
        validator.parse_graph.side_effect = DocumentError("test")

        result = _parse_input_graph(validator)

        validator.parse_graph.assert_called_once()
        MockJsonResponse.assert_called_with({"error": "test"},
                                            status=400)
        self.assertEqual(response, result)

    def test__parse_input_graph_ok(self):
        validator = MagicMock()
        validator.parse_graph.side_effect = None

        result = _parse_input_graph(validator)

        validator.parse_graph.assert_called_once()
        self.assertEqual(None, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch("provenance.views.graph_exists")
    def test__validate_duplicate_bundle_nok(self, MockGraphExists, MockJsonResponse):
        response = Response()
        response.status_code = 200
        MockJsonResponse.return_value = response
        MockGraphExists.return_value = True

        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        validator.get_bundle_id.return_value = 908

        result = _validate_duplicate_bundle(validator, doc_id, org_id)

        MockGraphExists.assert_called_with(org_id, 908)
        MockJsonResponse.assert_called_with({"error": f"Document with id [908] already "
                                                      f"exists under organization [{org_id}]."},
                                            status=409)
        self.assertEqual(response, result)

    @patch("provenance.views.graph_exists")
    def test__validate_duplicate_bundle_ok(self, MockGraphExists):
        validator = MagicMock()
        doc_id = "doc_id"
        org_id = "org_id"
        validator.get_bundle_id.return_value = 908
        MockGraphExists.return_value = False

        result = _validate_duplicate_bundle(validator, doc_id, org_id)

        MockGraphExists.assert_called_with(org_id, 908)
        self.assertEqual(None, result)

    @patch('provenance.views.HttpResponse', autospec=True)
    @patch("provenance.controller.meta_bundle_exists")
    def test_graph_meta_method_head_ok(self, MockBundleExists, MockHttpResponse):
        request = requests.Request()
        request.method = "HEAD"
        meta_id = "meta_id"
        response = Response()
        response.status_code = 200
        MockHttpResponse.return_value = response
        MockBundleExists.return_value = True

        from provenance.views import graph_meta
        result = graph_meta(request, meta_id)

        MockHttpResponse.assert_called_with(200)
        MockBundleExists.assert_called_with(meta_id)
        self.assertEqual(response, result)

    @patch('provenance.views.HttpResponseNotFound', autospec=True)
    @patch("provenance.controller.meta_bundle_exists")
    def test_graph_meta_method_head_nok(self, MockBundleExists, MockHttpResponse):
        request = requests.Request()
        request.method = "HEAD"
        meta_id = "meta_id"
        response = Response()
        response.status_code = 400
        MockHttpResponse.return_value = response
        MockBundleExists.return_value = False

        from provenance.views import graph_meta
        result = graph_meta(request, meta_id)

        MockHttpResponse.assert_called_once()
        MockBundleExists.assert_called_with(meta_id)
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    def test_graph_meta_method_get_wrong_format(self, MockJsonResponse):
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        request.GET = {"format": "not_json", "organizationId": org_id}
        meta_id = "meta_id"
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response

        from provenance.views import graph_meta
        result = graph_meta(request, meta_id)

        MockJsonResponse.assert_called_with({"error": f"Requested format [not_json] is not supported!"},
                                            status=400)
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch("provenance.controller.get_b64_encoded_meta_provenance")
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    def test_graph_meta_method_get_meta_prov_does_not_exist(self, MockDoesNotExistException, MockGetProv,
                                                            MockJsonResponse):
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        request.GET = {"organizationId": org_id}  # default format used - rdf
        meta_id = "meta_id"
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response
        MockGetProv.side_effect = DoesNotExist("error")

        from provenance.views import graph_meta
        result = graph_meta(request, meta_id)

        MockJsonResponse.assert_called_with({"error": f"The meta-provenance with id [{meta_id}] does not exist."},
                                            status=404)
        MockGetProv.assert_called_with(meta_id, "rdf")
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch("provenance.controller.get_b64_encoded_meta_provenance")
    def test_graph_meta_method_get_meta_disabled_tp(self, MockGetProv,
                                                    MockJsonResponse):
        config.disable_tp = True
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        request.GET = {"organizationId": org_id}  # default format used
        meta_id = "meta_id"
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response
        MockGetProv.return_value = "graph_test"

        from provenance.views import graph_meta
        result = graph_meta(request, meta_id)
        config.disable_tp = False

        MockGetProv.assert_called_with(meta_id, "rdf")
        MockJsonResponse.assert_called_with({"graph": "graph_test"})
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch("provenance.controller.get_b64_encoded_meta_provenance")
    @patch("provenance.controller.get_tp_url_by_organization")
    @patch("provenance.controller.send_token_request_to_tp")
    def test_graph_meta_method_get_meta_tp_allowed_org_defined(self, MockSendTokenRequest, MockGetTpUrl, MockGetProv,
                                                               MockJsonResponse):
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        request.GET = {"organizationId": org_id}  # default format used - rdf
        meta_id = "meta_id"
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response
        MockGetProv.return_value = "graph_test"
        MockGetTpUrl.return_value = "https://tp_url"
        MockSendTokenRequest.return_value = "token_test"

        from provenance.views import graph_meta
        result = graph_meta(request, meta_id)

        MockGetProv.assert_called_with(meta_id, "rdf")
        MockJsonResponse.assert_called_with({"graph": "graph_test", "token": "token_test"})
        call_args_get_tp_url_payload, call_args_tp_url = MockSendTokenRequest.call_args.args
        self.assertEqual(MockGetTpUrl.return_value, call_args_tp_url)
        self.assertEqual("graph_test", call_args_get_tp_url_payload["document"])
        self.assertIsNotNone(call_args_get_tp_url_payload["createdOn"])
        self.assertEqual("meta", call_args_get_tp_url_payload["type"])
        self.assertEqual(config.id, call_args_get_tp_url_payload["organizationId"])
        self.assertEqual("rdf", call_args_get_tp_url_payload["documentFormat"])
        self.assertEqual(meta_id, call_args_get_tp_url_payload["graphId"])

        MockGetTpUrl.assert_called_with(org_id)
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch("provenance.controller.get_b64_encoded_meta_provenance")
    @patch("provenance.controller.send_token_request_to_tp")
    def test_graph_meta_method_get_meta_tp_allowed_org_none(self, MockSendTokenRequest, MockGetProv,
                                                            MockJsonResponse):
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        request.GET = {}  # default format used - rdf, no org id
        meta_id = "meta_id"
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response
        MockGetProv.return_value = "graph_test"
        MockSendTokenRequest.return_value = "token_test"

        from provenance.views import graph_meta
        result = graph_meta(request, meta_id)

        MockGetProv.assert_called_with(meta_id, "rdf")
        MockJsonResponse.assert_called_with({"graph": "graph_test", "token": "token_test"})
        call_args_get_tp_url_payload, call_args_tp_url = MockSendTokenRequest.call_args.args
        self.assertEqual(None, call_args_tp_url)
        self.assertEqual("graph_test", call_args_get_tp_url_payload["document"])
        self.assertIsNotNone(call_args_get_tp_url_payload["createdOn"])
        self.assertEqual("meta", call_args_get_tp_url_payload["type"])
        self.assertEqual(config.id, call_args_get_tp_url_payload["organizationId"])
        self.assertEqual("rdf", call_args_get_tp_url_payload["documentFormat"])
        self.assertEqual(meta_id, call_args_get_tp_url_payload["graphId"])
        self.assertEqual(response, result)

    @patch('provenance.views.get_subgraph')
    def test_graph_domain_specific(self, MockGetSubgraph):
        MockGetSubgraph.return_value = "test_val"
        request = Request()
        request.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"

        from provenance.views import graph_domain_specific
        result = graph_domain_specific(request, org_id, doc_id)

        self.assertEqual("test_val", result)
        MockGetSubgraph.assert_called_with(request, org_id, doc_id, True)

    @patch('provenance.views.get_subgraph')
    def test_graph_backbone(self, MockGetSubgraph):
        MockGetSubgraph.return_value = "test_val"
        request = Request()
        request.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"

        from provenance.views import graph_backbone
        result = graph_backbone(request, org_id, doc_id)

        self.assertEqual("test_val", result)
        MockGetSubgraph.assert_called_with(request, org_id, doc_id, False)

    @patch('provenance.views.JsonResponse', autospec=True)
    def test_get_subgraph_wrong_format(self, MockJsonResponse):
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        request.GET = {"format": "not_json", "organizationId": org_id}
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response

        result = get_subgraph(request, org_id, doc_id, False)

        MockJsonResponse.assert_called_with({"error": f"Requested format [not_json] is not supported!"},
                                            status=400)
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch('provenance.controller.query_db_for_subgraph')
    def test_get_subgraph_subgraph_exists_in_db(self, MockQueryForSubgraph, MockJsonResponse):
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        request.GET = {"format": "json", "organizationId": org_id}
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response
        graph = "graph_test"
        token = "token_test"
        is_domain_specific = False
        MockQueryForSubgraph.return_value = graph, token

        result = get_subgraph(request, org_id, doc_id, is_domain_specific)

        MockJsonResponse.assert_called_with({"document": graph, "token": token})
        MockQueryForSubgraph.assert_called_with(org_id, doc_id, "json", is_domain_specific)
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch('provenance.controller.query_db_for_subgraph')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.controller.get_b64_encoded_subgraph')
    def test_get_subgraph_subgraph_does_not_exists_at_all(self, MockQueryForSubgraph_b64, MockDoesNotExistException,
                                                          MockQueryForSubgraph, MockJsonResponse):
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        request.GET = {"format": "json", "organizationId": org_id}
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response
        is_domain_specific = False
        MockQueryForSubgraph.side_effect = DoesNotExist("error")
        MockQueryForSubgraph_b64.side_effect = DoesNotExist("error")

        result = get_subgraph(request, org_id, doc_id, is_domain_specific)

        MockJsonResponse.assert_called_with({"error": f"Document with id [{doc_id}] does not "
                                                      f"exist under organization [{org_id}]."}, status=404)
        MockQueryForSubgraph.assert_called_with(org_id, doc_id, "json", is_domain_specific)
        MockQueryForSubgraph_b64.assert_called_with(org_id, doc_id, is_domain_specific, "json")
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch('provenance.controller.query_db_for_subgraph')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.controller.get_b64_encoded_subgraph')
    @patch('provenance.controller.store_subgraph_into_db')
    def test_get_subgraph_subgraph_created_now_tp_disabled(self, MockStoreSubgraph, MockQueryForSubgraph_b64,
                                                           MockDoesNotExistException,
                                                           MockQueryForSubgraph, MockJsonResponse):
        # disable_tp
        config.disable_tp = True
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        graph = "graph_test"
        token = "token_test"
        request.GET = {"format": "json", "organizationId": org_id}
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response
        is_domain_specific = False
        MockQueryForSubgraph.side_effect = DoesNotExist("error")
        MockQueryForSubgraph_b64.return_value = graph

        result = get_subgraph(request, org_id, doc_id, is_domain_specific)

        MockJsonResponse.assert_called_with({"document": graph})
        MockQueryForSubgraph.assert_called_with(org_id, doc_id, "json", is_domain_specific)
        MockQueryForSubgraph_b64.assert_called_with(org_id, doc_id, is_domain_specific, "json")
        MockStoreSubgraph.assert_called_with(f"{org_id}_{doc_id}_backbone", "json", graph, None)
        self.assertEqual(response, result)
        config.disable_tp = False

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch('provenance.controller.query_db_for_subgraph')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.controller.get_b64_encoded_subgraph')
    @patch('provenance.controller.store_subgraph_into_db')
    @patch('provenance.controller.send_token_request_to_tp')
    @patch('provenance.controller.get_tp_url_by_organization')
    def test_get_subgraph_subgraph_created_now_tp_enabled(self, MockGetTpUrl, MockSendTokenRequest, MockStoreSubgraph,
                                                          MockQueryForSubgraph_b64,
                                                          MockDoesNotExistException,
                                                          MockQueryForSubgraph, MockJsonResponse):
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        graph = "graph_test"
        token = "token_test"
        request.GET = {"format": "json", "organizationId": org_id}
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response
        is_domain_specific = False
        MockQueryForSubgraph.side_effect = DoesNotExist("error")
        MockQueryForSubgraph_b64.return_value = graph
        MockGetTpUrl.return_value = "random_url"
        MockSendTokenRequest.return_value = token

        result = get_subgraph(request, org_id, doc_id, is_domain_specific)

        MockJsonResponse.assert_called_with({"document": graph, "token": token})
        MockQueryForSubgraph.assert_called_with(org_id, doc_id, "json", is_domain_specific)
        MockQueryForSubgraph_b64.assert_called_with(org_id, doc_id, is_domain_specific, "json")
        MockStoreSubgraph.assert_called_with(f"{org_id}_{doc_id}_backbone", "json", graph, token)
        self.assertEqual(response, result)

        call_args_get_tp_url_payload, call_args_tp_url = MockSendTokenRequest.call_args.args
        self.assertEqual(MockGetTpUrl.return_value, call_args_tp_url)
        self.assertEqual("graph_test", call_args_get_tp_url_payload["document"])
        self.assertIsNotNone(call_args_get_tp_url_payload["createdOn"])
        self.assertEqual("backbone", call_args_get_tp_url_payload["type"])
        self.assertEqual(org_id, call_args_get_tp_url_payload["organizationId"])
        self.assertEqual("json", call_args_get_tp_url_payload["documentFormat"])
        self.assertEqual(doc_id, call_args_get_tp_url_payload["graphId"])
        self.assertEqual(response, result)

    @patch('provenance.views.JsonResponse', autospec=True)
    @patch('provenance.controller.query_db_for_subgraph')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.controller.get_b64_encoded_subgraph')
    @patch('provenance.controller.store_subgraph_into_db')
    def test_get_subgraph_subgraph_created_now_tp_disabled_domain(self, MockStoreSubgraph, MockQueryForSubgraph_b64,
                                                                  MockDoesNotExistException,
                                                                  MockQueryForSubgraph, MockJsonResponse):
        # disable_tp
        config.disable_tp = True
        request = requests.Request()
        request.method = "GET"
        org_id = "org_id"
        doc_id = "doc_id"
        graph = "graph_test"
        token = "token_test"
        request.GET = {"format": "json", "organizationId": org_id}
        response = Response()
        response.status_code = 400
        MockJsonResponse.return_value = response
        is_domain_specific = True
        MockQueryForSubgraph.side_effect = DoesNotExist("error")
        MockQueryForSubgraph_b64.return_value = graph

        result = get_subgraph(request, org_id, doc_id, is_domain_specific)

        MockJsonResponse.assert_called_with({"document": graph})
        MockQueryForSubgraph.assert_called_with(org_id, doc_id, "json", is_domain_specific)
        MockQueryForSubgraph_b64.assert_called_with(org_id, doc_id, is_domain_specific, "json")
        MockStoreSubgraph.assert_called_with(f"{org_id}_{doc_id}_domain", "json", graph, None)
        self.assertEqual(response, result)
        config.disable_tp = False
