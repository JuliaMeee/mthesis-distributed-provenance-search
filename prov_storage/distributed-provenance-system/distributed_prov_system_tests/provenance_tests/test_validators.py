from datetime import datetime
from unittest import TestCase

from unittest.mock import MagicMock, call
from unittest.mock import patch

from prov.model import first, ProvActivity
from requests import Response

import helpers
from constants import CPM_MAIN_ACTIVITY
from prov_doc_validators_strategies import ProvValidatorWithNormalization
from provenance.neomodel2prov import NAMESPACES
from provenance.validators import *
from provenance.validators import _check_connectors_references


def create_main_act_attributes(storage_name):
    meta_ns = Namespace(
        "meta", f"http://prov-storage-{storage_name}:8000/api/v1/documents/meta/"
    )
    timestamp = datetime.now().timestamp()
    main_activity_attributes_ok = {
        provm.PROV_TYPE: CPM_MAIN_ACTIVITY,
        CPM_REFERENCED_META_BUNDLE_ID: meta_ns[
            # id of meta bundle - used when requesting meta provenance
            f"test_{timestamp}_bundle_meta"
        ],
    }
    main_act_attr2 = {
        provm.PROV_TYPE: CPM_MAIN_ACTIVITY,
        CPM_REFERENCED_META_BUNDLE_ID: meta_ns[
            f"test_{timestamp}_bundle_w_backward_connector_meta"
        ],
    }
    return main_activity_attributes_ok, main_act_attr2, timestamp

class MyTestCase(TestCase):
    # instantiate this prov validator - always returns true now - we do not want to test it here
    prov_validator = ProvValidatorWithNormalization()

    @patch('provenance.models.Organization.nodes')
    def test_is_org_registered_true(self, MockOrgNodes):
        neo_org = Organization()
        neo_org.identifier = "org_id"

        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = neo_org

        result = is_org_registered("org_id")

        self.assertTrue(result)
        MockOrgNodes.get.assert_called_with(identifier="org_id")

    @patch('provenance.models.Organization.nodes')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    def test_is_org_registered_false(self, MockDoesnotExist, MockOrgNodes):
        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = None
        MockOrgNodes.get.side_effect = DoesNotExist("no")

        result = is_org_registered("org_id")

        self.assertFalse(result)

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Organization.nodes')
    def test_check_organization_is_registered_does_not_exist(self, MockOrgNodes, MockDoesnotExist):
        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = None
        MockOrgNodes.get.side_effect = DoesNotExist("no")

        self.assertRaises(OrganizationNotRegistered, check_organization_is_registered, "org_id")

    @patch('provenance.models.Organization.nodes')
    def test_check_organization_is_registered_tp_not_checked(self, MockOrgNodes):
        org = Organization()
        org.identifier = "org_id"

        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = org

        trusted_party = TrustedParty()
        trusted_party.url = "url..."
        org.trusts.all = MagicMock()
        org.trusts.all.return_value = [trusted_party]

        self.assertRaises(UncheckedTrustedParty, check_organization_is_registered, "org_id")

    @patch('provenance.models.Organization.nodes')
    def test_check_organization_is_registered_tp_not_valid(self, MockOrgNodes):
        org = Organization()
        org.identifier = "org_id"

        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = org

        trusted_party = TrustedParty()
        trusted_party.url = "url..."
        trusted_party.checked = True
        org.trusts.all = MagicMock()
        org.trusts.all.return_value = [trusted_party]

        self.assertRaises(InvalidTrustedParty, check_organization_is_registered, "org_id")

    @patch('provenance.models.Organization.nodes')
    def test_check_organization_is_registered_ok(self, MockOrgNodes):
        org = Organization()
        org.identifier = "org_id"

        MockOrgNodes.get = MagicMock()
        MockOrgNodes.get.return_value = org

        trusted_party = TrustedParty()
        trusted_party.url = "url..."
        trusted_party.checked = True
        trusted_party.valid = True
        org.trusts.all = MagicMock()
        org.trusts.all.return_value = [trusted_party]

        check_organization_is_registered("org_id")  # does not raise exception

    @patch('provenance.models.Document.nodes')
    def test_graph_exists_ok(self, MockDocNodes):
        MockDocNodes.get = MagicMock()
        MockDocNodes.get.return_value = "ok"

        result = graph_exists("org_id", "graph_id")

        self.assertTrue(result)
        MockDocNodes.get.assert_called_with(identifier="org_id_graph_id")

    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    @patch('provenance.models.Document.nodes')
    def test_graph_exists_nok(self, MockDocNodes, MockDoesnotExist):
        MockDocNodes.get = MagicMock()
        MockDocNodes.get.return_value = None
        MockDocNodes.get.side_effect = DoesNotExist("nok")

        self.assertFalse(graph_exists("org_id", "graph_id"))

    @patch('provenance.models.Entity.nodes')
    @patch('provenance.neomodel2prov.neomodel.Traversal')
    @patch('provenance.models.Bundle.nodes')
    @patch('neomodel.exceptions.DoesNotExist._model_class', return_value='c')
    def test_check_graph_id_belongs_to_meta_Bundle_does_not_exist(self, MockDoesnotExist, MockBundleNodes,
                                                                  MockTraversal, MockEntityNodes):
        entity = Entity()
        entity.identifier = "entity_id"
        MockEntityNodes.get.return_value = entity

        MockTraversal.return_value = None

        MockBundleNodes.get = MagicMock()
        MockBundleNodes.get.return_value = None
        MockBundleNodes.get.side_effect = DoesNotExist("nok")

        self.assertRaises(DocumentError, check_graph_id_belongs_to_meta, "meta_id", "graph_id", "org_id")

    @patch('provenance.models.Entity.nodes')
    @patch('provenance.neomodel2prov.neomodel.Traversal')
    @patch('provenance.models.Bundle.nodes')
    def test_check_graph_id_belongs_to_meta_other_version_exists(self, MockBundleNodes,
                                                                 MockTraversal, MockEntityNodes):
        entity = Entity()
        entity.identifier = "entity_id"
        MockEntityNodes.get.return_value = entity

        MockBundleNodes.get = MagicMock()
        MockBundleNodes.get.return_value = "ok"

        mock_entity_traversal = MagicMock()
        mock_entity_traversal.all.return_value = [1]
        MockTraversal.return_value = mock_entity_traversal

        self.assertRaises(DocumentError, check_graph_id_belongs_to_meta, "meta_id", "graph_id", "org_id")

    @patch('provenance.models.Entity.nodes')
    @patch('provenance.neomodel2prov.neomodel.Traversal')
    @patch('provenance.models.Bundle.nodes')
    def test_check_graph_id_belongs_to_meta_multiple_meta_bundles(self, MockBundleNodes,
                                                                  MockTraversal, MockEntityNodes):
        entity = MagicMock()
        entity.identifier = "entity_id"
        entity.contains = ["1st", "2nd"]
        MockEntityNodes.get = MagicMock()
        MockEntityNodes.get.return_value = entity

        MockTraversal.return_value = None

        MockBundleNodes.get = MagicMock()
        MockBundleNodes.get.return_value = Bundle()

        self.assertRaises(AssertionError, check_graph_id_belongs_to_meta, "meta_id", "graph_id", "org_id")

    @patch('provenance.models.Entity.nodes')
    @patch('provenance.neomodel2prov.neomodel.Traversal')
    @patch('provenance.models.Bundle.nodes')
    @patch('provenance.validators.len')
    def test_check_graph_id_belongs_to_meta_multiple_meta_bundles(self, MockLength, MockBundleNodes,
                                                                  MockTraversal, MockEntityNodes):
        entity = MagicMock()
        entity.identifier = "entity_id"
        bundle = Bundle()
        bundle_id = "id"
        bundle.identifier = bundle_id

        MockLength.return_value = 1

        entity.contains.single = MagicMock()
        entity.contains.single.return_value = bundle
        MockEntityNodes.get = MagicMock()
        MockEntityNodes.get.return_value = entity

        MockTraversal.return_value = None

        MockBundleNodes.get = MagicMock()
        MockBundleNodes.get.return_value = Bundle()

        self.assertRaises(DocumentError, check_graph_id_belongs_to_meta, "meta_id", "graph_id", "org_id")

    @patch('provenance.models.Entity.nodes')
    @patch('provenance.neomodel2prov.neomodel.Traversal')
    @patch('provenance.models.Bundle.nodes')
    @patch('provenance.validators.len')
    def test_check_graph_id_belongs_to_meta_multiple_meta_bundles(self, MockLength, MockBundleNodes,
                                                                  MockTraversal, MockEntityNodes):
        entity = MagicMock()
        entity.identifier = "entity_id"
        bundle = Bundle()
        bundle_id = "id"
        bundle.identifier = bundle_id

        MockLength.return_value = 1

        entity.contains.single = MagicMock()
        entity.contains.single.return_value = bundle
        MockEntityNodes.get = MagicMock()
        MockEntityNodes.get.return_value = entity

        mock_entity_traversal = MagicMock()
        mock_entity_traversal.all.return_value = []
        MockTraversal.return_value = mock_entity_traversal

        MockBundleNodes.get = MagicMock()
        MockBundleNodes.get.return_value = Bundle()

        check_graph_id_belongs_to_meta(bundle_id, "graph_id", "org_id")  # no exception raised

    @patch("provenance.validators.contains_my_ip_addr")
    @patch("requests.head")
    def test_parse_graph(self, MockRequest, MockContainsIp):
        main_activity_attrs, _, _ = helpers.create_main_act_attributes("hospital")
        doc, bundle, _, _, _, _ = helpers.create_cpm_provenance_basic("ex", "hospital", "org",
                                                                      datetime.now().timestamp(), main_activity_attrs)
        data_b64 = base64.b64encode(doc.serialize().encode("utf-8"))

        checker = InputGraphChecker(data_b64, "json",
                                    "http://provenance-storage-hospital.com:8000/api/v1/organizations/org/",
                                    self.prov_validator)
        MockContainsIp.return_value = True
        response = Response()
        response.url = "/api/v1/documents/meta/meta_id"
        MockRequest.return_value = response

        checker.parse_graph()

        main_activity = None
        for activity in (list(doc.bundles)[0]).get_records(provm.ProvActivity):
            prov_types = activity.get_asserted_types()

            if prov_types is None:
                continue

            if activity.bundle.valid_qualified_name(CPM_MAIN_ACTIVITY) in prov_types:
                main_activity = activity

        self.assertEqual(list(doc.bundles)[0], checker._prov_bundle)
        self.assertEqual(main_activity, checker._main_activity)
        self.assertEqual("meta_id", checker._meta_provenance_id)
        self.assertEqual(checker._retrieve_connectors_from_graph()[0], checker._forward_connectors)
        self.assertEqual(checker._retrieve_connectors_from_graph()[1], checker._backward_connectors)

    @patch("provenance.validators.contains_my_ip_addr")
    @patch("requests.head")
    def test_parse_graph_no_main_activity(self, MockRequest, MockContainsIp):
        main_activity_attrs, _, _ = helpers.create_main_act_attributes("hospital")
        doc, bundle, _, _, _, _ = helpers.create_cpm_provenance_basic("ex", "hospital", "org",
                                                                      datetime.now().timestamp(), main_activity_attrs)
        main_act = first(bundle.get_record("hospital:test_main_activity"))
        bundle._records.remove(main_act)

        data_b64 = base64.b64encode(doc.serialize().encode("utf-8"))

        checker = InputGraphChecker(data_b64, "json",
                                    "http://provenance-storage-hospital.com:8000/api/v1/organizations/org/",
                                    self.prov_validator)

        MockContainsIp.return_value = True
        response = Response()
        response.url = "/api/v1/documents/meta/meta_id"
        MockRequest.return_value = response

        self.assertRaises(DocumentError, checker.parse_graph)

    @patch("provenance.validators.contains_my_ip_addr")
    @patch("requests.head")
    def test_parse_graph_wrong_ip(self, MockRequest, MockContainsIp):
        main_activity_attrs, _, _ = helpers.create_main_act_attributes("hospital")
        doc, bundle, _, _, _, _ = helpers.create_cpm_provenance_basic("ex", "hospital", "org",
                                                                      datetime.now().timestamp(), main_activity_attrs)
        data_b64 = base64.b64encode(doc.serialize().encode("utf-8"))

        checker = InputGraphChecker(data_b64, "json",
                                    "http://provenance-storage-hospital.com:8000/api/v1/organizations/org/",
                                    self.prov_validator)

        MockContainsIp.return_value = False
        response = Response()
        response.url = "/api/v1/documents/meta/meta_id"
        MockRequest.return_value = response

        self.assertRaises(DocumentError, checker.parse_graph)

    @patch("provenance.validators.contains_my_ip_addr")
    @patch("requests.head")
    def test_parse_graph_wrong_url(self, MockRequest, MockContainsIp):
        main_activity_attrs, _, _ = helpers.create_main_act_attributes("hospital")
        doc, bundle, _, _, _, _ = helpers.create_cpm_provenance_basic("ex", "hospital", "org",
                                                                      datetime.now().timestamp(), main_activity_attrs)
        data_b64 = base64.b64encode(doc.serialize().encode("utf-8"))

        checker = InputGraphChecker(data_b64, "json",
                                    "http://provenance-storage-hospital.com:8000/api/v1/organizations/org/",
                                    self.prov_validator)

        MockContainsIp.return_value = True
        response = Response()
        response.url = "/api/v3/wrong_url/meta_id"
        MockRequest.return_value = response

        self.assertRaises(DocumentError, checker.parse_graph)

    @patch("provenance.validators.contains_my_ip_addr")
    @patch("requests.head")
    def test_validate_graph_ok(self, MockRequest, MockContainsIp):
        storage_name = "provider"
        org_name = "org"
        main_activity_attributes_ok, main_act_attr_2, timestamp = create_main_act_attributes(storage_name)
        document, bundle, meta_bundle_id, backbone_parts, prev_cpm_bundle_info, _ \
            = helpers.create_cpm_provenance_basic("_", storage_name, org_name, timestamp, main_activity_attributes_ok)

        data = document.serialize()

        data_b64 = base64.b64encode(data.encode())

        checker = InputGraphChecker(data_b64, "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)
        MockContainsIp.return_value = True
        response = Response()
        response.url = "/api/v1/documents/meta/meta_id"
        MockRequest.return_value = response

        checker.parse_graph()

        self.assertIsNone(checker.validate_graph())

    def test_validate_graph_graph_not_parsed(self):
        main_activity_attrs, _, _ = helpers.create_main_act_attributes("hospital")
        doc, bundle, _, _, _, _ = helpers.create_cpm_provenance_basic("ex", "hospital", "org",
                                                                      datetime.now().timestamp(), main_activity_attrs)
        data_b64 = base64.b64encode(doc.serialize().encode("utf-8"))

        checker = InputGraphChecker(data_b64, "json",
                                    "http://provenance-storage-hospital.com:8000/api/v1/organizations/org/",
                                    self.prov_validator)

        self.assertRaises(AssertionError, checker.validate_graph)

    def test_validate_graph_graph_no_bundles(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)
        checker._prov_document = provm.ProvDocument()
        # not adding bundles to doc

        checker._prov_bundle = provm.ProvBundle()

        self.assertRaises(HasNoBundles, checker.validate_graph)

    def test_validate_graph_graph_too_many_bundles(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)
        checker._prov_document = provm.ProvDocument()
        checker._prov_document.set_default_namespace('http://anotherexample.org/')
        checker._prov_document.bundle("bundle1")
        checker._prov_document.bundle("bundle2")
        # adding 2 bundles to doc

        checker._prov_bundle = provm.ProvBundle()

        self.assertRaises(TooManyBundles, checker.validate_graph)

    @patch.object(InputGraphChecker, "_process_bundle_references")
    def test_validate_graph_graph_not_resolvable_references(self, MockProcessReferences):
        MockProcessReferences.return_value = False, "message"

        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)
        checker._prov_document = provm.ProvDocument()
        checker._prov_document.set_default_namespace('http://anotherexample.org/')
        checker._prov_document.bundle("bundle1")
        checker._forward_connectors = []
        checker._backward_connectors = []

        checker._prov_bundle = provm.ProvBundle()

        self.assertRaises(ConnectorReferenceInvalidError, checker.validate_graph)

    @patch.object(CPM_VALIDATOR, "check_backward_connectors_attributes")
    def test_validate_graph_graph_nok_backward_connector_attrs(self, MockCheckConnectors):
        MockCheckConnectors.return_value = False

        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)
        checker._prov_document = provm.ProvDocument()
        checker._prov_document.set_default_namespace('http://anotherexample.org/')
        checker._prov_document.bundle("bundle1")

        checker._prov_bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))

        self.assertRaises(DocumentError, checker.validate_graph)

    @patch.object(CPM_VALIDATOR, "check_backward_connectors_attributes")
    @patch.object(CPM_VALIDATOR, "check_forward_connectors_attributes")
    def test_validate_graph_graph_nok_forward_connector_attrs(self, MockCheckForwardConnectors,
                                                              MockCheckBackwardConnectors):
        MockCheckBackwardConnectors.return_value = True
        MockCheckForwardConnectors.return_value = False

        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)
        checker._prov_document = provm.ProvDocument()
        checker._prov_document.set_default_namespace('http://anotherexample.org/')
        checker._prov_document.bundle("bundle1")
        checker._forward_connectors = ["con1"]
        checker._backward_connectors = ["con2"]

        checker._prov_bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))

        self.assertRaises(DocumentError, checker.validate_graph)
        MockCheckForwardConnectors.assert_called_with(checker._forward_connectors)
        MockCheckBackwardConnectors.assert_called_with(checker._backward_connectors)

    @patch.object(CPM_VALIDATOR, "check_backward_connectors_attributes")
    @patch.object(CPM_VALIDATOR, "check_forward_connectors_attributes")
    @patch.object(InputGraphChecker, "_process_bundle_references")
    @patch.object(CPM_VALIDATOR, "check_cpm_constraints")
    def test_validate_graph_graph_nok_cpm_constraints(self, MockCheckCPMConstraints, MockProcessReferences,
                                                      MockCheckForwardConnectors,
                                                      MockCheckBackwardConnectors):
        MockCheckBackwardConnectors.return_value = True
        MockCheckForwardConnectors.return_value = True
        MockProcessReferences.return_value = True, "message"
        MockCheckCPMConstraints.return_value = False, "message"

        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)
        checker._prov_document = provm.ProvDocument()
        checker._prov_document.set_default_namespace('http://anotherexample.org/')
        checker._prov_document.bundle("bundle1")
        checker._forward_connectors = ["con1"]
        checker._backward_connectors = ["con2"]
        checker._main_activity = "main"

        checker._prov_bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))

        self.assertRaises(DocumentError, checker.validate_graph)
        MockCheckCPMConstraints.assert_called_with(checker._prov_bundle, checker._forward_connectors,
                                                   checker._backward_connectors, checker._main_activity)
        MockCheckForwardConnectors.assert_called_with(checker._forward_connectors)
        MockCheckBackwardConnectors.assert_called_with(checker._backward_connectors)

    @patch.object(CPM_VALIDATOR, "check_backward_connectors_attributes")
    @patch.object(CPM_VALIDATOR, "check_forward_connectors_attributes")
    @patch.object(InputGraphChecker, "_process_bundle_references")
    @patch.object(CPM_VALIDATOR, "check_cpm_constraints")
    @patch.object(InputGraphChecker, "_check_namespaces")
    def test_validate_graph_graph_nok_namespaces(self, MockCheckNamespaces, MockCheckCPMConstraints,
                                                 MockProcessReferences,
                                                 MockCheckForwardConnectors,
                                                 MockCheckBackwardConnectors):
        MockCheckBackwardConnectors.return_value = True
        MockCheckForwardConnectors.return_value = True
        MockProcessReferences.return_value = True, "message"
        MockCheckCPMConstraints.return_value = True, "message"
        MockCheckNamespaces.return_value = False

        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)
        checker._prov_document = provm.ProvDocument()
        checker._prov_document.set_default_namespace('http://anotherexample.org/')
        checker._prov_document.bundle("bundle1")
        checker._forward_connectors = ["con1"]
        checker._backward_connectors = ["con2"]
        checker._main_activity = "main"

        checker._prov_bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))

        self.assertRaises(DocumentError, checker.validate_graph)
        MockCheckCPMConstraints.assert_called_with(checker._prov_bundle, checker._forward_connectors,
                                                   checker._backward_connectors, checker._main_activity)
        MockCheckForwardConnectors.assert_called_with(checker._forward_connectors)
        MockCheckBackwardConnectors.assert_called_with(checker._backward_connectors)
        MockCheckNamespaces.assert_called_once()

    @patch.object(CPM_VALIDATOR, "check_backward_connectors_attributes")
    @patch.object(CPM_VALIDATOR, "check_forward_connectors_attributes")
    @patch.object(InputGraphChecker, "_process_bundle_references")
    @patch.object(CPM_VALIDATOR, "check_cpm_constraints")
    @patch.object(InputGraphChecker, "_check_namespaces")
    @patch("provenance.validators.CHECK_PROV_VALIDITY")
    def test_validate_graph_graph_nok_validity(self, MockCheckValidity, MockCheckNamespaces, MockCheckCPMConstraints,
                                                 MockProcessReferences,
                                                 MockCheckForwardConnectors,
                                                 MockCheckBackwardConnectors):
        MockCheckBackwardConnectors.return_value = True
        MockCheckForwardConnectors.return_value = True
        MockProcessReferences.return_value = True, "message"
        MockCheckCPMConstraints.return_value = True, "message"
        MockCheckNamespaces.return_value = True
        MockValidator = MagicMock()
        MockValidator.is_valid.return_value = False
        MockCheckValidity.return_value = True

        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    MockValidator)
        checker._prov_document = provm.ProvDocument()
        checker._prov_document.set_default_namespace('http://anotherexample.org/')
        checker._prov_document.bundle("bundle1")
        checker._forward_connectors = ["con1"]
        checker._backward_connectors = ["con2"]
        checker._main_activity = "main"

        checker._prov_bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        CHECK_PROV_VALIDITY = True

        self.assertRaises(DocumentError, checker.validate_graph)
        MockCheckCPMConstraints.assert_called_with(checker._prov_bundle, checker._forward_connectors,
                                                   checker._backward_connectors, checker._main_activity)
        MockCheckForwardConnectors.assert_called_with(checker._forward_connectors)
        MockCheckBackwardConnectors.assert_called_with(checker._backward_connectors)
        MockCheckNamespaces.assert_called_once()
        MockValidator.is_valid.assert_called_with(document=checker._prov_document)

    @patch("provenance.validators.contains_my_ip_addr")
    @patch("requests.head")
    def test__check_CPM_constraints_nok_added_second_main_activity(self, MockRequest,
                                                                   MockContainsIp):
        MockContainsIp.return_value = True
        response = Response()
        response.url = "/api/v1/documents/meta/meta_id"
        MockRequest.return_value = response

        (backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector,
         maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _) = helpers.create_prov_cpm_basic()

        doc2, backbone_with_backwards_connector, _, _ = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        bundle = provm.ProvBundle()
        for b in doc2.bundles:
            bundle = b

        main_activity_2 = bundle.activity(
            identifier=f"pathology:test_main_activity_2",
            startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
            endTime=datetime(2025, 1, 7, 16, 8, 24, 78915).isoformat(" "),
            other_attributes=create_main_act_attributes("provider")[0]
        )

        data = doc2.serialize()

        data_b64 = base64.b64encode(data.encode())
        checker = InputGraphChecker(data_b64, "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)

        with self.assertRaises(DocumentError):
            checker.parse_graph()

    def test__check_namespaces_one_defined_ns_ok(self):
        doc = provm.ProvDocument()
        ns = Namespace("def", "http://default#")
        doc.add_namespace(ns)
        doc_bundle = doc.bundle(ns["bundle_identifier"])
        doc_bundle.add_namespace("example", "http://example.com#")
        doc_bundle.entity("example:ent1")

        data = doc.serialize()
        data_b64 = base64.b64encode(data.encode())
        checker = InputGraphChecker(data_b64, "json", "http://sample_url#", ProvValidatorWithNormalization())

        checker._prov_document = provm.ProvDocument.deserialize(
            content=checker._graph, format=checker._graph_format
        )

        checker._prov_bundle = list(checker._prov_document.bundles)[0]
        self.assertTrue(checker._check_namespaces())

    def test__check_namespaces_two_defined_nss_ok(self):
        doc = provm.ProvDocument()
        ns = Namespace("def", "http://default#")
        doc.add_namespace(ns)
        doc_bundle = doc.bundle(ns["bundle_identifier"])
        doc_bundle.add_namespace("example", "http://example.com#")
        doc_bundle.add_namespace("example2", "http://example2.com#")
        doc_bundle.entity("example:ent1")
        doc_bundle.agent("example:ag1")
        doc_bundle.activity("example2:ac1")

        data = doc.serialize()
        data_b64 = base64.b64encode(data.encode())
        checker = InputGraphChecker(data_b64, "json", "http://sample_url", ProvValidatorWithNormalization())

        checker._prov_document = provm.ProvDocument.deserialize(
            content=checker._graph, format=checker._graph_format
        )

        checker._prov_bundle = list(checker._prov_document.bundles)[0]
        self.assertTrue(checker._check_namespaces())

    def test__check_namespaces_entity_without_namespace_nok(self):
        doc = provm.ProvDocument()
        ns = Namespace("def", "http://def#")
        doc.add_namespace(ns)
        doc_bundle = doc.bundle(ns["bundle_identifier"])
        doc_bundle.add_namespace("example", "http://example.com#")
        doc.set_default_namespace("http://default#")
        doc_bundle.entity("ent1")

        data = doc.serialize()
        data_b64 = base64.b64encode(data.encode())
        checker = InputGraphChecker(data_b64, "json", "http://sample_url", ProvValidatorWithNormalization())

        checker._prov_document = provm.ProvDocument.deserialize(
            content=checker._graph, format=checker._graph_format
        )

        checker._prov_bundle = list(checker._prov_document.bundles)[0]
        self.assertFalse(checker._check_namespaces())

    def test__check_namespaces_agent_without_namespace_nok(self):
        doc = provm.ProvDocument()
        ns = Namespace("def", "http://def#")
        doc.add_namespace(ns)
        doc_bundle = doc.bundle(ns["bundle_identifier"])
        doc_bundle.add_namespace("example", "http://example.com#")
        doc.set_default_namespace("http://default#")
        ag = doc_bundle.agent("ag1")
        ent = doc_bundle.entity("ent")
        ent.wasInvalidatedBy(ag)

        data = doc.serialize()
        data_b64 = base64.b64encode(data.encode())
        checker = InputGraphChecker(data_b64, "json", "http://sample_url", ProvValidatorWithNormalization())

        checker._prov_document = provm.ProvDocument.deserialize(
            content=checker._graph, format=checker._graph_format
        )

        checker._prov_bundle = list(checker._prov_document.bundles)[0]
        self.assertFalse(checker._check_namespaces())

    def test__check_namespaces_namespace_ends_on_non_special_character_nok(self):
        doc = provm.ProvDocument()
        ns = Namespace("def", "http://def")
        doc.add_namespace(ns)
        doc_bundle = doc.bundle(ns["bundle_identifier"])
        doc_bundle.add_namespace("example", "http://example.com")
        doc_bundle.entity("example:ent1")

        data = doc.serialize()
        data_b64 = base64.b64encode(data.encode())
        checker = InputGraphChecker(data_b64, "json", "http://sample_url", ProvValidatorWithNormalization())

        checker._prov_document = provm.ProvDocument.deserialize(
            content=checker._graph, format=checker._graph_format
        )

        checker._prov_bundle = list(checker._prov_document.bundles)[0]
        self.assertFalse(checker._check_namespaces())

    @patch('provenance.validators.requests')
    def test_send_signature_verification_request(self, MockRequests):
        payload = {"id": "id"}
        org_id = "org_id"
        MockRequests.post = MagicMock()
        MockRequests.post.return_value = requests.Response()
        MockRequests.post.return_value.status_code = 200

        result = send_signature_verification_request(payload, org_id, tp_url="tp_url")

        self.assertEqual(MockRequests.post.return_value, result)
        self.assertEqual(org_id, payload["organizationId"])
        MockRequests.post.assert_called_with("http://tp_url/api/v1/verifySignature", json.dumps(payload))

    def test_check_ids_match_ok(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)

        bundle = provm.ProvBundle(identifier=QualifiedName(
            Namespace("namespace", "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/"),
            "bundle"))
        checker._prov_bundle = bundle

        checker.check_ids_match("bundle")  # does not throw error

    def test_check_ids_match_nok_wrong_bundle_id(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)

        bundle = provm.ProvBundle(identifier=QualifiedName(
            Namespace("namespace", "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/"),
            "bundle"))
        checker._prov_bundle = bundle

        self.assertRaises(DocumentError, checker.check_ids_match,
                          "bundle1")  # different id than in bundle - throws error

    def test_check_ids_match_nok_wrong_uri(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle1",
                                    # different id than in bundle - throws error
                                    self.prov_validator)

        bundle = provm.ProvBundle(identifier=QualifiedName(
            Namespace("namespace", "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/"),
            "bundle"))
        checker._prov_bundle = bundle

        self.assertRaises(DocumentError, checker.check_ids_match, "bundle")

    def test__contains_my_ip_addr_ok(self):
        my_name_ip = socket.gethostbyname_ex(socket.gethostname())[-1][0]

        url = MagicMock
        url.hostname = my_name_ip

        result = contains_my_ip_addr(url)
        self.assertTrue(result)

    def test__contains_my_ip_addr_nok(self):
        my_name_ip = socket.gethostbyname_ex(socket.gethostname())[-1][0]

        url = MagicMock
        url.hostname = my_name_ip + "1"

        result = contains_my_ip_addr(url)
        self.assertFalse(result)

    def test_filter_own_address_false_returns_input(self):
        my_name_ip = socket.gethostbyname_ex(socket.gethostname())[-1][0]
        url = MagicMock
        url.hostname = my_name_ip + "1" # not my address
        url.netloc = my_name_ip

        result = filter_own_address(url)

        self.assertEqual(my_name_ip, result)

    def test_filter_own_address_true_returns_empty_string(self):
        my_name_ip = socket.gethostbyname_ex(socket.gethostname())[-1][0]
        url = MagicMock
        url.hostname = my_name_ip  # my address

        result = filter_own_address(url)

        self.assertEqual("", result)

    @patch("requests.head")
    def test__check_resolvability_and_retrieve_meta_id_ok(self, MockRequest):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        my_name_ip = socket.gethostbyname_ex(socket.gethostname())[-1][0]
        bundle = provm.ProvBundle()
        remote_meta_bundle = bundle.add_namespace(
            "my_ns", "http://" + my_name_ip + ":80/api/v1/documents/meta/"
        )
        checker._main_activity = ProvActivity(identifier="my_ns:main_act", bundle=bundle,
                                              attributes={provm.PROV_TYPE: CPM_MAIN_ACTIVITY,
                                                          CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle["meta_id"]})
        response = Response()
        response.url = remote_meta_bundle["meta_id"].uri
        MockRequest.return_value = response

        result = checker._check_resolvability_and_retrieve_meta_id()

        self.assertEqual("meta_id", result)
        MockRequest.assert_called_with(response.url)

    @patch("requests.head")
    def test__check_resolvability_and_retrieve_meta_id_nok_missing_meta_bundle_id(self, MockRequest):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        my_name_ip = socket.gethostbyname_ex(socket.gethostname())[-1][0]
        bundle = provm.ProvBundle()
        remote_meta_bundle = bundle.add_namespace(
            "my_ns", "http://" + my_name_ip + ":80/api/v1/documents/meta/"
        )
        checker._main_activity = ProvActivity(identifier="my_ns:main_act", bundle=bundle,
                                              attributes={provm.PROV_TYPE: CPM_MAIN_ACTIVITY})
        response = Response()
        response.url = remote_meta_bundle["meta_id"].uri
        MockRequest.return_value = response

        self.assertRaises(DocumentError, checker._check_resolvability_and_retrieve_meta_id)

    @patch("requests.head")
    @patch("provenance.validators.contains_my_ip_addr")
    def test__check_resolvability_and_retrieve_meta_id_nok_does_meta_bundle_does_not_contain_my_ip(self, MockContainsIp,
                                                                                                   MockRequest):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        my_name_ip = socket.gethostbyname_ex(socket.gethostname())[-1][0]
        bundle = provm.ProvBundle()
        remote_meta_bundle = bundle.add_namespace(
            "my_ns", "http://" + my_name_ip + ":80/api/v1/documents/meta/"
        )
        checker._main_activity = ProvActivity(identifier="my_ns:main_act", bundle=bundle,
                                              attributes={provm.PROV_TYPE: CPM_MAIN_ACTIVITY,
                                                          CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle["meta_id"]})
        MockContainsIp.return_value = False
        response = Response()
        response.url = remote_meta_bundle["meta_id"].uri
        MockRequest.return_value = response

        self.assertRaises(DocumentError, checker._check_resolvability_and_retrieve_meta_id)

    @patch("requests.head")
    def test__check_resolvability_and_retrieve_meta_id_nok_meta_substring_not_in_meta_uri(self, MockRequest):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        my_name_ip = socket.gethostbyname_ex(socket.gethostname())[-1][0]
        bundle = provm.ProvBundle()
        remote_meta_bundle = bundle.add_namespace(
            "my_ns", "http://" + my_name_ip + ":80/api/v1/documen/"
        )
        checker._main_activity = provm.ProvEntity(identifier="my_ns:main_act", bundle=bundle,
                                                  attributes={provm.PROV_TYPE: CPM_MAIN_ACTIVITY,
                                                              CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle[
                                                                  "meta_id"]})
        response = Response()
        response.url = remote_meta_bundle["meta_id"].uri
        MockRequest.return_value = response

        self.assertRaises(DocumentError, checker._check_resolvability_and_retrieve_meta_id)

    def test__retrieve_main_activity_ok(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        main_act_bundle_ns = bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        main_act = bundle.activity(identifier="my_ns:main_act", other_attributes={provm.PROV_TYPE: CPM_MAIN_ACTIVITY,
                                                                                  CPM_REFERENCED_META_BUNDLE_ID:
                                                                                      main_act_bundle_ns["meta_id"]})
        checker._prov_bundle = bundle

        result = checker._retrieve_main_activity()

        self.assertEqual(main_act, result)

    def test__retrieve_main_activity_nok_more_main_activities(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        main_act_bundle_ns = bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        main_act = bundle.activity(identifier="my_ns:main_act", other_attributes={provm.PROV_TYPE: CPM_MAIN_ACTIVITY,
                                                                                  CPM_REFERENCED_META_BUNDLE_ID:
                                                                                      main_act_bundle_ns["meta_id"]})
        bundle.activity(identifier="my_ns:main_act2", other_attributes={provm.PROV_TYPE: CPM_MAIN_ACTIVITY,
                                                                        CPM_REFERENCED_META_BUNDLE_ID:
                                                                            main_act_bundle_ns[
                                                                                "meta_id"]})
        checker._prov_bundle = bundle

        self.assertRaises(DocumentError, checker._retrieve_main_activity)

    def test__retrieve_main_activity_nok_no_main_activities(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        main_act_bundle_ns = bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        main_act = bundle.activity(identifier="my_ns:main_act", other_attributes={  # missing prov type
            CPM_REFERENCED_META_BUNDLE_ID: main_act_bundle_ns["meta_id"]})
        checker._prov_bundle = bundle

        self.assertRaises(DocumentError, checker._retrieve_main_activity)

    def test__retrieve_connectors_from_graph(self):
        backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector, maa2, prev_cpm_bundle_info, prev_meta_bundle_info, _ = helpers.create_prov_cpm_basic()

        doc2, backbone_parts2, self.next_meta_bundle_id, self.next_cpm_bundle_info = helpers.create_cpm_provenance_with_backward_connector(
            document_with_backwards_connector,
            bundle_with_backwards_conn, "_",
            prev_cpm_bundle_info=prev_cpm_bundle_info,
            prev_meta_bundle_info=prev_meta_bundle_info,
            sender_org_name="hospital_org",
            main_activity_attributes=maa2,
            backward_connector_attributes=backward_connector_attributes)

        data = doc2.serialize()

        data_b64 = base64.b64encode(data.encode())
        checker = InputGraphChecker(data_b64, "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/",
                                    self.prov_validator)
        bundle_test = None
        for bundle in doc2.bundles:
            bundle_test = bundle
        checker._prov_bundle = bundle_test

        fw_connectors, bw_connectors = checker._retrieve_connectors_from_graph()
        self.assertEqual(1, len(fw_connectors))
        self.assertEqual(1, len(bw_connectors))
        self.assertEqual(CPM_FORWARD_CONNECTOR, first(fw_connectors[0].get_attribute(provm.PROV_TYPE)))
        self.assertEqual(CPM_BACKWARD_CONNECTOR, first(bw_connectors[0].get_attribute(provm.PROV_TYPE)))

    @patch("provenance.is_backbone_entity_strategies.IsBackboneStrategyOriginal.is_backbone_element")
    def test__retrieve_connectors_from_graph_2_decides_depending_on_attributes(self, MockIsBackbone):
        MockIsBackbone.return_value = True
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        main_act_bundle = bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        fw_conn = bundle.entity("my_ns:fw_con_1", other_attributes={provm.PROV_TYPE: CPM_FORWARD_CONNECTOR})
        bw_conn = bundle.entity("my_ns:bb_con_1", other_attributes={provm.PROV_TYPE: CPM_BACKWARD_CONNECTOR})
        not_conn = bundle.entity("my_ns:some_entity")

        checker._prov_bundle = bundle

        fw_connectors, bw_connectors = checker._retrieve_connectors_from_graph()
        self.assertEqual(1, len(fw_connectors))
        self.assertEqual(1, len(bw_connectors))
        self.assertEqual(fw_conn, fw_connectors[0])
        self.assertEqual(bw_conn, bw_connectors[0])

    @patch("provenance.is_backbone_entity_strategies.IsBackboneStrategyOriginal.is_backbone_element")
    def test__retrieve_connectors_from_graph_2_decides_depending_on_attributes_and_whether_belongs_to_backbone(self,
                                                                                                               MockIsBackbone):
        MockIsBackbone.side_effect = [True, False, True]
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        fw_conn = bundle.entity("my_ns:fw_con_1", other_attributes={provm.PROV_TYPE: CPM_FORWARD_CONNECTOR})
        bundle.entity("my_ns:bb_con_1", other_attributes={provm.PROV_TYPE: CPM_BACKWARD_CONNECTOR})
        bundle.entity("my_ns:some_entity")

        checker._prov_bundle = bundle

        fw_connectors, bw_connectors = checker._retrieve_connectors_from_graph()
        self.assertEqual(1, len(fw_connectors))
        self.assertEqual(0, len(bw_connectors))
        self.assertEqual(fw_conn, fw_connectors[0])

    def test_check_connectors_and_hash_no_cpm_attribute(self):
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        connector = bundle.entity(identifier="my_ns:id", other_attributes={})

        result = ping_connectors_and_check_hash(connector)

        self.assertIsNone(result)

    @patch("requests.head")
    @patch("requests.get")
    def test_check_connectors_and_hash_all_resolvable_except_hash(self, MockRequestGet, MockRequestHead):
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        main_act_bundle_ns = bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        connector = bundle.entity(identifier="my_ns:id", other_attributes={
            CPM_REFERENCED_BUNDLE_ID: main_act_bundle_ns["bundle_id"],
            CPM_REFERENCED_META_BUNDLE_ID: main_act_bundle_ns["meta_id"]
        })
        response = Response()
        response.url = "/api/v1/documents/meta/meta_id"
        response.status_code = 200
        MockRequestHead.return_value = response

        response_nok = Response()
        response_nok.status_code = 400
        MockRequestGet.return_value = response_nok

        result = ping_connectors_and_check_hash(connector)

        self.assertEqual(result, (response, response, True))
        MockRequestGet.assert_called_with(main_act_bundle_ns["bundle_id"].uri)
        calls = [call(main_act_bundle_ns["bundle_id"].uri), call(main_act_bundle_ns["meta_id"].uri)]
        MockRequestHead.assert_has_calls(calls)

    @patch("requests.head")
    @patch("requests.get")
    def test_check_connectors_and_hash_all_resolvable(self, MockRequestGet, MockRequestHead):
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        main_act_bundle_ns = bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        connector = bundle.entity(identifier="my_ns:id", other_attributes={
            CPM_REFERENCED_BUNDLE_ID: main_act_bundle_ns["bundle_id"],
            CPM_REFERENCED_META_BUNDLE_ID: main_act_bundle_ns["meta_id"],
            CPM_HASH_ALG: "SHA256",
            CPM_REFERENCED_BUNDLE_HASH_VALUE: "hash_value"
        })
        response = Response()
        response.url = "/api/v1/documents/meta/meta_id"
        response.status_code = 200
        MockRequestHead.return_value = response

        response_get_ok = Response()
        response_get_ok.status_code = 200
        response_get_ok._content = json.dumps({
            "token": {"data": {"documentDigest": "hash_value", "additionalData": {"hashFunction": "SHA256"}}}})
        MockRequestGet.return_value = response_get_ok

        result = ping_connectors_and_check_hash(connector)

        self.assertEqual(result, (response, response, True))
        MockRequestGet.assert_called_with(main_act_bundle_ns["bundle_id"].uri)
        calls = [call(main_act_bundle_ns["bundle_id"].uri), call(main_act_bundle_ns["meta_id"].uri)]
        MockRequestHead.assert_has_calls(calls)

    @patch("requests.head")
    @patch("requests.get")
    def test_check_connectors_and_hash_all_resolvable_wrong_hash(self, MockRequestGet, MockRequestHead):
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        main_act_bundle_ns = bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        connector = bundle.entity(identifier="my_ns:id", other_attributes={
            CPM_REFERENCED_BUNDLE_ID: main_act_bundle_ns["bundle_id"],
            CPM_REFERENCED_META_BUNDLE_ID: main_act_bundle_ns["meta_id"],
            CPM_HASH_ALG: "SHA256",
            CPM_REFERENCED_BUNDLE_HASH_VALUE: "hash_value"
        })
        response = Response()
        response.url = "/api/v1/documents/meta/meta_id"
        response.status_code = 200
        MockRequestHead.return_value = response

        response_get_ok = Response()
        response_get_ok.status_code = 200
        response_get_ok._content = json.dumps({
            # changed hash value
            "token": {"data": {"documentDigest": "hash_value_wrong", "additionalData": {"hashFunction": "SHA256"}}}})
        MockRequestGet.return_value = response_get_ok

        result = ping_connectors_and_check_hash(connector)

        self.assertEqual(result, (response, response, False))
        MockRequestGet.assert_called_with(main_act_bundle_ns["bundle_id"].uri)
        calls = [call(main_act_bundle_ns["bundle_id"].uri), call(main_act_bundle_ns["meta_id"].uri)]
        MockRequestHead.assert_has_calls(calls)

    @patch("requests.head")
    @patch("requests.get")
    def test_check_connectors_and_hash_all_resolvable_wrong_hash_alg(self, MockRequestGet, MockRequestHead):
        bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))
        main_act_bundle_ns = bundle.add_namespace(
            "my_ns", "http://this_storage:80/api/v1/documens/"
        )
        connector = bundle.entity(identifier="my_ns:id", other_attributes={
            CPM_REFERENCED_BUNDLE_ID: main_act_bundle_ns["bundle_id"],
            CPM_REFERENCED_META_BUNDLE_ID: main_act_bundle_ns["meta_id"],
            CPM_HASH_ALG: "SHA256",
            CPM_REFERENCED_BUNDLE_HASH_VALUE: "hash_value"
        })
        response = Response()
        response.url = "/api/v1/documents/meta/meta_id"
        response.status_code = 200
        MockRequestHead.return_value = response

        response_get_ok = Response()
        response_get_ok.status_code = 200
        response_get_ok._content = json.dumps({
            # changed hash algorithm
            "token": {"data": {"documentDigest": "hash_value", "additionalData": {"hashFunction": "SHA"}}}})
        MockRequestGet.return_value = response_get_ok

        result = ping_connectors_and_check_hash(connector)

        self.assertEqual(result, (response, response, False))
        MockRequestGet.assert_called_with(main_act_bundle_ns["bundle_id"].uri)
        calls = [call(main_act_bundle_ns["bundle_id"].uri), call(main_act_bundle_ns["meta_id"].uri)]
        MockRequestHead.assert_has_calls(calls)

    @patch("provenance.validators.contains_my_ip_addr")
    @patch("concurrent.futures.as_completed")
    def test__check_connectors_references_ok_returns_true(self, MockContainsIp, MockFutures):
        mock_future1 = MagicMock()
        response_get_ok = Response()
        response_get_ok.status_code = 200
        mock_future1.result.return_value = response_get_ok, response_get_ok, True
        connector1 = provm.ProvEntity(identifier=QualifiedName(Namespace("sample", "http://sample#"), "id1"),
                                      bundle=provm.ProvBundle())

        mock_future2 = MagicMock()
        mock_future2.result.return_value = None

        futures = {mock_future1: connector1, mock_future2: None}
        MockFutures.return_value = futures.keys()

        result = _check_connectors_references(futures)

        self.assertTrue(result[0])

    @patch("concurrent.futures.as_completed")
    def test__check_connectors_references_nok_response1_returns_false_and_message(self, MockFutures):
        mock_future1 = MagicMock()
        response_get_ok = Response()
        response_get_ok.status_code = 200
        response_get_nok = Response()
        response_get_nok.status_code = 400
        mock_future1.result.return_value = response_get_nok, response_get_ok, True
        connector1 = provm.ProvEntity(identifier=QualifiedName(Namespace("sample", "http://sample#"), "id1"),
                                      bundle=provm.ProvBundle())

        mock_future2 = MagicMock()
        mock_future2.result.return_value = None

        futures = {mock_future1: connector1, mock_future2: None}
        MockFutures.return_value = futures.keys()

        result = _check_connectors_references(futures)

        self.assertFalse(result[0])
        self.assertEqual(f"Referenced bundle URI of connector [{connector1.identifier.localpart}] not found.",
                         result[1])

    @patch("concurrent.futures.as_completed")
    def test__check_connectors_references_nok_response2_returns_false_and_message(self, MockFutures):
        mock_future1 = MagicMock()
        response_get_ok = Response()
        response_get_ok.status_code = 200
        response_get_nok = Response()
        response_get_nok.status_code = 400
        mock_future1.result.return_value = response_get_ok, response_get_nok, True
        connector1 = provm.ProvEntity(identifier=QualifiedName(Namespace("sample", "http://sample#"), "id1"),
                                      bundle=provm.ProvBundle())

        mock_future2 = MagicMock()
        mock_future2.result.return_value = None

        futures = {mock_future1: connector1, mock_future2: None}
        MockFutures.return_value = futures.keys()

        result = _check_connectors_references(futures)

        self.assertFalse(result[0])
        self.assertEqual(f"Referenced meta bundle URI of connector [{connector1.identifier.localpart}] not found.",
                         result[1])

    @patch("concurrent.futures.as_completed")
    def test__check_connectors_references_nok_hash_returns_false_and_message(self, MockFutures):
        mock_future1 = MagicMock()
        response_get_ok = Response()
        response_get_ok.status_code = 200
        mock_future1.result.return_value = response_get_ok, response_get_ok, False
        connector1 = provm.ProvEntity(identifier=QualifiedName(Namespace("sample", "http://sample#"), "id1"),
                                      bundle=provm.ProvBundle())

        mock_future2 = MagicMock()
        mock_future2.result.return_value = None

        futures = {mock_future1: connector1, mock_future2: None}
        MockFutures.return_value = futures.keys()

        result = _check_connectors_references(futures)

        self.assertFalse(result[0])
        self.assertEqual(f"Hash of bundle [{connector1.identifier.localpart}] has wrong value.",
                         result[1])

    @patch("provenance.validators.ping_connectors_and_check_hash")
    @patch("provenance.validators.contains_my_ip_addr")
    def test__process_bundle_references_ok(self, MockContainsMyIP, MockPingConnectors):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        response_get_ok = Response()
        response_get_ok.status_code = 200
        MockPingConnectors.return_value = response_get_ok, response_get_ok, True

        fw_conns = ["connector1"]
        checker._forward_connectors = fw_conns
        bw_conns = ["connector2"]
        checker._backward_connectors = bw_conns

        result = checker._process_bundle_references()

        self.assertTrue(result[0])
        calls = [call("connector2"), call("connector1")]
        MockPingConnectors.assert_has_calls(calls)

    @patch("provenance.validators.ping_connectors_and_check_hash")
    @patch("provenance.validators._check_connectors_references")
    def test__process_bundle_references_nok_response(self, MockCheckReferences, MockPingConnectors):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        MockCheckReferences.return_value = False, "message"

        fw_conns = ["connector1"]
        checker._forward_connectors = fw_conns
        bw_conns = ["connector2"]
        checker._backward_connectors = bw_conns

        result = checker._process_bundle_references()

        self.assertFalse(result[0])
        self.assertEqual("message", result[1])
        calls = [call("connector2"), call("connector1")]
        MockPingConnectors.assert_has_calls(calls)

    @patch("provenance.validators.ping_connectors_and_check_hash")
    @patch("provenance.validators._check_connectors_references")
    def test__process_bundle_references_nok_response_2(self, MockCheckReferences, MockPingConnectors):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        MockCheckReferences.side_effect = [(True, "nothing"), (False, "message")]

        fw_conns = ["connector1"]
        checker._forward_connectors = fw_conns
        bw_conns = ["connector2"]
        checker._backward_connectors = bw_conns

        result = checker._process_bundle_references()

        self.assertFalse(result[0])
        self.assertEqual("message", result[1])
        calls = [call("connector2"), call("connector1")]
        MockPingConnectors.assert_has_calls(calls)

    def test_get_document_ok_returns_document(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        checker._prov_document = "doc"

        result = checker.get_document()

        self.assertEqual("doc", result)

    def test_get_document_nok_throws_error(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)

        with self.assertRaises(AssertionError):
            checker.get_document()

    def test_get_bundle_id_ok_returns_id(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        checker._prov_bundle = checker._prov_bundle = provm.ProvBundle(identifier=QualifiedName(NAMESPACES["meta"], "test_bundle_id"))

        result = checker.get_bundle_id()

        self.assertEqual("test_bundle_id", result)

    def test_get_bundle_id_nok_throws_error(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)

        with self.assertRaises(AssertionError):
            checker.get_bundle_id()

    def test_get_meta_provenance_id_ok_returns_id(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        checker._meta_provenance_id = "id"

        result = checker.get_meta_provenance_id()

        self.assertEqual("id", result)

    def test_get_meta_provenance_id_nok_throws_error(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)

        with self.assertRaises(AssertionError):
            checker.get_meta_provenance_id()

    def test_get_forward_connectors_ok_returns_conns(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        checker._processed_forward_connectors = [1]

        result = checker.get_forward_connectors()

        self.assertEqual([1], result)

    def test_get_forward_connectors_nok_throws_error(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)

        with self.assertRaises(AssertionError):
            checker.get_forward_connectors()

    def test_get_backward_connectors_ok_returns_conns(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)
        checker._processed_backward_connectors = [1]

        result = checker.get_backward_connectors()

        self.assertEqual([1], result)

    def test_get_backward_connectors_nok_throws_error(self):
        checker = InputGraphChecker("", "json",
                                    "http://provenance-storage-provider.com:8000/api/v1/organizations/UniParis/bundle",
                                    self.prov_validator)

        with self.assertRaises(AssertionError):
            checker.get_backward_connectors()
