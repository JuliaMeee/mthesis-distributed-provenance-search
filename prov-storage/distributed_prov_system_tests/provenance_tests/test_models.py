import time
import unittest
from datetime import datetime
from provenance.models import *

from time import sleep
from subprocess import call
from neomodel import db


class MyTestCase(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        success = call("docker compose up -d", shell=True)
        sleep(10)
        # Need to sleep to wait for the test instance to completely come up
        assert (success == 0)
        # Delete all previous entries in the db prior to running tests
        query = "match (n)-[r]-() delete n,r"
        db.cypher_query(query)

    @classmethod
    def tearDownClass(cls):
        # Delete all previous entries in the db after running tests
        query = "match (n)-[r]-() delete n,r"
        db.cypher_query(query)
        query = "match (n) delete n"
        db.cypher_query(query)
        sleep(1)
        # Shut down test neo4j instance
        success = call("docker compose down", shell=True)
        assert (success == 0)

    # delete whats in database after each test
    def tearDown(self):
        query = "match (a) -[r] -> () delete a, r"
        db.cypher_query(query)
        query = "match (n) delete n"
        db.cypher_query(query)


    def test_BaseProvRelModel(self):
        rel = BaseProvRel()
        rel.identifier = "identifier"
        attributes = {"name": "John", "age": 30}
        rel.attributes = attributes
        self.assertEqual(rel.attributes, {"name": "John", "age": 30})
        self.assertEqual(rel.identifier, "identifier")

    def test_ModelsWithTime(self):
        rel = WasGeneratedBy()
        rel2 = Used()
        rel3 = WasInvalidatedBy()
        time = datetime.now()
        rel.time = time
        rel2.time = time
        rel3.time = time
        self.assertEqual(rel.time, time)
        self.assertEqual(rel2.time, time)
        self.assertEqual(rel3.time, time)

    def test_WasDerivedFromModel(self):
        rel = WasDerivedFrom()
        rel.activity = "activity"
        rel.generation = "generation"
        rel.usage = "usage"
        self.assertEqual(rel.activity, "activity")
        self.assertEqual(rel.generation, "generation")
        self.assertEqual(rel.usage, "usage")

    def test_WasStartedByModel(self):
        rel = WasStartedBy()
        rel.starter = "starter"
        time = datetime.now()
        rel.time = time
        self.assertEqual(rel.starter, "starter")
        self.assertEqual(rel.time, time)

    def test_WasEndedByModel(self):
        rel = WasEndedBy()
        rel.ender = "ender"
        time = datetime.now()
        rel.time = time
        self.assertEqual(rel.ender, "ender")
        self.assertEqual(rel.time, time)

    def test_WasAssociatedWithModel(self):
        rel = WasAssociatedWith()
        rel.plan = "plan"
        self.assertEqual(rel.plan, "plan")

    def test_ActedOnBehalfOfModel(self):
        rel = ActedOnBehalfOf()
        rel.activity = "activity"
        self.assertEqual(rel.activity, "activity")

    ### Classes for main PROV-DM types ###
    def test_BaseProvClassModel(self):
        prov_class = BaseProvClass()
        prov_class.identifier = "identifier"
        prov_class2 = BaseProvClass()
        prov_class2.identifier = "identifier2"
        bundle = Bundle(identifier='Bundle1')
        bundle.save()
        prov_class.attributes = {"name": "John", "age": 30}
        prov_class.save()
        prov_class2.save()

        bundle.contains.connect(prov_class)
        rel2 = prov_class.was_influenced_by.connect(prov_class2, {'identifier': "id"}).save()

        self.assertEqual(prov_class.identifier, "identifier")
        self.assertEqual(prov_class.attributes, {"name": "John", "age": 30})
        self.assertIsNotNone(prov_class.was_influenced_by)
        self.assertIsNotNone(prov_class.contains)
        self.assertEqual(rel2.identifier, prov_class.was_influenced_by.relationship(prov_class2).identifier)
        self.assertEqual(prov_class, bundle.contains[0])

    def test_EntityModel(self):
        prov_class = Entity()
        prov_class.identifier = "identifier"
        prov_class.attributes = {"name": "John", "age": 30}
        prov_class.save()
        generation_activity = Activity()
        generation_activity.identifier = "id1"
        generation_activity.save()
        generated_by_fake_act = FakeActivity()
        generated_by_fake_act.save()
        derived_from_entity = Entity()
        derived_from_entity.identifier = "id2"
        derived_from_entity.save()
        invalidation_activity = Activity()
        invalidation_activity.identifier = "id3"
        invalidation_activity.save()
        fake_invalidation_activity = FakeActivity()
        fake_invalidation_activity.save()
        agent = Agent()
        agent.identifier = "id4"
        agent.save()
        specialization_of_entity = Entity()
        specialization_of_entity.identifier = "id5"
        specialization_of_entity.save()
        alternate_of_entity = Entity()
        alternate_of_entity.identifier = "id6"
        alternate_of_entity.save()
        had_member = Entity()
        had_member.identifier = "id7"
        had_member.save()
        used_by_activity = Activity()
        used_by_activity.identifier = "id8"
        used_by_activity.save()

        rel = prov_class.was_generated_by.connect(generation_activity, {'identifier': "id1"}).save()
        rel2 = prov_class.was_generated_by_fake.connect(generated_by_fake_act, {'identifier': "id2"}).save()
        rel3 = prov_class.was_derived_from.connect(derived_from_entity,
                                                   {'activity': "act", "generation": "gen", "usage": "usg"}).save()
        rel4 = prov_class.was_invalidated_by.connect(invalidation_activity, {'identifier': "id3"}).save()
        rel5 = prov_class.was_invalidated_by_fake.connect(fake_invalidation_activity, {'identifier': "id4"}).save()
        rel6 = prov_class.was_attributed_to.connect(agent, {'identifier': "id5"}).save()
        prov_class.specialization_of.connect(specialization_of_entity)
        prov_class.alternate_of.connect(alternate_of_entity)
        prov_class.had_member.connect(had_member)
        rel7 = prov_class.used.connect(used_by_activity).save()

        self.assertEqual(prov_class.identifier, "identifier")
        self.assertEqual(prov_class.attributes, {"name": "John", "age": 30})
        self.assertEqual(rel.identifier, prov_class.was_generated_by.relationship(generation_activity).identifier)
        self.assertEqual(generation_activity, prov_class.was_generated_by[0])
        self.assertEqual(generated_by_fake_act, prov_class.was_generated_by_fake[0])
        self.assertEqual(rel2.identifier,
                         prov_class.was_generated_by_fake.relationship(generated_by_fake_act).identifier)
        self.assertEqual(rel3.activity, prov_class.was_derived_from.relationship(derived_from_entity).activity)
        self.assertEqual(rel3.generation, prov_class.was_derived_from.relationship(derived_from_entity).generation)
        self.assertEqual(rel3.usage, prov_class.was_derived_from.relationship(derived_from_entity).usage)
        self.assertEqual(derived_from_entity, prov_class.was_derived_from[0])
        self.assertEqual(rel4.identifier, prov_class.was_invalidated_by.relationship(invalidation_activity).identifier)
        self.assertEqual(invalidation_activity, prov_class.was_invalidated_by[0])
        self.assertEqual(rel5.identifier,
                         prov_class.was_invalidated_by_fake.relationship(fake_invalidation_activity).identifier)
        self.assertEqual(fake_invalidation_activity, prov_class.was_invalidated_by_fake[0])
        self.assertEqual(rel6.identifier,
                         prov_class.was_attributed_to.relationship(agent).identifier)
        self.assertEqual(agent, prov_class.was_attributed_to[0])
        self.assertEqual(specialization_of_entity, prov_class.specialization_of[0])
        self.assertEqual(alternate_of_entity, prov_class.alternate_of[0])
        self.assertEqual(had_member.identifier, prov_class.had_member[0].identifier)
        self.assertEqual(rel7.identifier,
                         prov_class.used.relationship(used_by_activity).identifier)
        self.assertEqual(used_by_activity, prov_class.used[0])


    def test_ActivityModel(self):
        activity = Activity()
        time_now = time.time()
        activity.start_time = datetime.fromtimestamp(time_now)
        activity.end_time = activity.start_time
        activity.identifier = "identifier"
        activity.save()

        used_entity = Entity()
        used_entity.identifier = "id1"
        used_entity.save()
        used_fake_entity = FakeEntity()
        used_fake_entity.save()
        was_informed_by_activity = Activity()
        was_informed_by_activity.identifier = "id2"
        was_informed_by_activity.save()
        was_associated_with_agent = Agent()
        was_associated_with_agent.identifier = "id3"
        was_associated_with_agent.save()
        was_associated_with_fake_agent = FakeAgent()
        was_associated_with_fake_agent.save()
        was_started_by_entity = Entity()
        was_started_by_entity.identifier = "id4"
        was_started_by_entity.save()
        was_started_by_fake_entity = FakeEntity()
        was_started_by_fake_entity.save()
        was_ended_by_entity = Entity()
        was_ended_by_entity.identifier = "id5"
        was_ended_by_entity.save()
        was_ended_by_fake_entity = FakeEntity()
        was_ended_by_fake_entity.save()
        was_generated_by_entity = Entity()
        was_generated_by_entity.identifier = "id6"
        was_generated_by_entity.save()

        rel = activity.used.connect(used_entity, {'identifier': "id1"}).save()
        rel2 = activity.used_fake.connect(used_fake_entity, {'identifier': "id2"}).save()
        rel3 = activity.was_informed_by.connect(was_informed_by_activity, {'identifier': "id3"}).save()
        rel4 = activity.was_associated_with.connect(was_associated_with_agent, {'identifier': "id4"}).save()
        rel5 = activity.was_associated_with_fake.connect(was_associated_with_fake_agent, {'identifier': "id5"}).save()
        rel6 = activity.was_started_by.connect(was_started_by_entity, {'identifier': "id6"}).save()
        rel7 = activity.was_started_by_fake.connect(was_started_by_fake_entity, {'identifier': "id7"}).save()
        rel8 = activity.was_ended_by.connect(was_ended_by_entity, {'identifier': "id8"}).save()
        rel9 = activity.was_ended_by_fake.connect(was_ended_by_fake_entity, {'identifier': "id9"}).save()
        rel10 = activity.was_generated_by.connect(was_generated_by_entity, {'identifier': "id10"}).save()

        self.assertEqual(datetime.fromtimestamp(time_now), activity.start_time)
        self.assertEqual(datetime.fromtimestamp(time_now), activity.end_time)
        self.assertEqual(rel.identifier, activity.used.relationship(used_entity).identifier)
        self.assertEqual(used_entity, activity.used[0])
        self.assertEqual(rel2.identifier, activity.used_fake.relationship(used_fake_entity).identifier)
        self.assertEqual(used_fake_entity, activity.used_fake[0])
        self.assertEqual(rel3.identifier, activity.was_informed_by.relationship(was_informed_by_activity).identifier)
        self.assertEqual(was_informed_by_activity, activity.was_informed_by[0])
        self.assertEqual(rel4.identifier, activity.was_associated_with.relationship(was_associated_with_agent).identifier)
        self.assertEqual(was_associated_with_agent, activity.was_associated_with[0])
        self.assertEqual(rel5.identifier, activity.was_associated_with_fake.relationship(was_associated_with_fake_agent).identifier)
        self.assertEqual(was_associated_with_fake_agent, activity.was_associated_with_fake[0])
        self.assertEqual(rel6.identifier, activity.was_started_by.relationship(was_started_by_entity).identifier)
        self.assertEqual(was_started_by_entity, activity.was_started_by[0])
        self.assertEqual(rel7.identifier, activity.was_started_by_fake.relationship(was_started_by_fake_entity).identifier)
        self.assertEqual(was_started_by_fake_entity, activity.was_started_by_fake[0])
        self.assertEqual(rel8.identifier, activity.was_ended_by.relationship(was_ended_by_entity).identifier)
        self.assertEqual(was_ended_by_entity, activity.was_ended_by[0])
        self.assertEqual(rel9.identifier, activity.was_ended_by_fake.relationship(was_ended_by_fake_entity).identifier)
        self.assertEqual(was_ended_by_fake_entity, activity.was_ended_by_fake[0])
        self.assertEqual(rel10.identifier, activity.was_generated_by.relationship(was_generated_by_entity).identifier)
        self.assertEqual(was_generated_by_entity, activity.was_generated_by[0])


    def test_AgentModel(self):
        agent1 = Agent()
        agent1.identifier = "identifier1"
        agent1.save()
        agent2 = Agent()
        agent2.identifier = "identifier2"
        agent2.save()

        rel = agent1.acted_on_behalf_of.connect(agent2, {'identifier': "id1"}).save()

        self.assertEqual("identifier1", agent1.identifier)
        self.assertEqual(rel.identifier, agent1.acted_on_behalf_of.relationship(agent2).identifier)
        self.assertEqual(agent2, agent1.acted_on_behalf_of[0])


    def test_BundleModel(self):
        bundle = Bundle()
        bundle.identifier = "identifier1"
        bundle.save()
        base_class = BaseProvClass()
        base_class.identifier = "identifier2"
        base_class.save()

        bundle.contains.connect(base_class)

        self.assertEqual("identifier1", bundle.identifier)
        self.assertEqual(base_class, bundle.contains[0])


    def test_ForwardConnectorModel(self):

        self.assertTrue(issubclass(BackwardConnector, Entity))

    def test_BackwardConnectorModel(self):

        self.assertTrue(issubclass(BackwardConnector, Entity))

    ### NON-PROV Models ###
    def test_DocumentModel(self):
        rel = Document()
        rel.identifier = "identifier"
        rel.graph = "graph"
        rel.format = "format"
        self.assertEqual(rel.identifier, "identifier")
        self.assertEqual(rel.graph, "graph")
        self.assertEqual(rel.format, "format")
        self.assertIsNotNone(rel.belongs_to)

    def test_TokenModel(self):
        rel = Token()
        rel.signature = "signature"
        rel.hash = "hash"
        rel.originator_id = "originator_id"
        rel.authority_id = "authority_id"
        rel.token_timestamp = 9
        rel.message_timestamp = 9
        rel.additional_data = {"name": "John", "age": 30}
        self.assertEqual(rel.signature, "signature")
        self.assertEqual(rel.hash, "hash")
        self.assertEqual(rel.originator_id, "originator_id")
        self.assertEqual(rel.authority_id, "authority_id")
        self.assertEqual(rel.token_timestamp, 9)
        self.assertEqual(rel.message_timestamp, 9)
        self.assertEqual(rel.additional_data, {"name": "John", "age": 30})
        self.assertIsNotNone(rel.belongs_to)
        self.assertIsNotNone(rel.was_issued_by)

    def test_OrganizationModel(self):
        rel = Organization()
        rel.identifier = "identifier"
        rel.client_cert = "client_cert"
        rel.intermediate_certs = ["1", "2"]
        self.assertEqual(rel.identifier, "identifier")
        self.assertEqual(rel.client_cert, "client_cert")
        self.assertEqual(rel.intermediate_certs, ["1", "2"])
        self.assertIsNotNone(rel.trusts)

    def test_TrustedPartyModel(self):
        rel = TrustedParty()
        rel.identifier = "identifier"
        rel.certificate = "certificate"
        rel.url = "url"
        self.assertEqual(rel.identifier, "identifier")
        self.assertEqual(rel.certificate, "certificate")
        self.assertEqual(rel.url, "url")
        self.assertEqual(rel.checked, False)
        self.assertEqual(rel.valid, False)
        self.assertIsNotNone(rel.trusts)
        self.assertIsNotNone(rel.was_issued_by)


if __name__ == '__main__':
    unittest.main()
