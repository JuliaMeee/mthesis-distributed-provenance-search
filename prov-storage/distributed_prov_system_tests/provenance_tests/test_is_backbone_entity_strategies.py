from unittest import TestCase

import helpers
from provenance.is_backbone_entity_strategies import IsBackboneStrategyOriginal


class MyTestCase(TestCase):
    #change strategy if needed
    is_backbone_strategy = IsBackboneStrategyOriginal()

    def test_is_backbone_element_multiple_cases(self):
        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector, maa2,
         prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic_and_prepare_bundle_referencing_it_with_backward_connector()

        # create cpm component
        doc2, backbone_parts2, self.next_meta_bundle_id, self.next_cpm_bundle_info = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle_test = None
        for bundle in doc2.bundles:
            bundle_test = bundle

        # call function and check whether it decided right
        for x in range(len(backbone_parts2)):
            if backbone_parts2[x].is_element():
                self.assertTrue(self.is_backbone_strategy.is_backbone_element(backbone_parts2[x], bundle_test), msg=backbone_parts2[x])

        for x in bundle_test.records:
            if x.is_element() and x not in backbone_parts2:
                self.assertFalse(self.is_backbone_strategy.is_backbone_element(x, bundle_test),
                                msg=x)