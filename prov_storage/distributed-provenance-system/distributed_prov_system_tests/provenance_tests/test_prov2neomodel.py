import unittest

from django.db.models.fields import return_None
from prov.constants import PROV_BUNDLE

from provenance.prov2neomodel import *

from unittest.mock import MagicMock
from unittest.mock import patch

class MyTestCase(unittest.TestCase):
    def test_create_gen_entity_and_first_version(self):
        first_version, gen_entity = create_gen_entity_and_first_version("ORG_id1")

        self.assertEqual("ORG_id1", first_version.identifier)
        self.assertEqual("ORG_gen", gen_entity.identifier)
        self.assertEqual('prov:bundle', first_version.attributes['prov:type'])
        self.assertEqual('prov:bundle', gen_entity.attributes['prov:type'])
        self.assertEqual(1, first_version.attributes['pav:version'])


    def MockEntity(self):
        self.attributes = {}

    @patch('provenance.prov2neomodel.Bundle')
    @patch('provenance.prov2neomodel.Entity')
    @patch('provenance.prov2neomodel.neomodel.Traversal')
    def test_update_meta_prov(self, MockTraversal, MockEntity, MockBundle):
        graph_id = "id1"
        new_entity_id = "ORG_bundleid"
        token = get_dummy_token()["data"]
        meta_id = "metaid"
        meta_bundle = MockBundle()
        meta_bundle.identifier = "id"
        first_entity = Entity()
        first_entity.identifier = "first"
        first_entity.attributes = {str(PAV_VERSION): 1}
        latest_entity = MockEntity()
        latest_entity.attributes = {str(PAV_VERSION):2}
        gen_entity = Entity()
        gen_entity.identifier = "id_gen"
        mock_first = MagicMock()
        mock_first.first.return_value = gen_entity
        meta_bundle.contains.filter = MagicMock(return_value=mock_first)
        MockBundle.nodes.get = MagicMock(return_value=meta_bundle)
        MockEntity.contains = MagicMock(return_value=[latest_entity])
        MockEntity.__label__ = MagicMock()
        mocked_entity = MagicMock()
        mocked_entity.specialization_of.all.return_value = iter([gen_entity])
        MockEntity.nodes.get.return_value = mocked_entity
        mock_all = MagicMock()
        mock_all.all.return_value = [latest_entity, first_entity]
        MockTraversal.return_value = mock_all

        (gen_entity, latest_entity2, meta_bundle_1, new_version, token2) = update_meta_prov(new_entity_id, meta_id, token, "graph_id")

        MockBundle.nodes.get.assert_called_with(identifier=meta_id)
        self.assertEqual(meta_bundle, meta_bundle_1)
        self.assertEqual(new_entity_id, new_version.identifier)
        self.assertEqual(3, new_version.attributes[str(PAV_VERSION)])
        self.assertEqual("id_gen", gen_entity.identifier)
        self.assertEqual(latest_entity, latest_entity2)
        self.assertEqual(token, token2)
        MockTraversal.assert_called_with(gen_entity, MockEntity.__label__, definition={
            "node_class": MockEntity,
            "direction": neomodel.INCOMING,
            "relation_type": "specialization_of",
            "model": None,
        })
        MockEntity.nodes.get.assert_called_with(identifier=token['originatorId'] + "_" + "graph_id")

    @patch('provenance.prov2neomodel.get_tp_agent')
    def test_store_token_into_meta(self, getAgent):
        meta_bundle = Bundle()
        meta_bundle.identifier = "id"
        entity = Entity()
        entity.identifier = "id_entity"
        token = get_dummy_token()["data"]
        #mock get agent return value
        agent = Agent()
        agent.identifier = "id_ag"
        getAgent.return_value = (agent, False)

        (activity, agent_returned, new_agent, e, entity2, meta_bundle2) = store_token_into_meta(meta_bundle, entity, token)

        getAgent.assert_called_with(meta_bundle, token)
        self.assertEqual(meta_bundle, meta_bundle2)
        self.assertEqual(entity, entity2)
        self.assertEqual("id_entity_token", e.identifier)
        self.assertEqual({
        "prov:type": "cpm:token",
        "cpm:originatorId": "ORG",
        "cpm:authorityId": "TrustedParty",
        "cpm:tokenTimestamp": 0,
        "cpm:documentCreationTimestamp": 0,
        "cpm:documentDigest": "17fd7484d7cac628cfa43c348fe05a009a81d18c8a778e6488b707954addf2a3",
        "cpm:bundle": "http://prov-provider",
        "cpm:hashFunction": "SHA256",
        'cpm:trustedPartyUri': "uri",
        'cpm:trustedPartyCertificate': "cert"
        }, e.attributes)
        self.assertEqual("id_entity_tokenGeneration", activity.identifier)
        self.assertEqual(datetime.fromtimestamp(token['tokenTimestamp']), activity.start_time)
        self.assertEqual(datetime.fromtimestamp(token['tokenTimestamp']), activity.end_time)
        self.assertEqual({"prov:type": 'cpm:tokenGeneration'}, activity.attributes)
        self.assertEqual(agent, agent_returned)

    @patch('provenance.prov2neomodel.neomodel.Traversal.all')
    def test_get_TP_agent_new_agent(self, MockTraversal):
        meta_bundle = Bundle()
        meta_bundle.identifier = "id"
        token = get_dummy_token()["data"]
        MockTraversal.return_value = []

        agent2, new_agent  = get_tp_agent(meta_bundle, token)

        self.assertEqual(True, new_agent)
        self.assertEqual(token["authorityId"], agent2.identifier)
        self.assertEqual('cpm:trustedParty', agent2.attributes[str(PROV_TYPE)])
        self.assertEqual(token['additionalData']['trustedPartyUri'], agent2.attributes[str(CPM_TRUSTED_PARTY_URI)])
        self.assertEqual(token['additionalData']['trustedPartyCertificate'], agent2.attributes[str(CPM_TRUSTED_PARTY_CERTIFICATE)])

    @patch('provenance.prov2neomodel.neomodel.Traversal.all')
    def test_get_TP_agent_old_agent(self, MockTraversal):
        meta_bundle = Bundle()
        meta_bundle.identifier = "id"
        token = get_dummy_token()["data"]
        agent = Agent()
        agent.identifier = token["authorityId"]
        MockTraversal.return_value = [agent]

        agent2, new_agent = get_tp_agent(meta_bundle, token)

        self.assertEqual(False, new_agent)
        self.assertEqual(agent.identifier, agent2.identifier)

    def test_save_new_agent(self):
        agent = MagicMock()
        meta_bundle = MagicMock()

        save_new_agent(agent, meta_bundle)

        agent.save.assert_called()
        meta_bundle.contains.connect.assert_any_call(agent)

    def test_save_version_to_db(self):
        first_version = Entity()
        first_version.identifier = "id1"
        gen_entity = Entity()
        gen_entity.identifier = "id2"
        meta_bundle = Bundle()
        meta_bundle.identifier = "id3"
        first_version.save = MagicMock()
        gen_entity.save = MagicMock()
        meta_bundle.contains.connect = MagicMock()
        first_version.specialization_of.connect = MagicMock()

        save_version_to_db(first_version, gen_entity, meta_bundle)

        gen_entity.save.assert_called()
        first_version.save.assert_called()
        meta_bundle.contains.connect.assert_any_call(gen_entity)
        meta_bundle.contains.connect.assert_any_call(first_version)
        first_version.specialization_of.connect.assert_called_with(gen_entity)

    def test_update_meta_prov_save(self):
        gen_entity = Entity()
        gen_entity.identifier = "id1"
        latest_entity = Entity()
        latest_entity.identifier = "id2"
        meta_bundle = Bundle()
        meta_bundle.identifier = "id3"
        new_version = Entity()
        new_version.identifier = "id4"
        new_version.save = MagicMock()
        meta_bundle.contains.connect = MagicMock()
        new_version.specialization_of.connect = MagicMock()
        new_version.was_revision_of.connect = MagicMock()

        update_meta_prov_save(gen_entity, latest_entity, meta_bundle, new_version)

        new_version.save.assert_called()
        meta_bundle.contains.connect.assert_called_with(new_version)
        new_version.specialization_of.connect.assert_called_with(gen_entity)
        new_version.was_revision_of.connect.assert_called_with(latest_entity)


    def test_store_token_into_meta_save(self):
        activity = Activity()
        activity.identifier = "id1"
        agent = Agent()
        agent.identifier = "id2"
        e = Entity()
        e.identifier = "id3"
        entity = Entity()
        entity.identifier = "id4"
        meta_bundle = Bundle()
        meta_bundle.identifier = "id5"
        activity.save = MagicMock()
        e.save = MagicMock()
        meta_bundle.contains.connect = MagicMock()
        activity.used.connect = MagicMock()
        activity.was_associated_with.connect = MagicMock()
        e.was_generated_by.connect = MagicMock()
        e.was_attributed_to.connect = MagicMock()

        store_token_into_meta_save(activity, agent, e, entity, meta_bundle)

        activity.save.assert_called()
        e.save.assert_called()
        meta_bundle.contains.connect.assert_any_call(e)
        meta_bundle.contains.connect.assert_any_call(activity)
        activity.used.connect.assert_called_with(entity)
        activity.was_associated_with.connect.assert_called_with(agent)
        e.was_generated_by.connect.assert_called_with(activity)
        e.was_attributed_to.connect.assert_called_with(agent)


    def test_import_graph_no_bundle_should_fail(self):
        document = provm.ProvDocument()
        json_data = {}
        token = get_dummy_token()
        graph_id = "id_graph"
        meta_id = "id_meta"

        with self.assertRaises(AssertionError):
            import_graph(document, json_data, token, meta_id, False)


    def test_import_graph_many_bundles_should_fail(self):
        document = provm.ProvDocument()
        json_data = {}
        token = get_dummy_token()
        graph_id = "id_graph"
        meta_id = "id_meta"
        document.set_default_namespace('http://example.org/0/')
        document.entity('e001')
        document.entity('e002')
        document.bundle('e001')
        document.bundle('e002')

        with self.assertRaises(AssertionError):
            import_graph(document, json_data, token, meta_id, False)

    @patch('provenance.prov2neomodel.store_token_into_meta_save')
    @patch('provenance.prov2neomodel.save_new_agent')
    @patch('provenance.prov2neomodel.save_version_to_db')
    @patch('provenance.prov2neomodel.db')
    @patch('provenance.prov2neomodel.neomodel.Traversal')
    @patch('provenance.prov2neomodel.Document', autospec=True)
    @patch('provenance.prov2neomodel.Bundle')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='banana')
    def test_import_graph_new_bundle_not_update(self, MockDoesnotExist, MockBundle, MockDocument, MockTraversal, MockDb, MockSaveVersion, MockSaveAgent, MockStoreToken):
        document = provm.ProvDocument()
        json_data = {'document': "graph", "documentFormat": "none"}
        token = get_dummy_token()
        graph_id = "id_graph"
        meta_id = "id_meta"
        document.set_default_namespace( "local part")
        document.entity('e001')
        document.bundle('e001')

        MockBundle.nodes.get = MagicMock()
        MockBundle.nodes.get.return_value = None
        MockBundle.nodes.get.side_effect = DoesNotExist("Test")
        agent = Agent()
        agent.identifier = token["data"]["authorityId"]
        agent.attributes = {'prov:type': 'cpm:trustedParty', 'cpm:trustedPartyUri': 'uri', 'cpm:trustedPartyCertificate': 'cert'}
        MockDocument.save = MagicMock()
        MockDocument.save.return_value = None
        MockDb.transaction.side_effect = None

        import_graph(document, json_data, token, meta_id, False)

        self.assertEqual(1, MockDocument.return_value.save.call_count)
        self.assertEqual(f"{token["data"]['originatorId']}_e001", MockDocument.return_value.identifier)
        #check attributes of new document
        self.assertEqual(json_data['document'], MockDocument.return_value.graph)
        self.assertEqual(json_data['documentFormat'], MockDocument.return_value.format)
        self.assertEqual(meta_id, MockBundle.return_value.identifier)
        MockBundle.nodes.get.assert_called_with(identifier='id_meta')
        self.assertEqual(1, MockBundle.return_value.save.call_count)

        first_version_test, gen_entity_test, meta_bundle_test = MockSaveVersion.call_args.args
        assert isinstance(first_version_test, Entity)
        self.assertEqual(f"{token["data"]['originatorId']}_e001", first_version_test.identifier)
        self.assertEqual({'prov:type': 'prov:bundle', 'pav:version': 1}, first_version_test.attributes)
        assert isinstance(gen_entity_test, Entity)
        self.assertEqual("ORG" + '_gen', gen_entity_test.identifier)
        self.assertEqual({'prov:type': 'prov:bundle'}, gen_entity_test.attributes)
        self.assertEqual(MockBundle.return_value, meta_bundle_test)

        agent_test, meta_bundle_test = MockSaveAgent.call_args.args
        assert isinstance(agent_test, Agent)
        self.assertEqual(agent.identifier, agent_test.identifier)
        self.assertEqual(agent.attributes, agent_test.attributes)
        self.assertEqual(MockBundle.return_value, meta_bundle_test)

        activity_test, agent_test_2, e_test, entity_test, meta_bundle_test2 = MockStoreToken.call_args.args
        assert isinstance(activity_test,Activity)
        self.assertEqual(f"{first_version_test.identifier}_tokenGeneration", activity_test.identifier)
        self.assertEqual(datetime.fromtimestamp(token["data"]['tokenTimestamp']), activity_test.start_time)
        self.assertEqual(datetime.fromtimestamp(token["data"]['tokenTimestamp']), activity_test.end_time)
        self.assertEqual({"prov:type": 'cpm:tokenGeneration'}, activity_test.attributes)
        self.assertEqual(agent_test, agent_test_2)
        assert isinstance(e_test, Entity)
        self.assertEqual(f"{entity_test.identifier}_token", e_test.identifier)
        self.assertEqual(first_version_test, entity_test)
        self.assertEqual(meta_bundle_test, meta_bundle_test2)

        definition = dict(node_class=Agent, direction=neomodel.OUTGOING,
                          relation_type="contains", model=None)
        self.assertEqual(MockTraversal.call_count, 1)
        MockTraversal.assert_called_with(meta_bundle_test, Agent.__label__, definition)

    @patch('provenance.prov2neomodel.store_token_into_meta_save')
    @patch('provenance.prov2neomodel.save_new_agent')
    @patch('provenance.prov2neomodel.save_version_to_db')
    @patch('provenance.prov2neomodel.db')
    @patch('provenance.prov2neomodel.neomodel.Traversal')
    @patch('provenance.prov2neomodel.Document', autospec=True)
    @patch('provenance.prov2neomodel.Bundle')
    def test_import_graph_old_bundle_not_update(self, MockBundle, MockDocument, MockTraversal, MockDb,
                                                    MockSaveVersion, MockSaveAgent, MockStoreToken):
        document = provm.ProvDocument()
        json_data = {'document': "graph", "documentFormat": "none"}
        token = get_dummy_token()
        graph_id = "id_graph"
        meta_id = "id_meta"
        document.set_default_namespace("local part")
        document.entity('e001')
        document.bundle('e001')

        meta_bundle = Bundle()
        meta_bundle.identifier = "meta_bundle"
        MockBundle.nodes.get.return_value = meta_bundle
        agent = Agent()
        agent.identifier = token["data"]["authorityId"]
        agent.attributes = {'prov:type': 'cpm:trustedParty', 'cpm:trustedPartyUri': 'uri',
                                'cpm:trustedPartyCertificate': 'cert'}
        MockDocument.save = MagicMock()
        MockDocument.save.return_value = None
        MockDb.transaction.side_effect = None

        import_graph(document, json_data, token, meta_id, False)

        self.assertEqual(1, MockDocument.return_value.save.call_count)
        self.assertEqual(f"{token["data"]['originatorId']}_e001", MockDocument.return_value.identifier)
        self.assertEqual(json_data['document'], MockDocument.return_value.graph)
        self.assertEqual(json_data['documentFormat'], MockDocument.return_value.format)
        MockBundle.nodes.get.assert_called_with(identifier='id_meta')

        first_version_test, gen_entity_test, meta_bundle_test = MockSaveVersion.call_args.args
        assert isinstance(first_version_test, Entity)
        self.assertEqual(f"{token["data"]['originatorId']}_e001", first_version_test.identifier)
        self.assertEqual({'prov:type': 'prov:bundle', 'pav:version': 1}, first_version_test.attributes)
        assert isinstance(gen_entity_test, Entity)
        self.assertEqual('ORG_gen', gen_entity_test.identifier)
        self.assertEqual({'prov:type': 'prov:bundle'}, gen_entity_test.attributes)
        self.assertEqual(meta_bundle, meta_bundle_test)

        agent_test, meta_bundle_test = MockSaveAgent.call_args.args
        assert isinstance(agent_test, Agent)
        self.assertEqual(agent.identifier, agent_test.identifier)
        self.assertEqual(agent.attributes, agent_test.attributes)
        self.assertEqual(meta_bundle, meta_bundle_test)

        activity_test, agent_test_2, e_test, entity_test, meta_bundle_test2 = MockStoreToken.call_args.args
        assert isinstance(activity_test, Activity)
        self.assertEqual(f"{first_version_test.identifier}_tokenGeneration", activity_test.identifier)
        self.assertEqual(datetime.fromtimestamp(token["data"]['tokenTimestamp']), activity_test.start_time)
        self.assertEqual(datetime.fromtimestamp(token["data"]['tokenTimestamp']), activity_test.end_time)
        self.assertEqual({"prov:type": 'cpm:tokenGeneration'}, activity_test.attributes)
        self.assertEqual(agent_test, agent_test_2)
        assert isinstance(e_test, Entity)
        self.assertEqual(f"{entity_test.identifier}_token", e_test.identifier)
        self.assertEqual(first_version_test, entity_test)
        self.assertEqual(meta_bundle_test, meta_bundle_test2)

        definition = dict(node_class=Agent, direction=neomodel.OUTGOING,
                              relation_type="contains", model=None)
        self.assertEqual(MockTraversal.call_count, 1)
        MockTraversal.assert_called_with(meta_bundle_test, Agent.__label__, definition)


    @patch('provenance.prov2neomodel.store_token_into_meta_save')
    @patch('provenance.prov2neomodel.save_new_agent')
    @patch('provenance.prov2neomodel.update_meta_prov_save')
    @patch('provenance.prov2neomodel.db')
    @patch('provenance.prov2neomodel.neomodel.Traversal')
    @patch('provenance.prov2neomodel.Document', autospec=True)
    @patch('provenance.prov2neomodel.Bundle')
    @patch('provenance.prov2neomodel.Entity.nodes')
    def test_import_graph_update(self, MockEntityNodes, MockBundle, MockDocument, MockTraversal, MockDb,
                                                MockUpdateMetaProvSave, MockSaveAgent, MockStoreToken):
        document = provm.ProvDocument()
        json_data = {'document': "graph", "documentFormat": "none"}
        token = get_dummy_token()
        graph_id = "id_graph"
        meta_id = "id_meta"
        document.set_default_namespace("local part")
        document.entity('e001')
        document.bundle('e001')

        meta_bundle = Bundle()
        meta_bundle.identifier = "meta_bundle"
        MockBundle.nodes.get.return_value = meta_bundle
        agent = Agent()
        agent.identifier = token["data"]["authorityId"]
        agent.attributes = {'prov:type': 'cpm:trustedParty', 'cpm:trustedPartyUri': 'uri',
                            'cpm:trustedPartyCertificate': 'cert'}
        MockDocument.save = MagicMock()
        MockDocument.save.return_value = None
        MockDb.transaction.side_effect = None

        first_entity = Entity()
        first_entity.identifier = "first"
        last_entity = Entity()
        last_entity.identifier = "last"
        gen_entity = Entity()
        gen_entity.identifier = "gen_entity"
        mock_first = MagicMock()
        mock_first.first.return_value = gen_entity
        meta_bundle.contains.filter = MagicMock(return_value = mock_first)
        latest_version = 2
        last_entity.attributes = {str(PAV_VERSION): latest_version, PROV_TYPE: PROV_BUNDLE}
        first_entity.attributes = {str(PAV_VERSION): 1}
        mock_all = MagicMock()
        mock_all.all.return_value = [last_entity, first_entity]
        MockTraversal.return_value = mock_all
        mocked_entity = MagicMock()
        mocked_entity.specialization_of.all.return_value = iter([gen_entity])
        MockEntityNodes.get = MagicMock()
        MockEntityNodes.get.return_value = mocked_entity

        res = import_graph(document, json_data, token, meta_id, "first", True)

        self.assertEqual(1, MockDocument.return_value.save.call_count)
        self.assertEqual(f"{token["data"]['originatorId']}_e001", MockDocument.return_value.identifier)
        self.assertEqual(json_data['document'], MockDocument.return_value.graph)
        self.assertEqual(json_data['documentFormat'], MockDocument.return_value.format)
        MockBundle.nodes.get.assert_called_with(identifier='id_meta')

        gen_entity_test, latest_entity_test, meta_bundle_test, new_version_test = MockUpdateMetaProvSave.call_args.args
        assert isinstance(latest_entity_test, Entity)
        self.assertEqual(last_entity, latest_entity_test)
        assert isinstance(gen_entity_test, Entity)
        self.assertEqual(gen_entity, gen_entity_test)
        self.assertEqual(meta_bundle, meta_bundle_test)
        #test new version
        assert isinstance(new_version_test, Entity)
        self.assertEqual(f"{token["data"]['originatorId']}_e001", new_version_test.identifier)
        self.assertEqual({"pav:version": latest_version + 1, PROV_TYPE: PROV_BUNDLE}, new_version_test.attributes)

        agent_test, meta_bundle_test = MockSaveAgent.call_args.args
        assert isinstance(agent_test, Agent)
        self.assertEqual(agent.identifier, agent_test.identifier)
        self.assertEqual(agent.attributes, agent_test.attributes)
        self.assertEqual(meta_bundle, meta_bundle_test)

        activity_test, agent_test_2, e_test, entity_test, meta_bundle_test2 = MockStoreToken.call_args.args
        assert isinstance(activity_test, Activity)
        self.assertEqual(f"{new_version_test.identifier}_tokenGeneration", activity_test.identifier)
        self.assertEqual(datetime.fromtimestamp(token["data"]['tokenTimestamp']), activity_test.start_time)
        self.assertEqual(datetime.fromtimestamp(token["data"]['tokenTimestamp']), activity_test.end_time)
        self.assertEqual({"prov:type": 'cpm:tokenGeneration'}, activity_test.attributes)
        self.assertEqual(agent_test, agent_test_2)
        assert isinstance(e_test, Entity)
        self.assertEqual(f"{entity_test.identifier}_token", e_test.identifier)
        self.assertEqual(new_version_test, entity_test)
        self.assertEqual(meta_bundle_test, meta_bundle_test2)

        definition = dict(node_class=Agent, direction=neomodel.OUTGOING,
                          relation_type="contains", model=None)
        self.assertEqual(2, MockTraversal.call_count)
        MockTraversal.assert_any_call(meta_bundle_test, Agent.__label__, definition)
        MockTraversal.assert_any_call(gen_entity, Entity.__label__, definition={
            "node_class": Entity,
            "direction": neomodel.INCOMING,
            "relation_type": "specialization_of",
            "model": None,
        })

def get_dummy_token():
    return {"data": {
        "originatorId": "ORG",
        "authorityId": "TrustedParty",
        "tokenTimestamp": 0,
        "documentCreationTimestamp": 0,
        "documentDigest": "17fd7484d7cac628cfa43c348fe05a009a81d18c8a778e6488b707954addf2a3",
        "additionalData": {
            "bundle": "http://prov-provider",
            "hashFunction": "SHA256",
            'trustedPartyUri': "uri",
            "trustedPartyCertificate": "cert"
        }
    },
        "signature": "bdysXEy2/sOSTN+Lh+v3x7cTdocMcndwuW5OT2wHpQOU/LM4os9Bow0sn4HTln9hRqFdCMukV6Cr6Nn8XvD96jlgEw9KqJj9I+cfBL81x9iqUJX/Wder3lkuIZXYUSeGsOOqUPdlqJAhapgr0V+vibAvPGoiRKqulNi/Xn0jn21lln1HEbHPsnOtM5Ca5wwXuTITJsiXCj+04y9V/XM9Uy9Ib4LLA1VYLCdifjg0ZuxJBcpS/HszlwW9B29rrkUGUsSrV9YU0ViYkeIMcS2bMXsur3EHi3/zSZ5IepUNOBDTu3BDUr33dbrgMOVraI8RU5DTZKmUOx8hzgtApZNotg=="
    }

if __name__ == '__main__':
    unittest.main()
