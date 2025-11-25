from datetime import datetime
from unittest import TestCase

from prov.identifier import QualifiedName, Identifier, Namespace
from prov.model import ProvBundle, ProvDocument, ProvRecord
from requests import Response

from helpers import create_cpm_provenance_basic, create_cpm_provenance_with_backward_connector, \
    create_main_act_attributes, prepare_attributes_for_doc_w_backward_connectors
from provenance.controller import _verify_at_local_storage, _referenced_bundles_exist, _check_referenced_bundles, \
    _verify_at_remote_storage
from provenance.neomodel2prov import NAMESPACES
from provenance.controller import *

from unittest.mock import MagicMock, call
from unittest.mock import patch

from provenance.views import get_dummy_token


class MyTestCase(TestCase):

    @patch('provenance.models.Document.nodes')
    def test_get_provenance_exists_returns_document(self, MockDocumentNodes):
        neo_document = Document()
        neo_document.identifier = "neo_doc"
        MockDocumentNodes.get = MagicMock()
        MockDocumentNodes.get.return_value = neo_document

        doc_returned = get_provenance("org_id", "graph_id")

        self.assertEqual(neo_document, doc_returned)
        MockDocumentNodes.get.assert_called_with(identifier="org_id_graph_id")

    @patch('provenance.models.Document.nodes')
    def test_query_db_for_subgraph_returns_subgraph_and_token(self, MockDocumentNodes):
        neo_document = Document()
        neo_document.identifier = "neo_doc"
        neo_document.format = "json"
        neo_document.graph = "json_graph..."
        neo_document.belongs_to.all = MagicMock()
        token = Token(originator_id="originator_id", authority_id="authority_id", token_timestamp=9,
                      message_timestamp=10, hash="hash", additional_data=None)
        neo_document.belongs_to.all.return_value = [token]
        MockDocumentNodes.get = MagicMock()
        MockDocumentNodes.get.return_value = neo_document

        graph_returned, token_returned = query_db_for_subgraph("org_id", "graph_id", "json", False)

        self.assertEqual(neo_document.graph, graph_returned)
        self.assertEqual(token.originator_id, token_returned["data"]["originatorId"])
        self.assertEqual(token.authority_id, token_returned["data"]["authorityId"])
        self.assertEqual(token.token_timestamp, token_returned["data"]["tokenTimestamp"])
        self.assertEqual(token.hash, token_returned["data"]["documentDigest"])
        self.assertEqual(token.message_timestamp, token_returned["data"]["documentCreationTimestamp"])
        self.assertEqual(token.additional_data, token_returned["data"]["additionalData"])
        MockDocumentNodes.get.assert_called_with(identifier=f"org_id_graph_id_backbone", format="json")

    @patch('provenance.models.Document.nodes')
    def test_query_db_for_subgraph_domain_specific_returns_subgraph_and_token(self, MockDocumentNodes):
        neo_document = Document()
        neo_document.identifier = "neo_doc"
        neo_document.format = "json"
        neo_document.graph = "json_graph..."
        neo_document.belongs_to.all = MagicMock()
        token = Token(originator_id="originator_id", authority_id="authority_id", token_timestamp=9,
                      message_timestamp=10, hash="hash", additional_data=None)
        neo_document.belongs_to.all.return_value = [token]
        MockDocumentNodes.get = MagicMock()
        MockDocumentNodes.get.return_value = neo_document

        graph_returned, token_returned = query_db_for_subgraph("org_id", "graph_id", "json", True)

        self.assertEqual(neo_document.graph, graph_returned)
        self.assertEqual(token.originator_id, token_returned["data"]["originatorId"])
        self.assertEqual(token.authority_id, token_returned["data"]["authorityId"])
        self.assertEqual(token.token_timestamp, token_returned["data"]["tokenTimestamp"])
        self.assertEqual(token.hash, token_returned["data"]["documentDigest"])
        self.assertEqual(token.message_timestamp, token_returned["data"]["documentCreationTimestamp"])
        self.assertEqual(token.additional_data, token_returned["data"]["additionalData"])
        MockDocumentNodes.get.assert_called_with(identifier=f"org_id_graph_id_domain", format="json")

    @patch('provenance.models.Document.nodes')
    @patch("distributed_prov_system.settings.config.disable_tp")
    def test_query_db_for_subgraph_no_token_returns_subgraph(self, ConfigDisable, MockDocumentNodes):
        neo_document = Document()
        neo_document.identifier = "neo_doc"
        neo_document.format = "json"
        neo_document.graph = "json_graph..."
        neo_document.belongs_to.all = MagicMock()
        MockDocumentNodes.get = MagicMock()
        MockDocumentNodes.get.return_value = neo_document
        ConfigDisable.return_value = True

        graph_returned, token_returned = query_db_for_subgraph("org_id", "graph_id", "json", False)

        self.assertEqual(neo_document.graph, graph_returned)
        self.assertEqual(None, token_returned)
        MockDocumentNodes.get.assert_called_with(identifier=f"org_id_graph_id_backbone", format="json")

    @patch('provenance.models.Document.nodes')
    @patch('provenance.models.TrustedParty.nodes')
    def test_get_token_to_store_into_db_returns_token_document_trusted_party(self, MockTrustedPartyNodes, MockDocumentNodes):
        neo_document = Document()
        neo_document.identifier = "neo_doc"
        neo_document.format = "json"
        neo_document.graph = "json_graph..."
        MockDocumentNodes.get = MagicMock()
        MockDocumentNodes.get.return_value = neo_document

        trusted_party = TrustedParty(identifier="trusted_party")
        MockTrustedPartyNodes.get = MagicMock()
        MockTrustedPartyNodes.get.return_value = trusted_party

        token = get_dummy_token()
        token["data"]["additionalData"] = "additional data"

        token_returned, doc_returned, tp_returned = get_token_to_store_into_db(token, "document_id")

        self.assertEqual(neo_document, doc_returned)
        self.assertEqual(trusted_party, tp_returned)
        self.assertEqual(token["data"]["originatorId"], token_returned.originator_id)
        self.assertEqual(token["data"]["authorityId"], token_returned.authority_id)
        self.assertEqual(token["data"]["tokenTimestamp"], token_returned.token_timestamp)
        self.assertEqual(token["data"]["documentDigest"], token_returned.hash)
        self.assertEqual(token["data"]["documentCreationTimestamp"], token_returned.message_timestamp)
        self.assertEqual(token["data"]["additionalData"], token_returned.additional_data)

    @patch('provenance.controller.Document', autospec=True)
    @patch('provenance.controller.db')
    def test_store_subgraph_into_db_no_token_creates_doc(self, MockDb, MockDocument):
        MockDocument.save = MagicMock()
        MockDocument.save.return_value = None
        doc_id = "doc id"
        doc_format = "json"
        doc_graph = "doc graph"
        MockDb.transaction.side_effect = None

        store_subgraph_into_db(doc_id, doc_format, doc_graph, None)

        # test set values of created document
        self.assertEqual(doc_id, MockDocument.return_value.identifier)
        self.assertEqual(doc_format, MockDocument.return_value.format)
        self.assertEqual(doc_graph, MockDocument.return_value.graph)

    def test_store_token_into_db_saves_and_creates_relations(self):
        token = Token(identifier="tokenid")
        neo_document = Document(identifier="docid")
        trusted_party = TrustedParty(identifier="tpid")
        token.save = MagicMock()
        token.belongs_to.connect = MagicMock()
        token.was_issued_by.connect = MagicMock()

        token_returned = store_token_into_db(token, neo_document, trusted_party)

        self.assertEqual(token, token_returned)
        token.save.assert_called_once()
        token.belongs_to.connect.assert_called_with(neo_document)
        token.was_issued_by.connect.assert_called_with(trusted_party)

    @patch('provenance.controller.Document.nodes', autospec=True)
    @patch('provenance.controller.retrieve_subgraph')
    def test_get_b64_encoded_subgraph_subgraph_exists_returns_subgraph(self, MockRetrieve_subgraph, MockDocumentNodes):
        document = Document(identifier="doc_id")
        document.format = "json"
        document.graph = b64encode("graph...".encode("utf-8"))

        MockDocumentNodes.get = MagicMock()
        MockDocumentNodes.get.return_value = document

        new_doc = provm.ProvDocument()
        new_doc.add_bundle(ProvBundle(identifier=QualifiedName(namespace=NAMESPACES["prov"], localpart="bundle1")))
        MockRetrieve_subgraph.return_value = new_doc

        result = get_b64_encoded_subgraph("org", "graph", True, "rdf")

        self.assertEqual(b64encode(new_doc.serialize(format="rdf").encode("utf-8")).decode("utf-8"), result)
        MockRetrieve_subgraph.assert_called_with(b64decode(document.graph), document.format, True)
        MockDocumentNodes.get.assert_called_with(identifier="org_graph")

    @patch('provenance.controller.is_org_registered')
    @patch('provenance.controller.db')
    def test_get_token_token_exists_returns_token(self, MockDb, MockIs_org_registered):
        document = Document(identifier="doc_id")
        MockIs_org_registered.return_value = True
        MockDb.cypher_query = MagicMock()
        token = Token(originator_id="originator_id", authority_id="authority_id", token_timestamp=9,
                      message_timestamp=10, hash="hash", additional_data=None)
        MockDb.cypher_query.return_value = [[token]], True

        token2 = get_token("org_id", "graph_id", document)

        self.assertEqual("originator_id", token2["data"]["originatorId"])
        self.assertEqual("authority_id", token2["data"]["authorityId"])
        self.assertEqual(9, token2["data"]["tokenTimestamp"])
        self.assertEqual(10, token2["data"]["documentCreationTimestamp"])
        self.assertEqual("hash", token2["data"]["documentDigest"])
        self.assertEqual(None, token2["data"]["additionalData"])
        self.assertEqual(None, token2["signature"])
        MockDb.cypher_query.assert_called_with("""
                MATCH (org:Organization) WHERE org.identifier=$organization_id
                MATCH (org)-[:trusts]->(tp:TrustedParty)<-[:was_issued_by]-(token:Token)-[:belongs_to]->(doc:Document)
                WHERE doc.identifier=$doc_id
                RETURN token
                """, {"organization_id": "org_id", "doc_id": "org_id_graph_id"}, resolve_objects=True)
        MockIs_org_registered.assert_called_with("org_id")

    @patch('provenance.controller.is_org_registered')
    @patch('provenance.controller.db')
    def test_get_token_org_not_registered_returns_token(self, MockDb, MockIs_org_registered):
        document = Document(identifier="doc_id")
        MockIs_org_registered.return_value = False
        MockDb.cypher_query = MagicMock()
        token = Token(originator_id="originator_id", authority_id="authority_id", token_timestamp=9,
                      message_timestamp=10, hash="hash", additional_data=None)
        MockDb.cypher_query.return_value = [[token]], True

        token2 = get_token("org_id", "graph_id", document)

        self.assertEqual("originator_id", token2["data"]["originatorId"])
        self.assertEqual("authority_id", token2["data"]["authorityId"])
        self.assertEqual(9, token2["data"]["tokenTimestamp"])
        self.assertEqual(10, token2["data"]["documentCreationTimestamp"])
        self.assertEqual("hash", token2["data"]["documentDigest"])
        self.assertEqual(None, token2["data"]["additionalData"])
        self.assertEqual(None, token2["signature"])
        MockDb.cypher_query.assert_called_with("""
                MATCH (tp:DefaultTrustedParty)<-[:was_issued_by]-(token:Token)-[:belongs_to]->(doc:Document)
                WHERE doc.identifier=$doc_id
                RETURN token
                """, {"organization_id": "org_id", "doc_id": "org_id_graph_id"}, resolve_objects=True)
        MockIs_org_registered.assert_called_with("org_id")

    @patch('provenance.controller.is_org_registered')
    @patch('provenance.controller.db')
    @patch('provenance.controller.get_tp_url_by_organization')
    @patch('provenance.controller.send_token_request_to_tp')
    @patch('provenance.controller.get_token_to_store_into_db')
    @patch('provenance.controller.store_token_into_db')
    def test_get_token_token_not_in_db_creates_and_returns_token(self, MockStoreToken, MockGetTokenToStore, MockTokenRequest, MockGetOrg, MockDb,
                                       MockIs_org_registered):
        document = Document(identifier="doc_id")
        document.graph = b64encode("graph...".encode("utf-8"))
        MockIs_org_registered.return_value = True
        MockDb.cypher_query = MagicMock()
        token = Token(originator_id="originator_id", authority_id="authority_id", token_timestamp=9,
                      message_timestamp=10, hash="hash", additional_data=None)
        token1 = Token(originator_id="originator_id1", authority_id="authority_id1", token_timestamp=9,
                       message_timestamp=10, hash="hash", additional_data=None)
        MockDb.cypher_query.return_value = [], True
        MockGetOrg.return_value = "url_to_tp"
        MockTokenRequest.return_value = get_dummy_token()
        trusted_party = TrustedParty(identifier="trusted_party")
        MockGetTokenToStore.return_value = token1, document, trusted_party
        MockDb.transaction.side_effect = None
        MockStoreToken.return_value = token

        token2 = get_token("org_id", "graph_id", document)

        self.assertEqual("originator_id", token2["data"]["originatorId"])
        self.assertEqual("authority_id", token2["data"]["authorityId"])
        self.assertEqual(9, token2["data"]["tokenTimestamp"])
        self.assertEqual(10, token2["data"]["documentCreationTimestamp"])
        self.assertEqual("hash", token2["data"]["documentDigest"])
        self.assertEqual(None, token2["data"]["additionalData"])
        self.assertEqual(None, token2["signature"])
        MockDb.cypher_query.assert_called_with("""
                MATCH (org:Organization) WHERE org.identifier=$organization_id
                MATCH (org)-[:trusts]->(tp:TrustedParty)<-[:was_issued_by]-(token:Token)-[:belongs_to]->(doc:Document)
                WHERE doc.identifier=$doc_id
                RETURN token
                """, {"organization_id": "org_id", "doc_id": "org_id_graph_id"}, resolve_objects=True)
        MockIs_org_registered.assert_called_with("org_id")
        MockGetOrg.assert_called_with("org_id")
        MockTokenRequest.assert_called_with({"graph": document.graph}, MockGetOrg.return_value)
        MockGetTokenToStore.assert_called_with(get_dummy_token(), None, document)
        MockStoreToken.assert_called_with(token1, document, trusted_party)

    @patch('provenance.models.Bundle.nodes')
    @patch('provenance.controller.convert_meta_to_prov')
    def test_get_b64_encoded_meta_provenance_returns_b64_graph(self, MockConvertToProv, MockBundle):
        meta_id = "prov:meta_1"
        neo_bundle = Bundle(identifier=meta_id)
        document = ProvDocument(namespaces=[NAMESPACES["meta"]])
        MockBundle.get = MagicMock()
        MockBundle.get.return_value = neo_bundle
        MockConvertToProv.return_value = document

        b64_encoded = get_b64_encoded_meta_provenance(meta_id, "json")

        MockBundle.get.assert_called_with(identifier=meta_id)
        MockConvertToProv.assert_called_with(neo_bundle)
        self.assertEqual(base64.b64encode((document.serialize(format="json"))
                                          .encode("utf-8")).decode("utf-8"), b64_encoded)

    def test_retrieve_subgraph_domain_specific_returns_domain_part(self):
        storage_name = "provider"
        org_name = "org"
        main_activity_attributes_ok, _, timestamp = create_main_act_attributes(storage_name)
        document, bundle, meta_bundle_id, backbone_parts, _, _ = create_cpm_provenance_basic("_", storage_name,
                                                                                             org_name,
                                                                                             timestamp,
                                                                                             main_activity_attributes_ok)

        doc = retrieve_subgraph(document.serialize(), "json", True)

        self.assertEqual(list(doc.namespaces), list(document.namespaces))
        bundle_returned = list(doc.bundles)[0]

        for record in bundle.records:
            if record not in backbone_parts:
                self.assertTrue(record in bundle_returned.records)
            else:
                self.assertTrue(record not in bundle_returned.records)
        self.assertEqual(bundle_returned.identifier, bundle.identifier)

    def test_retrieve_subgraph_domain_specific_2_returns_domain_part(self):
        storage_name = "provider"
        org_name = "org"
        main_activity_attributes_ok, main_act_attr_2, timestamp = create_main_act_attributes(storage_name)
        document, bundle, meta_bundle_id, backbone_parts, prev_cpm_bundle_info, _ \
            = create_cpm_provenance_basic("_", storage_name, org_name, timestamp, main_activity_attributes_ok)

        prev_meta_bundle_info = [meta_bundle_id.namespace.uri, meta_bundle_id.localpart]
        bundle_with_backwards_conn, document_with_backwards_connector, backward_connector_attributes, _ = prepare_attributes_for_doc_w_backward_connectors(
            prev_cpm_bundle_info,
            prev_meta_bundle_info,
            timestamp)

        doc2, backbone_parts2, _, _ = create_cpm_provenance_with_backward_connector(document_with_backwards_connector,
                                                                                    bundle_with_backwards_conn, "_",
                                                                                    prev_cpm_bundle_info=prev_cpm_bundle_info,
                                                                                    prev_meta_bundle_info=prev_meta_bundle_info,
                                                                                    sender_org_name="hospital",
                                                                                    main_activity_attributes=main_act_attr_2,
                                                                                    backward_connector_attributes=backward_connector_attributes)

        doc = retrieve_subgraph(doc2.serialize(), "json", True)

        self.assertEqual(list(doc.namespaces), list(doc2.namespaces))
        bundle_returned = list(doc.bundles)[0]
        print(bundle_returned.records)

        bundle2 = list(doc2.bundles)[0]
        for record in bundle2.records:
            if record not in backbone_parts2:
                self.assertTrue(record in bundle_returned.records)
            else:
                self.assertTrue(record not in bundle_returned.records)

    def test_retrieve_subgraph_backbone_returns_traversal_information(self):
        storage_name = "storage"
        org_name = "org"
        main_activity_attributes_ok, _, timestamp = create_main_act_attributes(storage_name)
        document, bundle, meta_bundle_id, backbone_parts, _, _ = create_cpm_provenance_basic("_", storage_name,
                                                                                             org_name,
                                                                                             timestamp,
                                                                                             main_activity_attributes_ok)

        doc = retrieve_subgraph(document.serialize(), "json", False)

        self.assertEqual(list(doc.namespaces), list(document.namespaces))
        bundle_returned = list(doc.bundles)[0]

        for record in bundle.records:
            if record in backbone_parts:
                self.assertTrue(record in bundle_returned.records)
            else:
                self.assertTrue(record not in bundle_returned.records)
        self.assertEqual(bundle_returned.identifier, bundle.identifier)

    def test_retrieve_subgraph_backbone_2_returns_traversal_information(self):
        storage_name = "provider"
        org_name = "org"
        main_activity_attributes_ok, maa2, timestamp = create_main_act_attributes(storage_name)
        document, bundle, meta_bundle_id, backbone_parts, prev_cpm_bundle_info, _ \
            = create_cpm_provenance_basic("_", storage_name, org_name, timestamp, main_activity_attributes_ok)

        prev_meta_bundle_info = [meta_bundle_id.namespace.uri, meta_bundle_id.localpart]
        (bundle_with_backwards_conn, document_with_backwards_connector,
         backward_connector_attributes, _) = prepare_attributes_for_doc_w_backward_connectors(
            prev_cpm_bundle_info,
            prev_meta_bundle_info,
            timestamp)

        doc2, backbone_parts2, _, _ = create_cpm_provenance_with_backward_connector(document_with_backwards_connector,
                                                                                    bundle_with_backwards_conn, "_",
                                                                                    prev_cpm_bundle_info=prev_cpm_bundle_info,
                                                                                    prev_meta_bundle_info=prev_meta_bundle_info,
                                                                                    sender_org_name="hospital_org",
                                                                                    main_activity_attributes=maa2,
                                                                                    backward_connector_attributes=backward_connector_attributes)

        doc = retrieve_subgraph(doc2.serialize(), "json", False)

        self.assertEqual(list(doc.namespaces), list(doc2.namespaces))
        bundle_returned = list(doc.bundles)[0]
        bundle2 = list(doc2.bundles)[0]

        for record in bundle2.records:
            if record in backbone_parts2:
                self.assertTrue(record in bundle_returned.records)
            else:
                self.assertTrue(record not in bundle_returned.records)

    @patch('provenance.controller.get_tp')
    @patch('provenance.controller.store_organization')
    @patch('provenance.controller.db')
    def test_create_organization_creates_organizations_and_calls_store(self, MockDb, MockStoreOrg, GetTpMock):
        org_id = "ORG"
        org_client_cert = "client"
        org_intermediate = ["interm1", "interm2"]
        tp_uri = "uri.com"
        trusted_party = MagicMock()
        trusted_party.save.side_effect = None
        GetTpMock.return_value = trusted_party, False
        MockDb.transaction.side_effect = None

        create_and_store_organization(org_id, org_client_cert, org_intermediate, tp_uri)

        args = MockStoreOrg.call_args
        org = args[0][0]
        tp = args[0][1]
        GetTpMock.assert_called_with(tp_uri)
        self.assertEqual(org_id, org.identifier)
        self.assertEqual(org_client_cert, org.client_cert)
        self.assertEqual(org_intermediate, org.intermediate_certs)
        self.assertEqual(trusted_party, tp)

    def test_store_organization_saves_organization(self):
        org = MagicMock()
        tp = TrustedParty()

        org.save.side_effect = None
        org.trusts.connect.side_effect = None

        store_organization(org, tp)

        org.save.assert_called_once()
        org.trusts.connect.assert_called_with(tp)

    @patch("requests.head")
    def test__verify_at_remote_storage_both_ok_returns_true(self, MockRequest):
        referenced_bundle_id = MagicMock()
        referenced_meta_bundle_id = MagicMock()
        response = Response()
        response.status_code = 200
        MockRequest.return_value = response

        self.assertTrue(_verify_at_remote_storage(referenced_bundle_id, referenced_meta_bundle_id))
        MockRequest.assert_has_calls([call(referenced_bundle_id.uri), call(referenced_meta_bundle_id.uri)], True)

    @patch("requests.head")
    def test__verify_at_remote_storage_one_nok_returns_false(self, MockRequest):
        referenced_bundle_id = MagicMock()
        referenced_meta_bundle_id = MagicMock()
        response = Response()
        response.status_code = 200
        response_nok = Response()
        response_nok.status_code = 400
        MockRequest.side_effect = response, response_nok

        self.assertFalse(_verify_at_remote_storage(referenced_bundle_id, referenced_meta_bundle_id))
        MockRequest.assert_has_calls([call(referenced_bundle_id.uri), call(referenced_meta_bundle_id.uri)], True)

    @patch("requests.head")
    def test__verify_at_remote_storage_one_nok_returns_false_2(self, MockRequest):
        referenced_bundle_id = MagicMock()
        referenced_meta_bundle_id = MagicMock()
        response = Response()
        response.status_code = 200
        response_nok = Response()
        response_nok.status_code = 400
        MockRequest.side_effect = response_nok, response

        self.assertFalse(_verify_at_remote_storage(referenced_bundle_id, referenced_meta_bundle_id))
        MockRequest.assert_has_calls([call(referenced_bundle_id.uri), call(referenced_meta_bundle_id.uri)], True)

    @patch("requests.head")
    def test__verify_at_remote_storage_both_nok_returns_false(self, MockRequest):
        referenced_bundle_id = MagicMock()
        referenced_meta_bundle_id = MagicMock()
        response_nok = Response()
        response_nok.status_code = 400
        MockRequest.return_value = response_nok

        self.assertFalse(_verify_at_remote_storage(referenced_bundle_id, referenced_meta_bundle_id))
        MockRequest.assert_has_calls([call(referenced_bundle_id.uri), call(referenced_meta_bundle_id.uri)], True)

    @patch('provenance.models.Document.nodes')
    def test_bundle_exists_exists_returns_true(self, MockDocumentNodes):
        id = "id"
        MockDocumentNodes.get_or_none = MagicMock()
        MockDocumentNodes.get_or_none.return_value = Document()
        exists = bundle_exists(id)

        self.assertEqual(True, exists)
        MockDocumentNodes.get_or_none.assert_called_with(identifier=id, lazy=True)

    @patch('provenance.models.Document.nodes')
    def test_bundle_exists_does_not_exists_returns_false(self, MockDocumentNodes):
        id = "id"
        MockDocumentNodes.get_or_none = MagicMock()
        MockDocumentNodes.get_or_none.return_value = None
        exists = bundle_exists(id)

        self.assertEqual(False, exists)
        MockDocumentNodes.get_or_none.assert_called_with(identifier=id, lazy=True)

    @patch('provenance.models.Bundle.nodes')
    def test_meta_bundle_exists_exists_returns_true(self, MockDocumentNodes):
        id = "id"
        MockDocumentNodes.get_or_none = MagicMock()
        MockDocumentNodes.get_or_none.return_value = Bundle()
        exists = meta_bundle_exists(id)

        self.assertEqual(True, exists)
        MockDocumentNodes.get_or_none.assert_called_with(identifier=id, lazy=True)

    @patch('provenance.models.Bundle.nodes')
    def test_meta_bundle_exists_dos_not_exists_returns_false(self, MockDocumentNodes):
        id = "id"
        MockDocumentNodes.get_or_none = MagicMock()
        MockDocumentNodes.get_or_none.return_value = None
        exists = meta_bundle_exists(id)

        self.assertEqual(False, exists)
        MockDocumentNodes.get_or_none.assert_called_with(identifier=id, lazy=True)

    @patch('provenance.controller.bundle_exists')
    @patch('provenance.controller.meta_bundle_exists')
    def test__verify_at_local_storage_exists_returns_true(self, MockMetaBundleExists, MockBundleExists):
        referenced_bundle_id = QualifiedName(Namespace("org", "http://www.w3.org/organizations/org_id/end"),
                                             "bundle_id")
        referenced_meta_bundle_id = QualifiedName(NAMESPACES["meta"], "meta_bundle_id")

        MockMetaBundleExists.return_value = True
        MockBundleExists.return_value = True

        result = _verify_at_local_storage(referenced_bundle_id, referenced_meta_bundle_id)

        MockBundleExists.assert_called_with("org_id_bundle_id")
        MockMetaBundleExists.assert_called_with("meta_bundle_id")
        self.assertTrue(result)

    @patch('provenance.controller.bundle_exists')
    @patch('provenance.controller.meta_bundle_exists')
    def test__verify_at_local_storage_not_exists_returns_false(self, MockMetaBundleExists, MockBundleExists):
        referenced_bundle_id = QualifiedName(Namespace("org", "http://www.w3.org/organizations/org_id/end"),
                                             "bundle_id")
        referenced_meta_bundle_id = QualifiedName(NAMESPACES["meta"], "meta_bundle_id")

        MockMetaBundleExists.return_value = False
        MockBundleExists.return_value = True

        result = _verify_at_local_storage(referenced_bundle_id, referenced_meta_bundle_id)

        self.assertFalse(result)

        MockMetaBundleExists.return_value = True
        MockBundleExists.return_value = False

        result = _verify_at_local_storage(referenced_bundle_id, referenced_meta_bundle_id)

        self.assertFalse(result)

    @patch('provenance.controller._verify_at_remote_storage')
    def test__referenced_bundles_exist_remote_returns_response_from_remote(self, MockVerifyRemote):
        bundle = ProvBundle(identifier="bundle")
        connector = ProvRecord(bundle, "id")
        connector._attributes = {CPM_REFERENCED_BUNDLE_ID: [
            QualifiedName(Namespace("org", "http://www.w3.org/organizations/org_id/end"), "bundle_id")],
            CPM_REFERENCED_META_BUNDLE_ID: [
                QualifiedName(Namespace("org", "http://www.w3.org/organizations/org_id/end"),
                              "ref_bundle_id")]}
        MockVerifyRemote.return_value = True

        result = _referenced_bundles_exist("storage_url", connector)

        self.assertTrue(result)
        MockVerifyRemote.assert_called_with(
            QualifiedName(Namespace("org", "http://www.w3.org/organizations/org_id/end"), "bundle_id"),
            QualifiedName(
                Namespace("org", "http://www.w3.org/organizations/org_id/end"),
                "ref_bundle_id")
        )

    @patch('provenance.controller._verify_at_local_storage')
    def test__referenced_bundles_exist_local_returns_response_from_local(self, MockVerifyLocal):
        bundle = ProvBundle(identifier="bundle")
        connector = ProvRecord(bundle, "id")
        connector._attributes = {CPM_REFERENCED_BUNDLE_ID: [
            QualifiedName(Namespace("org", "http://www.w3.org/organizations/org_id/end"), "bundle_id")],
            CPM_REFERENCED_META_BUNDLE_ID: [
                QualifiedName(Namespace("org", "http://www.w3.org/organizations/org_id/end"),
                              "ref_bundle_id")]}
        MockVerifyLocal.return_value = False

        result = _referenced_bundles_exist("", connector)

        self.assertFalse(result)
        MockVerifyLocal.assert_called_with(
            QualifiedName(Namespace("org", "http://www.w3.org/organizations/org_id/end"), "bundle_id"),
            QualifiedName(
                Namespace("org", "http://www.w3.org/organizations/org_id/end"),
                "ref_bundle_id")
        )

    @patch('provenance.controller._referenced_bundles_exist')
    def test__check_referenced_bundles_not_exist_returns_false(self, MockBundlesExist):
        connectors = [["", "connector1"], ["url...", "connector2"]]
        MockBundlesExist.side_effect = [True, False]

        result = _check_referenced_bundles(connectors)

        MockBundlesExist.assert_any_call(connectors[0][0], connectors[0][1])
        MockBundlesExist.assert_any_call(connectors[1][0], connectors[1][1])
        self.assertFalse(result)

    @patch('provenance.controller._referenced_bundles_exist')
    def test__check_referenced_bundles_exist_returns_true(self, MockBundlesExist):
        connectors = [["", "connector1"], ["url...", "connector2"]]
        MockBundlesExist.side_effect = [True, True]

        result = _check_referenced_bundles(connectors)

        self.assertTrue(result)

    @patch('provenance.controller._check_referenced_bundles')
    def test_check_connectors_ok_returns_true(self, MockCheckBundles):
        forward_connectors = "forward_connectors"
        backward_connectors = "backward_connectors"
        MockCheckBundles.return_value = True

        result_ok = check_connectors(forward_connectors, backward_connectors)

        MockCheckBundles.assert_any_call(forward_connectors)
        MockCheckBundles.assert_any_call(backward_connectors)
        self.assertTrue(result_ok)

    @patch('provenance.controller._check_referenced_bundles')
    def test_check_connectors_nok_returns_false(self, MockCheckBundles):
        forward_connectors = "forward_connectors"
        backward_connectors = "backward_connectors"
        MockCheckBundles.return_value = False

        result_nok = check_connectors(forward_connectors, backward_connectors)

        MockCheckBundles.assert_any_call(forward_connectors)
        self.assertFalse(result_nok)

    @patch('provenance.controller.get_tp')
    @patch('provenance.models.Organization.nodes')
    @patch('provenance.controller.store_organization')
    @patch('provenance.controller.db')
    def test_modify_organization_modifies_organization_calls_store(self, MockDb, MockStoreOrg, MockOrgNodes, MockGetTP):
        org = Organization()
        org.client_cert = "client"
        org.intermediate_certs = ["cert1"]
        MockGetTP.return_value = "tp", False
        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = org
        MockDb.transaction.side_effect = None

        modify_organization("id", "new_client", ["new_cert1"], "tp:uri")

        args = MockStoreOrg.call_args
        new_org = args[0][0]
        new_tp = args[0][1]
        MockOrgNodes.get.assert_called_with(identifier="id")
        self.assertEqual("new_client", new_org.client_cert)
        self.assertEqual(["new_cert1"], new_org.intermediate_certs)
        self.assertEqual("tp", new_tp)

    @patch('provenance.models.Organization.nodes')
    def test_get_tp_url_by_organization_retrieves_trusted_party(self, MockOrgNodes):
        org = MagicMock()
        trusted_party = TrustedParty()
        trusted_party.url = "url..."
        org.trusts.all.return_value = [trusted_party]
        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = org

        result = get_tp_url_by_organization("id")

        self.assertEqual(trusted_party.url, result)

    @patch('provenance.models.Organization.nodes')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    def test_get_tp_url_by_organization_does_not_exist_returns_None(self, MockDoesnotExist, MockOrgNodes):
        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = None
        MockOrgNodes.get.side_effect = DoesNotExist("no")

        result = get_tp_url_by_organization("id")

        self.assertEqual(None, result)

    @patch('provenance.models.Organization.nodes')
    def test_is_org_registered_true_returns_true(self, MockOrgNodes):
        org_id = "org_id"
        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = None

        result = is_org_registered(org_id)

        self.assertTrue(result)

    @patch('provenance.models.Organization.nodes')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    def test_is_org_registered_true_returns_false(self, MockDoesnotExist, MockOrgNodes):
        org_id = "org_id"
        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = None
        MockOrgNodes.get.side_effect = DoesNotExist("no")

        result = is_org_registered(org_id)

        self.assertFalse(result)

    @patch('provenance.controller.requests')
    def test_send_token_request_to_tp_ok_sends_request_returns_result(self, MockRequests):
        payload = {"something": "value"}
        MockRequests.post = MagicMock()
        MockRequests.post.return_value = requests.Response()
        MockRequests.post.return_value.status_code = 200
        MockRequests.post.return_value._content = json.dumps(payload)

        tp_url = "tp_url"

        result = send_token_request_to_tp(payload, tp_url)

        self.assertEqual(payload, result)
        MockRequests.post.assert_called_with(f"http://{tp_url}/api/v1/issueToken", json.dumps(payload))

    @patch('provenance.controller.requests')
    def test_send_token_request_to_tp_nok_response_throws_exception(self, MockRequests):
        payload = {"something": "value"}
        MockRequests.post = MagicMock()
        MockRequests.post.return_value = requests.Response()
        MockRequests.post.return_value.status_code = 400
        MockRequests.post.return_value._content = json.dumps(payload)

        tp_url = "tp_url"

        self.assertRaises(AssertionError, send_token_request_to_tp, payload, tp_url)

    @patch('provenance.models.DefaultTrustedParty.nodes')
    @patch('provenance.models.TrustedParty.nodes')
    def test_get_tp_called_with_no_url_returns_default_trusted_party(self, MockTrustedPartyNodes, MockDefaultTrustedPartyNodesGet):
        mock_default_tp = MagicMock()
        mock_default_tp.identifier = "tp_id"

        trusted_party = "tp"
        MockDefaultTrustedPartyNodesGet.all = MagicMock()
        MockDefaultTrustedPartyNodesGet.all.return_value = [mock_default_tp]
        MockTrustedPartyNodes.get = MagicMock()
        MockTrustedPartyNodes.get.return_value = trusted_party

        result = get_tp(None)

        self.assertEqual((trusted_party, False), result)
        MockTrustedPartyNodes.get.assert_called_with(identifier=mock_default_tp.identifier)

    @patch('provenance.models.DefaultTrustedParty.nodes')
    @patch('provenance.models.TrustedParty.nodes')
    def test_get_tp_with_no_url_multiple_default_tps_throws_exception(self, MockTrustedPartyNodes, MockDefaultTrustedPartyNodesGet):
        mock_default_tp = MagicMock()
        mock_default_tp.identifier = "tp_id"

        trusted_party = "tp"
        MockDefaultTrustedPartyNodesGet.all = MagicMock()
        MockDefaultTrustedPartyNodesGet.all.return_value = [mock_default_tp, mock_default_tp]
        MockTrustedPartyNodes.get = MagicMock()
        MockTrustedPartyNodes.get.return_value = trusted_party

        self.assertRaises(AssertionError, get_tp, None)

    @patch('provenance.models.TrustedParty.nodes')
    @patch('provenance.controller.requests')
    def test_get_tp_url_saved_tp_ok_returns_trusted_party(self, MockRequests, MockTrustedPartyNodes):
        trusted_party = {"id": "tp_id"}
        MockRequests.get = MagicMock()
        MockRequests.get.return_value = requests.Response()
        MockRequests.get.return_value.status_code = 200
        MockRequests.get.return_value._content = json.dumps(trusted_party)

        MockTrustedPartyNodes.get = MagicMock()
        MockTrustedPartyNodes.get.return_value = trusted_party

        result = get_tp("tp_url")

        self.assertEqual((trusted_party, False), result)
        MockTrustedPartyNodes.get.assert_called_with(identifier=trusted_party["id"])
        MockRequests.get.assert_called_with("http://tp_url/api/v1/info")

    @patch('provenance.models.TrustedParty.nodes')
    @patch('provenance.controller.requests')
    def test_get_tp_url_saved_tp_wrong_response_from_tp_throws_exception(self, MockRequests, MockTrustedPartyNodes):
        trusted_party = {"id": "tp_id"}
        MockRequests.get = MagicMock()
        MockRequests.get.return_value = requests.Response()
        MockRequests.get.return_value.status_code = 400
        MockRequests.get.return_value._content = json.dumps(trusted_party)

        MockTrustedPartyNodes.get = MagicMock()
        MockTrustedPartyNodes.get.return_value = trusted_party

        self.assertRaises(AssertionError, get_tp, "tp_url")

    @patch('provenance.models.TrustedParty.nodes')
    @patch('provenance.controller.requests')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    def test_get_tp_url_new_trusted_party_ok_creates_and_returns_tp(self, MockDoesNotExist, MockRequests, MockTrustedPartyNodes):
        trusted_party = {"id": "tp_id", "certificate": "cert"}
        MockRequests.get = MagicMock()
        MockRequests.get.return_value = requests.Response()
        MockRequests.get.return_value.status_code = 200
        MockRequests.get.return_value._content = json.dumps(trusted_party)

        MockTrustedPartyNodes.get = MagicMock()
        MockTrustedPartyNodes.get.return_value = None
        MockTrustedPartyNodes.get.side_effect = DoesNotExist("no")

        tp, new_tp = get_tp("tp_url")

        self.assertEqual(True, new_tp)
        self.assertEqual(trusted_party["id"], tp.identifier)
        self.assertEqual(trusted_party["certificate"], tp.certificate)
        self.assertEqual("tp_url", tp.url)
        MockTrustedPartyNodes.get.assert_called_with(identifier=trusted_party["id"])
        MockRequests.get.assert_called_with("http://tp_url/api/v1/info")
