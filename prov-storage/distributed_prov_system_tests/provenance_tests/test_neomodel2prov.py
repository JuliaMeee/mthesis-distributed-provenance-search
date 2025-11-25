import unittest
from datetime import datetime

from unittest import TestCase

from prov.constants import PROV_GENERATION, PROV_DERIVATION, PROV_SPECIALIZATION

from provenance.neomodel2prov import *
from provenance.models import Bundle, Entity, Activity, Agent

from unittest.mock import MagicMock, call
from unittest.mock import patch


class MyTestCase(TestCase):
    used_all_call_count = 0

    def generate_gen_entities(self, count):
        return [Entity(identifier=f"ORG_entity_id_{x}_gen") for x in range(count)]

    def generate_entities(self, count):
        return [Entity(identifier=f"ORG_entity_id_{x}", attributes={'pav:version': x}) for x in range(count)]

    @patch('provenance.neomodel2prov.add_version_chain_to_bundle')
    @patch('provenance.neomodel2prov.neomodel.Traversal.all')
    def test_convert_meta_to_prov_ok_returns_document(self, MockTraversal, MockAddVersionChainToBundle):
        neo_bundle = Bundle()
        neo_bundle.identifier = "neo_bundle"
        generated_entiries = self.generate_gen_entities(5) + self.generate_entities(5)
        MockTraversal.return_value = generated_entiries

        document = convert_meta_to_prov(neo_bundle)

        self.assertEqual(list(document.namespaces)[0],
                         Namespace("meta", DEFAULT_NAMESPACE + f"/api/v1/documents/meta/"))
        self.assertEqual(MockAddVersionChainToBundle.call_count, 5)
        bundle = list(document.bundles)[0]
        id = QualifiedName(Namespace("meta", DEFAULT_NAMESPACE + f"/api/v1/documents/meta/"),
                           neo_bundle.identifier)
        self.assertEqual(id, bundle.identifier)
        self.assertEqual(Namespace("meta", DEFAULT_NAMESPACE + f"/api/v1/documents/meta/"),
                         next(iter(document.namespaces)))
        calls = [call(bundle, generated_entiries[0]), call(bundle, generated_entiries[1]),
                 call(bundle, generated_entiries[2]), call(bundle, generated_entiries[3]),
                 call(bundle, generated_entiries[4])]
        MockAddVersionChainToBundle.assert_has_calls(calls)

    def test_convert_tuple_list_to_dict_returns_dict_with_right_content(self):
        attrs_to_convert = {"prov:local1": "b", "prov:local2": "d", "prov:local3": "f"}
        element = Entity()
        element.identifier = "id"
        element.attributes = attrs_to_convert
        dict_converted = convert_attributes_to_dict(element)
        self.assertEqual(dict_converted[QualifiedName(Namespace("prov", "http://www.w3.org/ns/prov#"),
                                                      "local1")], "b")
        self.assertEqual(dict_converted[QualifiedName(Namespace("prov", "http://www.w3.org/ns/prov#"),
                                                      "local2")], "d")
        self.assertEqual(dict_converted[QualifiedName(Namespace("prov", "http://www.w3.org/ns/prov#"),
                                                      "local3")], "f")

    def side_effect_entity_used_all(self, *args, **kwargs):
        ver = self.__class__.used_all_call_count // 3  # calculate which version is this - use all is called thrice for every entity
        activity = Activity()
        activity.identifier = f"ORG_entity_id_{ver}_tokenGeneration"
        activity.attributes = {'prov:version': ver}
        activity.start_time = datetime(2024, 12, 5, 10, 0, 5, ver)
        activity.end_time = activity.start_time
        activity.was_associated_with = MagicMock()
        agent = Agent(identifier="TrustedParty")
        agent.attributes = {'meta:version': 0}
        activity.was_associated_with.all.return_value = [agent]
        token = Entity(
            identifier=f"ORG_entity_id_{ver}_token")  # checked - fix in neomodel2prov - now creating qualified_name from this id
        token.attributes = {'prov:version': ver}
        activity.was_generated_by = MagicMock()
        activity.was_generated_by.all.return_value = [token]
        self.__class__.used_all_call_count += 1  # increment call count
        return [activity]

    @patch('provenance.neomodel2prov.neomodel.Traversal.all')
    def test_add_version_chain_to_bundle_adds_info_to_bundle(self, MockTraversal):
        bundle = ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "prov"))
        bundle.set_default_namespace(DEFAULT_NAMESPACE)
        gen_entity = Entity()
        gen_entity.identifier = f"ORG_entity_gen"  # called get_entity_qualified_name
        gen_entity.attributes = {"prov:local1": "b"}  # called convert to dict
        generated_entities = self.generate_entities(5)
        for entity in generated_entities:
            entity.used = MagicMock()
            entity.used.all.side_effect = self.side_effect_entity_used_all
        MockTraversal.side_effect = [generated_entities, iter([generated_entities[1]]),
                                     iter([generated_entities[2]]), iter([generated_entities[3]]),
                                     iter([generated_entities[
                                               4]]),
                                     iter([])]  # called get_sorted_specialized_entities_from_gen - mock traversal

        add_version_chain_to_bundle(bundle, gen_entity)

        # asserts
        # assert that org namespace is created for generated entiries
        org_namespace = NAMESPACES["ORG"]
        self.assertIsNotNone(org_namespace)

        # general entity assert
        print(len(bundle.get_records()))
        self.assertEqual(len(bundle.get_records()), 46)

        gen_prov_entity_list = bundle.get_record(QualifiedName(NAMESPACES["ORG"], "entity" + "_gen"))
        self.assertEqual(1, len(gen_prov_entity_list))
        gen_prov_entity = gen_prov_entity_list[0]
        self.assertEqual(1, len(gen_prov_entity.attributes))
        self.assertEqual(list(convert_attributes_to_dict(gen_entity).items())[0],
                         gen_prov_entity.attributes[0])

        # generated entities assrts
        for x in range(5):
            prov_entity_list = bundle.get_record(QualifiedName(NAMESPACES["ORG"], f"entity_id_{x}"))
            self.assertEqual(1, len(prov_entity_list))
            prov_entity = prov_entity_list[0]
            self.assertEqual(1, len(prov_entity.attributes))
            self.assertEqual(list(convert_attributes_to_dict(generated_entities[x]).items())[0],
                             prov_entity.attributes[0])

        # tokens asserts
        for x in range(5):
            self.__class__.used_all_call_count = x * 3  # for getting mocked values in this test - used in side_effect_entity_used_all
            # tokens id - not as qualified name - does not matter - not in standard
            prov_token_list = bundle.get_record(f"ORG_entity_id_{x}_token")
            self.assertEqual(1, len(prov_token_list))
            prov_token = prov_token_list[0]
            self.assertEqual(1, len(prov_token.attributes))
            self.assertEqual(
                list(convert_attributes_to_dict(generated_entities[x].used.all()[0].was_generated_by.all()[0]).items())[
                    0],
                prov_token.attributes[0])

        # token generation activity asserts
        for x in range(5):
            self.__class__.used_all_call_count = x * 3  # for getting mocked values in this test
            # - used in side_effect_entity_used_all
            activity_list = bundle.get_record(f"entity_id_{x}_tokenGeneration")
            self.assertEqual(1, len(activity_list))
            activity = activity_list[0]
            self.assertEqual(3, len(activity.attributes))

            self.assertEqual(
                (generated_entities[x].used.all()[0]).start_time,
                activity.attributes[0][1])
            self.assertEqual(
                (generated_entities[x].used.all()[0]).end_time,
                activity.attributes[1][1])
            self.assertEqual(
                list(convert_attributes_to_dict(generated_entities[x].used.all()[0].was_generated_by.all()[0]).items())[
                    0],
                activity.attributes[2])

        # asserts for agent
        agent_list = bundle.get_record("TrustedParty")
        self.assertEqual(1, len(agent_list))
        agent = agent_list[0]
        self.assertEqual(1, len(agent.attributes))
        self.assertEqual(0, agent.attributes[0][1])
        self.assertEqual(QualifiedName(NAMESPACES["meta"], "version"), agent.attributes[0][0])

        # wasDerivedFrom asserts
        records_list = bundle.get_records()
        was_derived_from_list = []
        for x in records_list:
            if x._prov_type == PROV_DERIVATION:
                was_derived_from_list.append(x)
        for x in range(4):
            self.assertEqual((QualifiedName(NAMESPACES["prov"], "type"), "prov:revisionOf"),
                             was_derived_from_list[x].attributes[-1])
            self.assertEqual((QualifiedName(NAMESPACES["prov"], "generatedEntity"),
                              bundle.get_record(QualifiedName(NAMESPACES["ORG"], f"entity_id_{x + 1}"))[0]._identifier),
                             was_derived_from_list[x].attributes[0])
            self.assertEqual((QualifiedName(NAMESPACES["prov"], "usedEntity"),
                              bundle.get_record(QualifiedName(NAMESPACES["ORG"], f"entity_id_{x}"))[0]._identifier),
                             was_derived_from_list[x].attributes[1])

        # specializationOf asserts
        specialization_of_list = []
        for x in records_list:
            if x._prov_type == PROV_SPECIALIZATION:
                specialization_of_list.append(x)
        for x in range(5):
            self.assertEqual((QualifiedName(NAMESPACES["prov"], "specificEntity"),
                              bundle.get_record(QualifiedName(NAMESPACES["ORG"], f"entity_id_{x}"))[0]._identifier),
                             specialization_of_list[x].attributes[0])
            self.assertEqual((QualifiedName(NAMESPACES["prov"], "generalEntity"),
                              QualifiedName(NAMESPACES["ORG"], "entity_gen")),
                             specialization_of_list[x].attributes[1])

        # wasgeneratedby asserts
        was_generated_by_list = []
        for x in records_list:
            if x._prov_type == PROV_GENERATION:
                was_generated_by_list.append(x)
        for x in range(5):
            self.assertEqual((QualifiedName(NAMESPACES["prov"], "entity"),
                              bundle.get_record(f"ORG_entity_id_{x}_token")[0]._identifier),
                             was_generated_by_list[x].attributes[0])
            self.assertEqual((QualifiedName(NAMESPACES["prov"], "activity"),
                              bundle.get_record(f"entity_id_{x}_tokenGeneration")[0]._identifier),
                             was_generated_by_list[x].attributes[1])

    @patch('provenance.neomodel2prov.neomodel.Traversal.all')
    def test_add_version_chain_to_bundle_agent_in_bundle_adds_info_to_bundle(self, MockTraversal):
        bundle = ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "prov"))
        bundle.set_default_namespace(DEFAULT_NAMESPACE)
        gen_entity = Entity()
        gen_entity.identifier = f"ORG_entity_gen"  # called get_entity_qualified_name
        gen_entity.attributes = {"prov:local1": "b"}  # called convert to dict
        generated_entities = self.generate_entities(5)
        bundle.agent("TrustedParty", {"prov:created_at": "test"})
        for entity in generated_entities:
            entity.used = MagicMock()
            entity.used.all.side_effect = self.side_effect_entity_used_all
        MockTraversal.side_effect = [generated_entities, iter([generated_entities[1]]),
                                     iter([generated_entities[2]]), iter([generated_entities[3]]),
                                     iter([generated_entities[
                                               4]]),
                                     iter([])]  # called get_sorted_specialized_entities_from_gen - mock traversal

        add_version_chain_to_bundle(bundle, gen_entity)

        # asserts for agent
        agent_list = bundle.get_record("TrustedParty")
        self.assertEqual(1, len(agent_list))
        agent = agent_list[0]
        self.assertEqual(1, len(agent.attributes))
        self.assertEqual("test", agent.attributes[0][1])
        self.assertEqual(QualifiedName(namespace=NAMESPACES["prov"], localpart="created_at"),
                         agent.attributes[0][0])


if __name__ == '__main__':
    unittest.main()
