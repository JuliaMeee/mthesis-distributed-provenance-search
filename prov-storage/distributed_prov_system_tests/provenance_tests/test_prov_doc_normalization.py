from datetime import datetime
from unittest import TestCase

from prov.model import ProvDocument

import helpers
from provenance.prov_doc_validators_strategies import _is_graph_normalized


class MyTestCase(TestCase):

    # prefix for recognizing existential variables
    existential_variable_prefix = "_"
    # create class for generating existential identifiers
    existential_variable_id_generator = helpers.ExistentialVariablesGenerator(existential_variable_prefix)
    # placeholder time - use as existential variable in next tests - change to needed time
    placeholder_time = datetime.now()

    def test__is_graph_normalized_graph_ok_returns_true(self):
        #  created doc normalized - according to
        # https://www.w3.org/TR/prov-constraints/#normalization-validity-equivalence
        # next tests create not normalized docs and test for negative result

        document = helpers.create_normalized_graph(self.existential_variable_prefix)
        bundle = None
        for bndl in document.bundles:
            bundle = bndl

        self.assertTrue(_is_graph_normalized(bundle))

    # next tests failing for now - is_graph_normalized is not  implemented yet
    def test__is_graph_normalized_returns_false_def1(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')
        entity = bundle.entity('e001', [])

        bundle.alternate(entity, entity)  # Inference 16
        bundle.generation(entity, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # wasInfluencedBy, Inference 7
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            other_attributes=[])  # no id - Definition 1 nok

        self.check_is_normalized_negative(document)

    def check_is_normalized_negative(self, document):
        data = document.serialize()
        self.assertFalse(_is_graph_normalized(data))

    def test__is_graph_normalized_def2_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity('e001')  # no optional attributes list - Definition 2 nok
        bundle.alternate(entity, entity)  # Inference 16
        bundle.generation(entity, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])  # Inference 7
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # wasInfluencedBy, Inference 7
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_def3_and_4_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')
        entity = bundle.entity('e001', [])

        bundle.alternate(entity, entity)  # Inference 16
        bundle.generation(entity, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])  # Inference 7
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # wasInfluencedBy, Inference 7
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid1",
                            other_attributes=[])  # no time added to normal form - Definition 3, 4 nok

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_5_gen_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        activity2 = bundle.activity(identifier='act002', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                    endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                    other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8
        bundle.start(activity2, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact2", other_attributes=[])  # Inference 8
        bundle.end(activity2, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact2", other_attributes=[])  # Inference 8

        entity4 = bundle.entity("e004", [])
        bundle.alternate(entity4, entity4)  # Inference 16

        # Inference 5 / 6 applied
        bundle.communication(activity2, activity, "acttoact2", other_attributes=[])  # wasInformedBy
        # bundle.generation(entity4, activity, identifier="gen2", time=placeholder_time, other_attributes=[]) - generation missing - inference 5 nok
        bundle.usage(activity2, entity4, identifier="usage2", time=self.placeholder_time, other_attributes=[])
        bundle.influence(activity2, entity4, identifier="influence3",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        bundle.invalidation(entity4, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_5_usage_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')
        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        activity2 = bundle.activity(identifier='act002', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                    endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                    other_attributes=[])

        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8
        bundle.start(activity2, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact2", other_attributes=[])  # Inference 8
        bundle.end(activity2, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact2", other_attributes=[])  # Inference 8

        entity4 = bundle.entity("e004", [])
        bundle.alternate(entity4, entity4)  # Inference 16

        # Inference 5 / 6 applied
        bundle.communication(activity2, activity, "acttoact2", other_attributes=[])  # wasInformedBy
        bundle.generation(entity4, activity, identifier="gen2", time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity4, activity, identifier="influence2",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        # bundle.usage(activity2, entity4, identifier="usage2", time=placeholder_time, other_attributes=[]) - usage missing - Inference 5 nok
        bundle.invalidation(entity4, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_6_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')
        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        activity2 = bundle.activity(identifier='act002', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                    endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                    other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_activity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8
        bundle.start(activity2, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact2", other_attributes=[])  # Inference 8
        bundle.end(activity2, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact2", other_attributes=[])  # Inference 8

        entity4 = bundle.entity("e004", [])
        bundle.alternate(entity4, entity4)  # Inference 16

        # Inference 5 / 6 applied
        # bundle.communication(activity2, activity, "acttoact2", other_attributes=[])  # wasInformedBy missing - Inference 6 nok
        bundle.generation(entity4, activity, identifier="gen2", time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity4, activity, identifier="influence2",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        bundle.usage(activity2, entity4, identifier="usage2", time=self.placeholder_time, other_attributes=[])
        bundle.influence(activity2, entity4, identifier="influence3",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        bundle.invalidation(entity4, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_7_invalidation_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')
        entity4 = bundle.entity("e004", [])
        bundle.alternate(entity4, entity4)  # Inference 16

        bundle.generation(entity4, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity4, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # wasInfluencedBy, Inference 15

        # bundle.invalidation(entity4, existential_activity, time=placeholder_time, identifier="invalid4",
        #                     other_attributes=[])  - Inference 7 nok - invalidation not done

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_7_generation_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')
        entity4 = bundle.entity("e004", [])
        bundle.alternate(entity4, entity4)  # Inference 16

        # bundle.generation(entity4, existential_activity, identifier="gen2", time=placeholder_time, other_attributes=[]) - Inference 7 nok - generation not done
        bundle.influence(entity4, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        bundle.invalidation(entity4, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_8_start_missing_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        # bundle.start(activity, trigger=existential_entity, starter=existential_activity,
        #             time=datetime(2025, 1, 5, 15, 8, 24, 78915),
        #             identifier="startact", other_attributes=[])  # Inference 8 nok - start missing
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_8_end_missing_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        # bundle.end(activity, existential_entity, ender=existential_activity,
        #           time=datetime(2025, 1, 7, 15, 8, 24, 78915),
        #           identifier="endact", other_attributes=[])  # Inference 8 nok - end missing
        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_9_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity("e001", [])
        bundle.alternate(entity, entity)  # Inference 16

        bundle.generation(entity, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8

        activity2 = bundle.activity(identifier='act002', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                    endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                    other_attributes=[])
        bundle.end(activity2, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact2", other_attributes=[])  # Inference 8

        bundle.start(activity2, trigger=entity, starter=activity, time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact2", other_attributes=[])  # Inference 8, Inference 9
        # bundle.generation(entity, activity, identifier="gen0", time=placeholder_time,
        #                  other_attributes=[])  # Inference 9 nok - generation with activity missing
        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_10_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity("e001", [])
        bundle.alternate(entity, entity)  # Inference 16

        bundle.generation(entity, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8

        activity2 = bundle.activity(identifier='act002', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                    endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                    other_attributes=[])
        bundle.end(activity2, entity, ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact2", other_attributes=[])  # Inference 8, Inference 9

        bundle.start(activity2, trigger=self.existential_variable_id_generator.get_entity_id(), starter=activity,
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact2", other_attributes=[])  # Inference 8
        # bundle.generation(entity, activity, identifier="gen0", time=placeholder_time,
        #                  other_attributes=[])  # Inference 10 nok - generation with activity missing
        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_11_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity("e001", [])
        bundle.alternate(entity, entity)  # Inference 16
        entity2 = bundle.entity("e002", [])
        bundle.alternate(entity2, entity2)  # Inference 16

        bundle.generation(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.generation(entity, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        bundle.invalidation(entity2, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid5",
                            other_attributes=[])  # Inference 7

        activity2 = bundle.activity(identifier='act002', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                    endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                    other_attributes=[])
        bundle.end(activity2, entity, ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact2", other_attributes=[])  # Inference 8, Inference 9

        bundle.start(activity2, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact2", other_attributes=[])  # Inference 8

        # Inference 11
        generation = bundle.generation(entity2, activity2, identifier="gen1", time=self.placeholder_time,
                                       other_attributes=[])  # wasGeneratedBy, Inference 13
        bundle.influence(entity2, activity2, identifier="influence7",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        usage = bundle.usage(activity2, self.existential_variable_id_generator.get_entity_id(), self.placeholder_time,
                             identifier="usage1",
                             other_attributes=[])  # Inference 11 nok - existential entity used instead of entity
        bundle.influence(activity2, entity, identifier="influence8",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        bundle.derivation(entity2, entity, activity2, generation, usage, identifier="derivation0",
                          other_attributes=[])  # wasDerivedFrom, if no activity, generation and usage are non expandable
        bundle.influence(entity2, entity, identifier="influence9",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_12_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity("e001", [])
        bundle.alternate(entity, entity)  # Inference 16
        entity2 = bundle.entity("e002", [])
        bundle.alternate(entity2, entity2)  # Inference 16

        bundle.generation(entity, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        bundle.generation(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity2, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid5",
                            other_attributes=[])  # Inference 7

        # Inference 12
        bundle.derivation(entity, entity2, self.existential_variable_id_generator.get_activity_id(),
                          identifier="derivation2",
                          other_attributes=[])  # wasDerivedFrom - if no activity, generation and usage are non expandable
        bundle.influence(entity, entity2, identifier="influence14",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        # bundle.alternate(entity, entity2)  # alternateOf missing - Inference 12 nok
        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_13_generation_missing_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity("e001", [])
        bundle.alternate(entity, entity)  # Inference 16
        agent = bundle.agent("ag00", [])
        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8

        bundle.generation(entity, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        bundle.attribution(entity, agent, identifier="attribution1",
                           other_attributes=[])  # wasAttributedTo, Inference 13
        bundle.influence(entity, agent, identifier="influence10",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        # bundle.generation(entity, activity, identifier="gen2",
        # time=placeholder_time, other_attributes=[]) - generation missing - Inference 13 nok

        bundle.association(activity, agent, identifier="association1",
                           plan=self.existential_variable_id_generator.get_plan_id(),
                           other_attributes=[])  # wasAssociatedWith - Inference 13
        bundle.influence(activity, agent, identifier="influence11",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_13_association_missing_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity("e001", [])
        bundle.alternate(entity, entity)  # Inference 16
        agent = bundle.agent("ag00", [])
        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8

        bundle.generation(entity, self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time, other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        bundle.attribution(entity, agent, identifier="attribution1",
                           other_attributes=[])  # wasAttributedTo, Inference 13
        bundle.influence(entity, agent, identifier="influence10",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        bundle.generation(entity, activity, identifier="gen2", time=self.placeholder_time, other_attributes=[])
        # bundle.association(activity, agent, identifier="association1", plan=existential_plan,
        #                  other_attributes=[])  # wasAssociatedWith - Inference 13 nok - association missing
        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_14_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity("e001", [])
        bundle.alternate(entity, entity)  # Inference 16
        agent = bundle.agent("ag00", [])
        agent2 = bundle.agent("ag01", [])
        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8

        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        # Inference 14
        bundle.delegation(agent2, agent, activity, identifier="delegation1",
                          other_attributes=[])  # actedOnBehalfOf
        bundle.influence(agent2, agent, identifier="influence12",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        """bundle.association(activity, agent2, identifier="association2", plan=existential_plan,
                           other_attributes=[])
        bundle.influence(activity, agent2, identifier="influence13",
                         other_attributes=[])  # wasInfluencedBy, Inference 15
        bundle.association(activity, agent, identifier="association1", plan=existential_plan,
                           other_attributes=[])  # wasAssociatedWith - Inference 13, Inference 14
        bundle.influence(activity, agent, identifier="influence11",
                         other_attributes=[])  # wasInfluencedBy, Inference 15 - associations missing - Inference 14 nok"""
        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_15_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity("e001", [])
        bundle.alternate(entity, entity)  # Inference 16

        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        # bundle.influence(entity, existential_activity, identifier="influence2",
        #                other_attributes=[])  # Inference 15 nok - missing influencedBy
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_16_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity("e001", [])
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        # bundle.alternate(entity, entity)  # Inference 16 nok - alternateOf missing

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_17_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity('e002', [])
        bundle.alternate(entity, entity)  # Inference 16
        entity2 = bundle.entity("e003", [])
        bundle.alternate(entity2, entity2)  # Inference 16
        entity3 = bundle.entity("e004", [])
        bundle.alternate(entity3, entity3)  # Inference 16
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        bundle.generation(entity2, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen3",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="influence3",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity2, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid5",
                            other_attributes=[])  # Inference 7
        bundle.generation(entity3, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen4",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity3, self.existential_variable_id_generator.get_activity_id(), identifier="influence4",
                         other_attributes=[])  # Inference
        bundle.invalidation(entity3, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid6",
                            other_attributes=[])  # Inference 7

        # Inference 17
        bundle.alternate(entity, entity2)
        bundle.alternate(entity2, entity)  # Inference 18
        bundle.alternate(entity2, entity3)
        bundle.alternate(entity3, entity2)  # Inference 18
        # bundle.alternate(entity2, entity4) - alternateOf(e2,e4) missing - Inference 17 nok

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_18_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')

        bundle.set_default_namespace('http://example.org/2/')
        entity = bundle.entity('e002', [])
        bundle.alternate(entity, entity)  # Inference 16
        entity2 = bundle.entity("e003", [])
        bundle.alternate(entity2, entity2)  # Inference 16
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        bundle.generation(entity2, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen3",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="influence3",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity2, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid5",
                            other_attributes=[])  # Inference 7

        bundle.alternate(entity, entity2)
        # bundle.alternate(entity2, entity)  # Inference 18 nok - missing symetric altenateOf

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_19_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity('e002', [])
        bundle.alternate(entity, entity)  # Inference 16
        entity2 = bundle.entity("e003", [])
        bundle.alternate(entity2, entity2)  # Inference 16
        entity3 = bundle.entity("e004", [])
        bundle.alternate(entity3, entity3)  # Inference 16
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        bundle.generation(entity2, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen3",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="influence3",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity2, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid5",
                            other_attributes=[])  # Inference 7
        bundle.generation(entity3, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen4",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity3, self.existential_variable_id_generator.get_activity_id(), identifier="influence4",
                         other_attributes=[])  # Inference
        bundle.invalidation(entity3, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid6",
                            other_attributes=[])  # Inference 7

        bundle.specialization(entity, entity2)  # specializationOf
        bundle.alternate(entity, entity2)  # Inference 20
        bundle.specialization(entity2, entity3)
        bundle.alternate(entity2, entity3)  # Inference 20
        # bundle.specialization(entity, entity3) - Inference 19 nok

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_20_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity('e002', [])
        bundle.alternate(entity, entity)  # Inference 16
        entity2 = bundle.entity("e003", [])
        bundle.alternate(entity2, entity2)  # Inference 16
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        bundle.generation(entity2, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen3",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="influence3",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity2, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid5",
                            other_attributes=[])  # Inference 7

        bundle.specialization(entity, entity2)  # specializationOf
        # bundle.alternate(entity, entity2)  # Inference 20 nok

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_infer_21_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity('e002', other_attributes=[("attribute", "attr1")])
        bundle.alternate(entity, entity)  # Inference 16
        entity2 = bundle.entity("e003", [])
        bundle.alternate(entity2, entity2)  # Inference 16
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        bundle.generation(entity2, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen3",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="influence3",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity2, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid5",
                            other_attributes=[])  # Inference 7

        bundle.specialization(entity, entity2)  # specializationOf, Inference 21 nok - entity is missing attribute
        bundle.alternate(entity, entity2)

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_constrain_22_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity('e002', other_attributes=[])
        bundle.alternate(entity, entity)  # Inference 16
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        bundle.entity('e002', other_attributes=[])  # same entity created - constrain 22 nok

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_constrain_23_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity('e002', other_attributes=[])
        bundle.alternate(entity, entity)  # Inference 16
        entity2 = bundle.entity("e003", [])
        bundle.alternate(entity2, entity2)  # Inference 16

        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        bundle.generation(entity2, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])  # id same as for generation for previous entity - constraint 23 nok
        bundle.influence(entity2, self.existential_variable_id_generator.get_activity_id(), identifier="influence3",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity2, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid5",
                            other_attributes=[])  # Inference 7

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_constrain_24_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity('e002', other_attributes=[])
        bundle.alternate(entity, entity)  # Inference 16
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen3",
                          time=self.placeholder_time,
                          other_attributes=[])  # entity has 2 generations with different ids - constraint 24 nok
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_constrain_25_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        entity = bundle.entity('e002', other_attributes=[])
        bundle.alternate(entity, entity)  # Inference 16
        bundle.generation(entity, activity=self.existential_variable_id_generator.get_activity_id(), identifier="gen2",
                          time=self.placeholder_time,
                          other_attributes=[])
        bundle.influence(entity, self.existential_variable_id_generator.get_activity_id(), identifier="influence2",
                         other_attributes=[])  # Inference 15
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid4",
                            other_attributes=[])  # Inference 7
        bundle.invalidation(entity, self.existential_variable_id_generator.get_activity_id(),
                            time=self.placeholder_time,
                            identifier="invalid5",
                            other_attributes=[])  # entity has 2 invalidations with different ids - constraint 25 nok

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_constrain_26_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact2", other_attributes=[])  # double start of activity - constraint 26 nok
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_constrain_27_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8
        end_ = bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                          ender=self.existential_variable_id_generator.get_activity_id(),
                          time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                          identifier="endact2", other_attributes=[])  # double ended by for activity - constraint 27 nok

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_constrain_28_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        activity = bundle.activity(identifier='act001', startTime=datetime(2024, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact",
                     other_attributes=[])  # different time here than in activity definition - constraint 28 nok
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact", other_attributes=[])  # Inference 8

        self.check_is_normalized_negative(document)

    def test__is_graph_normalized_constrain_29_nok_returns_false(self):
        document = ProvDocument()

        document.set_default_namespace('http://example.org/0/')
        document.add_namespace('ex1', 'http://example.org/1/')
        document.add_namespace('ex2', 'http://example.org/2/')

        bundle = document.bundle('b001')
        bundle.set_default_namespace('http://example.org/2/')

        activity = bundle.activity(identifier='act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                   endTime=datetime(2024, 1, 7, 15, 8, 24, 78915),
                                   other_attributes=[])
        bundle.start(activity, trigger=self.existential_variable_id_generator.get_entity_id(),
                     starter=self.existential_variable_id_generator.get_activity_id(),
                     time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                     identifier="startact", other_attributes=[])  # Inference 8
        bundle.end(activity, self.existential_variable_id_generator.get_entity_id(),
                   ender=self.existential_variable_id_generator.get_activity_id(),
                   time=datetime(2025, 1, 7, 15, 8, 24, 78915),
                   identifier="endact",
                   other_attributes=[])  # different time here than in activity definition - constraint 29 nok

        self.check_is_normalized_negative(document)