from datetime import datetime
from unittest import TestCase

from prov.constants import PROV_TYPE
from prov.identifier import Namespace
from prov.model import ProvBundle, first, ProvActivity, ProvUsage, ProvGeneration, ProvAttribution, ProvAgent, ProvEntity, ProvSpecialization, ProvDerivation, ProvDocument

import helpers
from provenance.CPM_validation_strategies import CPMValidatorFirst
from provenance.constants import CPM_MAIN_ACTIVITY, CPM_BACKWARD_CONNECTOR, CPM_REFERENCED_BUNDLE_ID, \
    CPM_REFERENCED_META_BUNDLE_ID, CPM_REFERENCED_BUNDLE_HASH_VALUE, CPM_HASH_ALG, CPM_FORWARD_CONNECTOR, \
    CPM_SENDER_AGENT
from provenance_integration_tests.api_test_helpers import provenance_storage_hospital_name
from test_validators import create_main_act_attributes


class MyTestCase(TestCase):
    # tests use InputGraphChecker for graph parsing

    # change used validator in case new one implemented
    CPM_validator = CPMValidatorFirst()

    # prefix for recognizing existential variables
    existential_variable_prefix = "_"
    # create class for generating existential identifiers
    existential_variable_id_generator = helpers.ExistentialVariablesGenerator(existential_variable_prefix)
    # placeholder time - use as existential variable in next tests - change to needed time
    placeholder_time = datetime.now()

    # create global doc with backward connector
    backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector, maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _ = (
        helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector())

    doc2, backbone_parts2, next_meta_bundle_id, next_cpm_bundle_info = helpers.create_cpm_provenance_with_backward_connector(
        document_with_backwards_connector,
        bundle_with_backwards_conn, "_",
        prev_cpm_bundle_info=prev_cpm_bundle_info,
        prev_meta_bundle_info=prev_meta_bundle_info,
        sender_org_name="hospital_org",
        main_activity_attributes=maa2,
        backward_connector_attributes=backward_connector_attributes)

    next_meta_bundle_info = [next_meta_bundle_id.namespace.uri, next_meta_bundle_id.localpart]
    remote_bundle_namespace_2 = bundle_with_backwards_conn.add_namespace("remote_bundle",
                                                                         next_cpm_bundle_info[0])
    remote_meta_bundle_namespace_2 = bundle_with_backwards_conn.add_namespace(
        "remote_meta_bundle", next_meta_bundle_info[0]
    )

    def test__check_CPM_constraints_added_connector_between_backbone_and_ds_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        # add usage of domain entity by main activity - wrong
        placeholder_time = datetime(2025, 1, 6, 15, 8, 24, 78915)
        bb_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in bb_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]
        entity_to_connect = first(bundle.get_record("e003"))
        bundle.usage(entity_to_connect, main_activity, time=placeholder_time, identifier="usage_main_act_wrong",
                     other_attributes=[])

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])

    def test__check_CPM_constraints_added_relation_to_main_activity_generation_of_not_forward_connector_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        placeholder_time = datetime(2025, 1, 6, 15, 8, 24, 78915)
        entity_to_generate = first(bundle.get_record("e003"))
        backbone_entities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_entities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]
        bundle.wasGeneratedBy(entity_to_generate, main_activity, placeholder_time, other_attributes=[])

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Main activity generated entity that is not forward connector', result[1])

    def test__check_CPM_constraints_added_relation_to_main_activity_usage_of_not_backward_connector_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
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

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Main activity used entity that is not backward connector', result[1])

    def test__check_CPM_constraints_backwards_connector_connected_to_same_bundle_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info,
         remote_meta_bundle_namespace) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        remote_bundle_namespace_2 = bundle_with_backwards_conn.add_namespace("remote_bundle_2",
                                                                             bundle_with_backwards_conn.identifier.namespace.uri)
        backward_connector_attributes = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_2[bundle_with_backwards_conn.identifier.localpart],
            # change referenced bundle id to the bundle being saved now
            CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace[
                self.prev_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.prev_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b
        backbone_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])

    def test__check_CPM_constraints_missing_relation_to_main_activity_from_backward_connector_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b
        backbone_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        usage_connector = [x for x in backbone_with_backwards_connector if isinstance(x, ProvUsage)][0]
        records_all = bundle._records
        records_all.remove(usage_connector)

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Backward connector [remote_bundle:pathology:e001_sample_backwards_connector] '
                         'not used by main activity and no other backward connector derived from it.', result[1])

    def test__check_backward_connectors_ok_returns_true(self):
        result = self.CPM_validator.check_backward_connectors_attributes([x for x in self.backbone_parts2 if
                                                                          CPM_BACKWARD_CONNECTOR in x.get_asserted_types()])
        self.assertTrue(result)

    def test__check_backward_connectors_CPM_REFERENCED_BUNDLE_ID_missing_returns_false(self):
        backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector, maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _ = (
            helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector())

        # remove attribute from connector
        backward_connector_attributes.pop(CPM_REFERENCED_BUNDLE_ID)

        doc2, backbone_parts2, self.next_meta_bundle_id, self.next_cpm_bundle_info = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        self.assertFalse(self.CPM_validator.check_backward_connectors_attributes([x for x in backbone_parts2 if
                                                                                  CPM_BACKWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_backward_connectors_CPM_REFERENCED_META_BUNDLE_ID_missing_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        # remove attribute from connector
        backward_connector_attributes.pop(CPM_REFERENCED_META_BUNDLE_ID)

        doc2, backbone_parts2, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        self.assertFalse(self.CPM_validator.check_backward_connectors_attributes([x for x in backbone_parts2 if
                                                                                  CPM_BACKWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_backward_connectors_CPM_REFERENCED_BUNDLE_HASH_VALUE_missing_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        # remove attribute from connector
        backward_connector_attributes.pop(CPM_REFERENCED_BUNDLE_HASH_VALUE)

        doc2, backbone_parts2, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        self.assertFalse(self.CPM_validator.check_backward_connectors_attributes([x for x in backbone_parts2 if
                                                                                  CPM_BACKWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_backward_connectors_CPM_HASH_ALG_missing_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        # remove attribute from connector
        backward_connector_attributes.pop(CPM_HASH_ALG)

        doc2, backbone_parts2, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        self.assertFalse(self.CPM_validator.check_backward_connectors_attributes([x for x in backbone_parts2 if
                                                                                  CPM_BACKWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_forward_connectors_ok_returns_true(self):
        storage_name = "provider"
        org_name = "org"

        main_activity_attributes_ok, maa2, timestamp = create_main_act_attributes(storage_name)

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_meta_bundle_namespace_2[
                self.next_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, _, _, backbone, _ = helpers.create_cpm_with_forward_connector(
            self.existential_variable_prefix,
            storage_name,
            org_name=org_name, main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="receiver", timestamp=datetime.now().timestamp(),
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        self.assertTrue(self.CPM_validator.check_forward_connectors_attributes([x for x in backbone if
                                                                                CPM_FORWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_forward_connectors_ok_2_returns_true(self):
        storage_name = "provider"
        org_name = "org"

        main_activity_attributes_ok, maa2, timestamp = create_main_act_attributes(storage_name)

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
        }

        doc2, _, _, backbone, _ = helpers.create_cpm_with_forward_connector(
            self.existential_variable_prefix,
            storage_name,
            org_name=org_name, main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="receiver", timestamp=datetime.now().timestamp(),
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        self.assertTrue(self.CPM_validator.check_forward_connectors_attributes([x for x in backbone if
                                                                                CPM_FORWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_forward_connectors_CPM_REFERENCED_BUNDLE_ID_missing_returns_false(self):
        storage_name = "provider"
        org_name = "org"

        main_activity_attributes_ok, maa2, timestamp = create_main_act_attributes(storage_name)

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            # CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_meta_bundle_namespace_2[
                self.next_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, _, _, backbone, _ = helpers.create_cpm_with_forward_connector(
            self.existential_variable_prefix,
            storage_name,
            org_name=org_name, main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="receiver", timestamp=datetime.now().timestamp(),
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        self.assertFalse(self.CPM_validator.check_forward_connectors_attributes([x for x in backbone if
                                                                                 CPM_FORWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_forward_connectors_CPM_REFERENCED_META_BUNDLE_ID_missing_returns_false(self):
        storage_name = "provider"
        org_name = "org"

        main_activity_attributes_ok, maa2, timestamp = create_main_act_attributes(storage_name)

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            # CPM_REFERENCED_META_BUNDLE_ID: self.remote_meta_bundle_namespace_2[
            #    self.next_meta_bundle_info[1]
            # ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, _, _, backbone, _ = helpers.create_cpm_with_forward_connector(
            self.existential_variable_prefix,
            storage_name,
            org_name=org_name, main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="receiver", timestamp=datetime.now().timestamp(),
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        self.assertFalse(self.CPM_validator.check_forward_connectors_attributes([x for x in backbone if
                                                                                 CPM_FORWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_forward_connectors_CPM_REFERENCED_BUNDLE_HASH_VALUE_missing_returns_false(self):
        storage_name = "provider"
        org_name = "org"

        main_activity_attributes_ok, maa2, timestamp = create_main_act_attributes(storage_name)

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_meta_bundle_namespace_2[
                self.next_meta_bundle_info[1]
            ],
            # CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, _, _, backbone, _ = helpers.create_cpm_with_forward_connector(
            self.existential_variable_prefix,
            storage_name,
            org_name=org_name, main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="receiver", timestamp=datetime.now().timestamp(),
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        self.assertFalse(self.CPM_validator.check_forward_connectors_attributes([x for x in backbone if
                                                                                 CPM_FORWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_forward_connectors_CPM_HASH_ALG_missing_returns_false(self):
        storage_name = "provider"
        org_name = "org"

        main_activity_attributes_ok, maa2, timestamp = create_main_act_attributes(storage_name)

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_meta_bundle_namespace_2[
                self.next_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            # CPM_HASH_ALG: "SHA256"
        }

        doc2, _, _, backbone, _ = helpers.create_cpm_with_forward_connector(
            self.existential_variable_prefix,
            storage_name,
            org_name=org_name, main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="receiver", timestamp=datetime.now().timestamp(),
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)

        self.assertFalse(self.CPM_validator.check_forward_connectors_attributes([x for x in backbone if
                                                                                 CPM_FORWARD_CONNECTOR in x.get_asserted_types()]))

    def test__check_CPM_constraints_ok_1_returns_true(self):
        storage_name = "provider"
        org_name = "org"

        main_activity_attributes_ok, maa2, timestamp = create_main_act_attributes(storage_name)

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_meta_bundle_namespace_2[
                self.next_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, bundle, _, backbone, _ = helpers.create_cpm_with_forward_connector(
            self.existential_variable_prefix,
            storage_name,
            org_name=org_name, main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="receiver", timestamp=datetime.now().timestamp(),
            bundle_suffix="_with_forward_connector_wrong",
            forward_connector_attributes=forward_connector_attributes)
        backbone_activities = [x for x in backbone if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertTrue(result[0])

    def test__check_CPM_constraints_missing_relation_to_main_activity_from_forward_connector_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        gen_connector = [x for x in backbone_with_backwards_connector if isinstance(x, ProvGeneration)][0]
        records_all = bundle._records
        records_all.remove(gen_connector)
        backbone_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Forward connector [pathology:example:e003_connector] not generated by main '
                         'activity and not derived from other forward connector.', result[1])

    def test__check_CPM_constraints_derivation_of_backward_connector_from_entity_from_domain_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        backward_connector = first(bundle.get_record("remote_bundle:pathology:e001_sample_backwards_connector"))
        sample_ent = first(bundle.get_record('pathology:e001'))
        backward_connector.wasDerivedFrom(sample_ent)

        backbone_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Backward connector is related to entity that is not connector by derivation', result[1])

    def test__check_CPM_constraints_missing_relation_to_sender_agent_from_backward_connector_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        attribution_connector = [x for x in backbone_with_backwards_connector if isinstance(x, ProvAttribution)][0]
        records_all = bundle._records
        records_all.remove(attribution_connector)

        backbone_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Sender agent is not attributed to backward connector', result[1])

    def test__check_CPM_constraints_missing_sender_agent_for_backward_connector_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
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

        backbone_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Backward connector does not have agent attributed', result[1])

    def test__check_CPM_constraints_missing_relation_to_receiver_agent_from_specialized_forward_connector_returns_false(self):
        main_activity_attributes_ok, main_act_attr2, timestamp = create_main_act_attributes("hospital")

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_bundle_namespace_2[  # wronf ns - does not matter for this test
                self.prev_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, bundle, _, backbone_parts, _ = helpers.create_cpm_with_forward_connector(
            "ex",
            "hospital",
            "org", datetime.now().timestamp(),
            main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="pathology", forward_connector_attributes=forward_connector_attributes)

        attribution_connector = [x for x in backbone_parts if isinstance(x, ProvAttribution)][0]
        records_all = bundle._records
        records_all.remove(attribution_connector)

        backbone_activities = [x for x in backbone_parts if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_parts if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_parts if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Receiver agent is not attributed to forward connector', result[1])

    def test__check_CPM_constraints_derivation_of_forward_connector_from_not_connector_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)
        bundle = ProvBundle()
        for b in doc2.bundles:
            bundle = b

        entities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvEntity)]
        forward_connector = [x for x in entities if first(x.get_attribute(PROV_TYPE)) == CPM_FORWARD_CONNECTOR][0]
        main_activity = \
            [x for x in backbone_with_backwards_connector if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]
        forward_connector.wasDerivedFrom(main_activity)

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual(('Forward connector [pathology:example:e003_connector] derived from entity '
                          'other than forward or backward connector.'), result[1])

    def test__check_CPM_constraints_derivation_of_backward_connector_from_not_connector_returns_false(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = None
        for b in doc2.bundles:
            bundle = b

        entities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvEntity)]
        backward_connector = [x for x in entities if first(x.get_attribute(PROV_TYPE)) == CPM_BACKWARD_CONNECTOR][0]
        random_entity = first(bundle.get_record('example:e004'))
        backward_connector.wasDerivedFrom(random_entity)

        backbone_activities = [x for x in backbone_with_backwards_connector if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_with_backwards_connector if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_with_backwards_connector if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Backward connector is related to entity that is not connector by derivation', result[1])

    def test__check_CPM_constraints_specialization_between_concrete_and_general_forward_connector_missing_returns_false(self):
        main_activity_attributes_ok, main_act_attr2, timestamp = create_main_act_attributes("hospital")

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_bundle_namespace_2[  # wronf ns - does not matter for this test
                self.prev_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, bundle, _, backbone_parts, _ = helpers.create_cpm_with_forward_connector(
            "ex",
            "hospital",
            "org", datetime.now().timestamp(),
            main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="pathology", forward_connector_attributes=forward_connector_attributes)

        specialization_connector = [x for x in backbone_parts if isinstance(x, ProvSpecialization)][0]
        records_all = bundle._records
        records_all.remove(specialization_connector)

        backbone_activities = [x for x in backbone_parts if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_parts if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_parts if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Forward connector [hospital:hospital:e001_connector_s1] is not general one '
                         'and not specialized from other forward connector.', result[1])

    def test__check_CPM_constraints_derivation_between_first_and_redundant_forward_connector_missing_returns_false(self):
        main_activity_attributes_ok, main_act_attr2, timestamp = create_main_act_attributes("hospital")

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_bundle_namespace_2[  # wrong ns - does not matter for this test
                self.prev_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc_two_forward_conns, bundle, _, backbone_parts, first_with_two_conns_cpm_bundle_info = (
            helpers.create_cpm_with_forward_connector(
                "ex_",
                storage_name="hospital",
                org_name="org_id", main_activity_attributes=main_activity_attributes_ok,
                receiver_org_name="pathology",
                timestamp=timestamp, bundle_suffix="_with_both_forward_connectors",
                forward_connector_attributes=forward_connector_attributes, add_second_forward_connector=True,
                second_forward_connector_attributes=forward_connector_attributes,  # same attributes  - does not matter
                second_receiver_org_name="provenance_storage_hospital"))

        derivation_connector = [x for x in backbone_parts if isinstance(x, ProvDerivation)][0]
        records_all = bundle._records
        records_all.remove(derivation_connector)

        backbone_activities = [x for x in backbone_parts if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_parts if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_parts if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Forward connector [hospital:hospital:e002_connector] has many generations or '
                         'is missing one, or is not derived from other connector.', result[1])

    def test__check_CPM_constraints_derivation_between_first_and_redundant_backward_connector_missing_returns_false(self):
        main_activity_attributes_ok, main_act_attr2, timestamp = create_main_act_attributes("hospital")

        # prepare document with redundant connector
        document_with_backwards_connector_updated = ProvDocument()

        bundle_namespace = Namespace(
            provenance_storage_hospital_name,
            f"http://prov-storage-{provenance_storage_hospital_name}:8000/api/v1/organizations/org/documents/",
        )
        bundle_identifier = f"test_{timestamp}_bundle_end_updated"
        bundle = document_with_backwards_connector_updated.bundle(bundle_namespace[bundle_identifier])
        bundle.add_namespace("example", "http://example.com#")

        remote_bundle_namespace_1 = bundle.add_namespace("remote_bundle_1",
                                                         self.next_cpm_bundle_info[0])
        bundle.add_namespace("remote_bundle_2",
                                                         self.next_cpm_bundle_info[0])
        remote_meta_bundle_namespace_1 = bundle.add_namespace(
            "remote_meta_bundle_1", self.next_meta_bundle_info[0]
        )

        backward_connector_attributes_1 = {
            PROV_TYPE: CPM_BACKWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_1[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace_1[
                self.next_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        document_with_backwards_connector_updated, bundle, meta_bundle_id_end, backbone_parts, last_cpm_bundle_info_3 = (
            helpers.create_cpm_provenance_basic_without_fw_connector_with_two_bw_connectors(
                document_with_backwards_connector_updated, bundle,
                "ex_",
                storage_name=provenance_storage_hospital_name,
                main_activity_attributes=main_activity_attributes_ok,
                # add backward connectors to reference basic cpm doc, doc with both connectors
                sender_org_name_1=provenance_storage_hospital_name,
                backward_connector_attributes_1=backward_connector_attributes_1,
                sender_org_name_2="pathology",
                backward_connector_attributes_2=backward_connector_attributes_1))

        # remove derivation relation
        derivation_connector = [x for x in backbone_parts if isinstance(x, ProvDerivation)][0]
        records_all = bundle._records
        records_all.remove(derivation_connector)

        backbone_activities = [x for x in backbone_parts if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_parts if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_parts if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Backward connector '
                         '[remote_bundle_1:hospital:e001_sample_backwards_connector] has many usages '
                         'or is missing one or nothing was derived from it.', result[1])

    def test__check_CPM_constraints_derivation_of_redundant_forward_connector_from_domain_entity_returns_false(self):
        main_activity_attributes_ok, main_act_attr2, timestamp = create_main_act_attributes("hospital")

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_bundle_namespace_2[  # wrong ns - does not matter for this test
                self.prev_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc_two_forward_conns, bundle, _, backbone_parts, first_with_two_conns_cpm_bundle_info = (
            helpers.create_cpm_with_forward_connector(
                "ex_",
                storage_name="hospital",
                org_name="org_id", main_activity_attributes=main_activity_attributes_ok,
                receiver_org_name="pathology",
                timestamp=timestamp, bundle_suffix="_with_both_forward_connectors",
                forward_connector_attributes=forward_connector_attributes, add_second_forward_connector=True,
                second_forward_connector_attributes=forward_connector_attributes,  # same attributes  - does not matter
                second_receiver_org_name="provenance_storage_hospital"))

        derivation_connector = [x for x in backbone_parts if isinstance(x, ProvDerivation)][0]
        records_all = bundle._records
        records_all.remove(derivation_connector)

        # add wrong connector
        conn_id = f"hospital:hospital:e002_connector"
        entity = bundle.get_record("example:e003")[0]
        conn = bundle.get_record(conn_id)[0]
        bundle.wasDerivedFrom(conn, entity)

        backbone_activities = [x for x in backbone_parts if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_parts if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_parts if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual(('Forward connector [hospital:hospital:e002_connector] derived from entity '
                          'other than forward or backward connector.'), result[1])

    def test__check_CPM_constraints_generation_of_forward_connector_by_main_activity_and_other_activity_returns_false(self):
        main_activity_attributes_ok, main_act_attr2, timestamp = create_main_act_attributes("hospital")

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_bundle_namespace_2[  # wrong ns - does not matter for this test
                self.prev_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc_two_forward_conns, bundle, _, backbone_parts, first_with_two_conns_cpm_bundle_info = (
            helpers.create_cpm_with_forward_connector(
                "ex_",
                storage_name="hospital",
                org_name="org_id", main_activity_attributes=main_activity_attributes_ok,
                receiver_org_name="pathology",
                timestamp=timestamp, bundle_suffix="_with_both_forward_connectors",
                forward_connector_attributes=forward_connector_attributes))

        # add wrong connector
        conn_id = f"hospital:hospital:e001_connector"
        activity = bundle.get_record("example:act001")[0]
        conn = bundle.get_record(conn_id)[0]
        bundle.wasGeneratedBy(conn, activity)

        backbone_activities = [x for x in backbone_parts if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_parts if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_parts if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual(('Forward connector [hospital:hospital:e001_connector] has many generations or '
                          'is missing one, or is not derived from other connector.'), result[1])

    def test__check_CPM_constraints_generation_of_forward_connector_by_not_main_activity_returns_false(self):
        main_activity_attributes_ok, main_act_attr2, timestamp = create_main_act_attributes("hospital")

        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_bundle_namespace_2[  # wrong ns - does not matter for this test
                self.prev_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc_two_forward_conns, bundle, _, backbone_parts, first_with_two_conns_cpm_bundle_info = (
            helpers.create_cpm_with_forward_connector(
                "ex_",
                storage_name="hospital",
                org_name="org_id", main_activity_attributes=main_activity_attributes_ok,
                receiver_org_name="pathology",
                timestamp=timestamp, bundle_suffix="_with_both_forward_connectors",
                forward_connector_attributes=forward_connector_attributes))

        generation_connector = [x for x in backbone_parts if isinstance(x, ProvGeneration)][0]
        records_all = bundle._records
        records_all.remove(generation_connector)

        # add wrong connector
        conn_id = f"hospital:hospital:e001_connector"
        activity = bundle.get_record("example:act001")[0]
        conn = bundle.get_record(conn_id)[0]
        bundle.wasGeneratedBy(conn, activity)

        backbone_activities = [x for x in backbone_parts if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_parts if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_parts if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual(('Forward connector [hospital:hospital:e001_connector] generated by activity '
                          'other than main one.'), result[1])

    def test__check_CPM_constraints_connector_references_bundle_returns_false(self):
        main_activity_attributes_ok, main_act_attr2, timestamp = create_main_act_attributes("hospital")

        bundle_namespace = Namespace(
            "hospital",
            f"http://prov-storage-hospital:8000/api/v1/organizations/org/documents/",
        )
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: bundle_namespace[f"test_{timestamp}_bundle"],
            # same id as of bundle being created now
            CPM_REFERENCED_META_BUNDLE_ID: self.remote_bundle_namespace_2[  # wronf ns - does not matter for this test
                self.prev_meta_bundle_info[1]
            ],
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, bundle, _, backbone_parts, _ = helpers.create_cpm_with_forward_connector(
            "ex",
            "hospital",
            "org", timestamp,
            main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="pathology", forward_connector_attributes=forward_connector_attributes)
        backbone_activities = [x for x in backbone_parts if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_parts if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_parts if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual(f'Forward or backward connector references this bundle [hospital:test_{timestamp}_bundle].',
                         result[1])

    def test__check_CPM_constraints_connector_references_meta_bundle_returns_false(self):
        main_activity_attributes_ok, main_act_attr2, timestamp = create_main_act_attributes("hospital")

        bundle_namespace = Namespace(
            "hospital",
            f"http://prov-storage-hospital:8000/api/v1/organizations/org/documents/",
        )
        forward_connector_attributes = {
            PROV_TYPE: CPM_FORWARD_CONNECTOR,
            CPM_REFERENCED_BUNDLE_ID: self.remote_bundle_namespace_2[self.next_cpm_bundle_info[1]],
            CPM_REFERENCED_META_BUNDLE_ID: main_activity_attributes_ok[CPM_REFERENCED_META_BUNDLE_ID],
            # same meta bundle id as is main activity referencing in bundle being created now
            CPM_REFERENCED_BUNDLE_HASH_VALUE: self.next_cpm_bundle_info[2],
            CPM_HASH_ALG: "SHA256"
        }

        doc2, bundle, _, backbone_parts, _ = helpers.create_cpm_with_forward_connector(
            "ex",
            "hospital",
            "org", timestamp,
            main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name="pathology", forward_connector_attributes=forward_connector_attributes)
        backbone_activities = [x for x in backbone_parts if isinstance(x, ProvActivity)]
        main_activity = [x for x in backbone_activities if first(x.get_attribute(PROV_TYPE)) == CPM_MAIN_ACTIVITY][0]

        result = self.CPM_validator.check_cpm_constraints(bundle, [x for x in backbone_parts if
                                                                   CPM_FORWARD_CONNECTOR in x.get_asserted_types()],
                                                          [x for x in backbone_parts if
                                                           CPM_BACKWARD_CONNECTOR in x.get_asserted_types()],
                                                          main_activity)
        self.assertFalse(result[0])
        self.assertEqual('Forward or backward connector references this meta bundle '
                         f'[{main_activity_attributes_ok[CPM_REFERENCED_META_BUNDLE_ID]}].',
                         result[1])
