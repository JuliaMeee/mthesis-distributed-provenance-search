from unittest import TestCase
from unittest.mock import MagicMock, call

from prov.constants import PROV_TYPE, PROV_ENTITY
from prov.identifier import QualifiedName, Namespace
from prov.model import ProvBundle, ProvDocument, ProvEntity, ProvInfluence

from provenance.CPM_helpers import get_prov_generations_usages_attributions_agents, get_backbone_and_domain, has_any_cpm_type, \
    contains_non_backbone_attribute, relation_belongs_to_bb
from provenance.constants import CPM_SENDER_AGENT, CPM_RECEIVER_AGENT, CPM_BACKWARD_CONNECTOR, CPM_FORWARD_CONNECTOR, \
    CPM_MAIN_ACTIVITY, CPM_ID, DCT_HAS_PART


def create_basic_bundle():
    bundle = ProvBundle(identifier=QualifiedName(Namespace("ns", "http://inind"), "test_bundle_id"))
    bundle.add_namespace(
        "my_ns", "http://this_storage:80/api/v1/documens/"
    )
    return bundle


class MyTestCase(TestCase):

    def test_get_prov_generations_usages_attributions_agents_returns_expected_values(self):
        bundle = create_basic_bundle()
        connector = bundle.entity(identifier="my_ns:id", other_attributes={})
        ent = bundle.entity(identifier="my_ns:ent_id", other_attributes={})
        sender_agent = bundle.agent(identifier="my_ns:id2", other_attributes={PROV_TYPE: CPM_SENDER_AGENT})
        sender_agent2 = bundle.agent(identifier="my_ns:id3", other_attributes={PROV_TYPE: CPM_SENDER_AGENT})
        receiver_agent = bundle.agent(identifier="my_ns:id4", other_attributes={PROV_TYPE: CPM_RECEIVER_AGENT})
        activity = bundle.activity(identifier="my_ns:act")
        activity2 = bundle.activity(identifier="my_ns:act2")
        geenration = bundle.generation(connector, activity)
        usage = bundle.usage(activity2, connector)
        attribution = bundle.attribution(connector, sender_agent)
        attribution2 = bundle.attribution(ent, receiver_agent)
        specialization = bundle.specialization(sender_agent, sender_agent2)

        generations, usages, attributions, sender_agents, receiver_agents, specializations, derivations, all_relations = get_prov_generations_usages_attributions_agents(
            bundle)
        self.assertEqual([geenration], generations)
        self.assertEqual([usage], usages)
        self.assertEqual([attribution, attribution2], attributions)
        self.assertEqual([sender_agent, sender_agent2], sender_agents)
        self.assertEqual([receiver_agent], receiver_agents)
        self.assertEqual([geenration, usage, attribution, attribution2, specialization], all_relations)
        self.assertEqual([specialization], specializations)

    def test_get_backbone_and_domain_uses_provided_strategy_returns_backbone_and_domain(self):
        document = ProvDocument()
        bundle = document.bundle(identifier=QualifiedName(Namespace("ns", "http://inind"), "test_bundle_id"))
        bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        connector = bundle.entity(identifier="my_ns:id1", other_attributes={})
        agent = bundle.agent(identifier="my_ns:id2", other_attributes={})
        activity = bundle.activity(identifier="my_ns:id3")
        connector2 = bundle.entity(identifier="my_ns:id4", other_attributes={})
        agent2 = bundle.agent(identifier="my_ns:id5", other_attributes={})
        activity2 = bundle.activity(identifier="my_ns:id6")

        mock_backbone_strategy = MagicMock()
        # define return values - this order because of order after deserialization by prov library (first are entities...)
        mock_backbone_strategy.is_backbone_element.side_effect = [True, False, True, False, True, False]

        # add relations
        usage = bundle.usage(activity2, connector)
        usage2 = bundle.usage(activity, connector)
        attribution = bundle.attribution(connector, agent)
        attribution2 = bundle.attribution(connector2, agent2)

        graph = document.serialize(format="json")

        bundle_returned, document_returned, records_bb, records_ds = get_backbone_and_domain(graph, "json",
                                                                                             mock_backbone_strategy)

        self.assertEqual(bundle, bundle_returned)
        self.assertEqual(document, document_returned)
        self.assertTrue(connector in records_bb and connector not in records_ds)
        self.assertTrue(agent in records_bb and agent not in records_ds)
        self.assertTrue(activity in records_bb and activity not in records_ds)
        self.assertTrue(connector2 in records_ds and connector2 not in records_bb)
        self.assertTrue(agent2 in records_ds and agent2 not in records_bb)
        self.assertTrue(activity2 in records_ds and activity2 not in records_bb)
        self.assertTrue(usage in records_ds and usage not in records_bb)
        self.assertTrue(usage2 in records_bb and usage2 not in records_ds)
        self.assertTrue(attribution in records_bb and attribution not in records_ds)
        self.assertTrue(attribution2 in records_ds and attribution2 not in records_bb)

        calls = [call.is_backbone_element(connector, bundle), call.is_backbone_element(connector2, bundle),
                 call.is_backbone_element(agent, bundle), call.is_backbone_element(agent2, bundle),
                 call.is_backbone_element(activity, bundle), call.is_backbone_element(activity2, bundle)]
        mock_backbone_strategy.assert_has_calls(calls)

    def test_has_any_cpm_type_does_not_have_cpm_type_returns_false(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={})

        self.assertFalse(has_any_cpm_type(entity))

    def test_has_any_cpm_type_true_CPM_BACKWARD_CONNECTOR_returns_true(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: CPM_BACKWARD_CONNECTOR})

        self.assertTrue(has_any_cpm_type(entity))

    def test_has_any_cpm_type_true_CPM_FORWARD_CONNECTOR_returns_true(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: CPM_FORWARD_CONNECTOR})

        self.assertTrue(has_any_cpm_type(entity))

    def test_has_any_cpm_type_true_CPM_MAIN_ACTIVITY_returns_true(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: CPM_MAIN_ACTIVITY})

        self.assertTrue(has_any_cpm_type(entity))

    def test_has_any_cpm_type_true_CPM_RECEIVER_AGENT_returns_true(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: CPM_RECEIVER_AGENT})

        self.assertTrue(has_any_cpm_type(entity))

    def test_has_any_cpm_type_true_CPM_SENDER_AGENT_returns_true(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: CPM_SENDER_AGENT})

        self.assertTrue(has_any_cpm_type(entity))

    def test_has_any_cpm_type_true_CPM_ID_returns_true(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: CPM_ID})

        self.assertTrue(has_any_cpm_type(entity))

    def test_contains_non_backbone_attribute_true_returns_true(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={"my_ns:my_attrribute": "9"})

        self.assertTrue(contains_non_backbone_attribute(entity))

    def test_contains_non_backbone_attribute_true_2_returns_true(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: CPM_FORWARD_CONNECTOR, "my_ns:my_attrribute": "9"})

        self.assertTrue(contains_non_backbone_attribute(entity))

    def test_contains_non_backbone_attribute_false_returns_false(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: CPM_FORWARD_CONNECTOR})

        self.assertFalse(contains_non_backbone_attribute(entity))

    def test_contains_non_backbone_attribute_true_3_returns_true(self):
        bundle = ProvBundle(identifier=QualifiedName(Namespace("ns", "http://inind"), "test_bundle_id"))
        ns = bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: ns["my_type"]})

        self.assertTrue(contains_non_backbone_attribute(entity))

    def test_contains_non_backbone_attribute_false_2_returns_false(self):
        bundle = create_basic_bundle()
        entity = bundle.entity(identifier="my_ns:id1", other_attributes={PROV_TYPE: CPM_MAIN_ACTIVITY, DCT_HAS_PART: "my_ns:activity"})

        self.assertFalse(contains_non_backbone_attribute(entity))

    def test_relation_belongs_to_bb_true_generation_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.activity(identifier="my_ns:ent_id", other_attributes={})
        generation = bundle.generation(record1, record2)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], generation))

    def test_relation_belongs_to_bb_true_usage_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.activity(identifier="my_ns:ent_id", other_attributes={})
        usage = bundle.usage(record2, record1)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], usage))

    def test_relation_belongs_to_bb_true_influence_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.activity(identifier="my_ns:ent_id", other_attributes={})
        influence = bundle.influence(record2, record1)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], influence))

    def test_relation_belongs_to_bb_true_communication_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.activity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.activity(identifier="my_ns:ent_id", other_attributes={})
        communication = bundle.communication(record2, record1)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], communication))

    def test_relation_belongs_to_bb_true_alternate_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.entity(identifier="my_ns:ent_id", other_attributes={})
        alternate = bundle.alternate(record2, record1)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], alternate))

    def test_relation_belongs_to_bb_true_start_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.activity(identifier="my_ns:ent_id", other_attributes={})
        start = bundle.start(record2, record1)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], start))

    def test_relation_belongs_to_bb_true_end_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.activity(identifier="my_ns:ent_id", other_attributes={})
        end = bundle.end(record2, record1)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], end))

    def test_relation_belongs_to_bb_true_invalidation_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.activity(identifier="my_ns:ent_id", other_attributes={})
        invalidaiton = bundle.invalidation(record1, record2)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], invalidaiton))

    def test_relation_belongs_to_bb_true_attribution_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.agent(identifier="my_ns:ent_id", other_attributes={})
        attribution = bundle.attribution(record1, record2)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], attribution))

    def test_relation_belongs_to_bb_true_association_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.activity(identifier="my_ns:ent_id", other_attributes={})
        association = bundle.association(record1, record2)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], association))

    def test_relation_belongs_to_bb_true_delegation_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.agent(identifier="my_ns:id", other_attributes={})
        record2 = bundle.agent(identifier="my_ns:ent_id", other_attributes={})
        delegation = bundle.delegation(record1, record2)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], delegation))

    def test_relation_belongs_to_bb_true_specialization_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.entity(identifier="my_ns:ent_id", other_attributes={})
        specialization = bundle.specialization(record1, record2)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], specialization))

    def test_relation_belongs_to_bb_true_mention_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.entity(identifier="my_ns:ent_id", other_attributes={})
        mention = bundle.mention(record1, record2, bundle.identifier)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], mention))

    def test_relation_belongs_to_bb_true_membership_returns_true(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.entity(identifier="my_ns:ent_id", other_attributes={})
        membership = bundle.membership(record1, record2)

        self.assertTrue(relation_belongs_to_bb([record1.identifier, record2.identifier], membership))

    def test_relation_belongs_to_bb_false_returns_false(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.entity(identifier="my_ns:ent_id", other_attributes={})
        record3 = bundle.entity(identifier="my_ns:ent_id2", other_attributes={})
        membership = bundle.membership(record1, record2)

        self.assertFalse(relation_belongs_to_bb([record1.identifier, record3.identifier], membership))

    def test_relation_belongs_to_bb_false_2_returns_false(self):
        bundle = create_basic_bundle()
        record1 = bundle.entity(identifier="my_ns:id", other_attributes={})
        record2 = bundle.entity(identifier="my_ns:ent_id", other_attributes={})
        membership = bundle.membership(record1, record2)

        self.assertFalse(relation_belongs_to_bb([record1.identifier], membership))

