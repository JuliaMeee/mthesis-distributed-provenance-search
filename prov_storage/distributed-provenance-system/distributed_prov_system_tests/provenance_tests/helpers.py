from datetime import datetime, timezone

import jcs
from cryptography.hazmat.primitives import hashes
from prov.constants import PROV_TYPE, PROV_ATTR_SPECIFIC_ENTITY, PROV_ATTR_GENERAL_ENTITY, PROV_START, PROV_END, \
    PROV_ATTR_TIME, PROV_ATTR_STARTTIME, PROV_ATTR_ENDTIME, PROV_ATTR_INFLUENCEE, PROV_ATTR_INFLUENCER, \
    PROV_ATTR_INFORMED, PROV_ATTR_INFORMANT, PROV_ATTR_ALTERNATE1, PROV_ATTR_ALTERNATE2, PROV_ATTR_ENTITY, \
    PROV_ATTR_ACTIVITY, PROV_ATTR_GENERATED_ENTITY, PROV_ATTR_USED_ENTITY, PROV_ATTR_AGENT, PROV_ATTR_DELEGATE, \
    PROV_ATTR_RESPONSIBLE, PROV_ATTR_COLLECTION, PROV_ATTR_STARTER, PROV_ATTR_ENDER, PROV_LOCATION
from prov.identifier import Namespace, QualifiedName
from prov.model import ProvDocument, ProvActivity, first, ProvBundle, ProvEntity, ProvAgent, ProvSpecialization, \
    ProvInfluence, ProvCommunication, ProvAlternate, ProvGeneration, ProvUsage, ProvStart, ProvEnd, ProvInvalidation, \
    ProvDerivation, ProvAttribution, ProvAssociation, ProvDelegation, ProvMention, ProvMembership
from prov.serializers import provjson

from constants import CPM_MAIN_ACTIVITY, CPM_REFERENCED_META_BUNDLE_ID, DCT_HAS_PART, CPM_FORWARD_CONNECTOR, \
    CPM_BACKWARD_CONNECTOR, CPM_REFERENCED_BUNDLE_ID, CPM_RECEIVER_AGENT, CPM_SENDER_AGENT, CPM_ID, CPM, DCT, \
    CPM_REFERENCED_BUNDLE_HASH_VALUE, CPM_HASH_ALG, CPM_CONTACT_ID_PID


class ExistentialVariablesGenerator:
    def __init__(self, existential_variable_prefix):
        self.entity_id = 0
        self.activity_id = 0
        self.plan_id = 0
        self.existential_variable_prefix = existential_variable_prefix

    def get_entity_id(self):
        self.entity_id += 1
        return f"{self.existential_variable_prefix}_existential_entity_{self.entity_id}"

    def get_activity_id(self):
        self.activity_id += 1
        return f"{self.existential_variable_prefix}_existential_activity_{self.activity_id}"

    def get_plan_id(self):
        self.plan_id += 1
        return f"{self.existential_variable_prefix}_existential_plan_{self.plan_id}"


def create_normalized_graph(existential_variable_prefix):
    document = ProvDocument()

    document.set_default_namespace('http://example.org/0/')
    document.add_namespace('ex1', 'http://example.org/1/')
    document.add_namespace('ex2', 'http://example.org/2/')

    bundle = document.bundle('b001')
    bundle.add_namespace("example", 'http://example.org/2/')
    existential_var_generator = ExistentialVariablesGenerator(existential_variable_prefix)
    create_domain_prov(bundle, existential_var_generator, "ex1")

    return document


def create_main_act_attributes(storage_name):
    meta_ns = Namespace(
        "meta", f"http://prov-storage-{storage_name}:8000/api/v1/documents/meta/"
    )
    timestamp = datetime.now().timestamp()
    main_activity_attributes_ok = {
        PROV_TYPE: CPM_MAIN_ACTIVITY,
        CPM_REFERENCED_META_BUNDLE_ID: meta_ns[
            # id of meta bundle - used when requesting meta provenance
            f"test_{timestamp}_bundle_meta"
        ],
    }
    main_act_attr2 = {
        PROV_TYPE: CPM_MAIN_ACTIVITY,
        CPM_REFERENCED_META_BUNDLE_ID: meta_ns[
            f"test_{timestamp}_bundle_w_backward_connector_meta"
        ],
    }
    return main_activity_attributes_ok, main_act_attr2, timestamp


def prepare_attributes_for_doc_w_backward_connectors(bundle, prev_cpm_bundle_info, prev_meta_bundle_info, timestamp):
    document_with_backwards_connector = ProvDocument()
    bundle_namespace_with_backwards_connector = Namespace(
        "pathology",
        f"http://prov-storage-pathology:8000/api/v1/organizations/hospital_org/documents/",
    )
    bundle_identifier = f"test_{timestamp}_bundle_w_backward_connector"
    bundle_with_backwards_conn = document_with_backwards_connector.bundle(
        bundle_namespace_with_backwards_connector[bundle_identifier])
    remote_bundle = bundle.add_namespace("remote_bundle", prev_cpm_bundle_info[0])
    remote_meta_bundle = bundle.add_namespace(
        "remote_meta_bundle", prev_meta_bundle_info[0]
    )
    backward_connector_attributes = {
        PROV_TYPE: CPM_BACKWARD_CONNECTOR,
        CPM_REFERENCED_BUNDLE_ID: remote_bundle[prev_cpm_bundle_info[1]],
        CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle[
            prev_meta_bundle_info[1]
        ],
        CPM_REFERENCED_BUNDLE_HASH_VALUE: prev_cpm_bundle_info[2],
        # a hash value of a PROV bundle referenced by the connector in which the attribute is present
        CPM_HASH_ALG: "SHA256"
    }
    return bundle_with_backwards_conn, document_with_backwards_connector, backward_connector_attributes, remote_meta_bundle


def create_domain_prov_complete_namespaces(bundle, existential_variable_generator, org_prefix):
    # add also attribute because otherwise it would be taken as part of backbone by algorithm, existential variables have their namespace
    # because xml serializer needs it for deserialization
    bundle.add_namespace(Namespace("existential", "http://existential-variable.com"))
    placeholder_time = datetime(2025, 1, 7, 15, 8, 24, 78915)
    entity = bundle.entity(f'{org_prefix}:e001', {PROV_LOCATION: "result"})
    bundle.alternate(entity, entity)  # Inference 16
    entity2 = bundle.entity(f'{org_prefix}:e002', [])
    bundle.alternate(entity2, entity2)  # Inference 16
    bundle.invalidation(entity, "existential:" + existential_variable_generator.get_activity_id(),
                        time=placeholder_time,
                        identifier=f'{org_prefix}:invalid2',
                        other_attributes=[])
    bundle.invalidation(entity2, "existential:" + existential_variable_generator.get_activity_id(),
                        time=placeholder_time,
                        identifier=f'{org_prefix}:invalid3',
                        other_attributes=[])
    activity = bundle.activity(identifier=f'{org_prefix}:act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                               endTime=datetime(2025, 1, 7, 15, 9, 24, 78915),
                               other_attributes=[])
    bundle.start(activity, trigger="existential:" + existential_variable_generator.get_entity_id(),
                 starter="existential:" + existential_variable_generator.get_activity_id(),
                 time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                 identifier=f'{org_prefix}:startact3', other_attributes=[])
    bundle.end(activity, "existential:" + existential_variable_generator.get_entity_id(),
               ender="existential:" + existential_variable_generator.get_activity_id(),
               time=datetime(2025, 1, 7, 15, 9, 24, 78915),
               identifier=f'{org_prefix}:endact2', other_attributes=[])
    bundle.generation(entity, activity, identifier=f'{org_prefix}:gen0', time=datetime(2025, 1, 6, 15, 8, 24, 78915),
                      other_attributes=[])  # Inference 9, 10
    bundle.influence(entity, activity, identifier=f'{org_prefix}:influence6',
                     other_attributes=[])  # wasInfluencedBy, Inference 15


def create_domain_prov(bundle, existential_variable_generator, org_prefix):
    existential_namestace = bundle.add_namespace(Namespace("existential", "http://existential-variable.com"))
    agent = bundle.agent('example:ag001', [])
    agent2 = bundle.agent("example:ag002", [])
    # add also attribute because otherwise it would be taken as part of backbone by algorithm
    entity = bundle.entity(f'{org_prefix}:e001', {PROV_LOCATION: "result"})
    bundle.alternate(entity, entity)  # Inference 16
    entity2 = bundle.entity(f'{org_prefix}:e002', [])
    bundle.alternate(entity2, entity2)  # Inference 16
    # again - because of alg
    entity3 = bundle.entity("example:e003", {PROV_LOCATION: "result"})
    bundle.alternate(entity3, entity3)  # Inference 16
    entity4 = bundle.entity("example:e004", [])
    bundle.alternate(entity4, entity4)  # Inference 16
    # Inference 17
    bundle.alternate(entity2, entity3)
    bundle.alternate(entity3, entity2)  # Inference 18
    bundle.alternate(entity3, entity4)
    bundle.alternate(entity4, entity3)  # Inference 18
    bundle.alternate(entity2, entity4)
    bundle.alternate(entity4, entity2)  # Inference 18

    collection = bundle.collection("example:collection1", [])
    activity = bundle.activity(identifier='example:act001', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                               endTime=datetime(2025, 1, 7, 15, 8, 24, 78915),
                               other_attributes=[])
    activity2 = bundle.activity(identifier='example:act002', startTime=datetime(2025, 1, 5, 15, 8, 24, 78915),
                                endTime=datetime(2025, 1, 7, 15, 9, 24, 78915),
                                other_attributes=[])
    bundle.communication(activity2, activity, "example:acttoact2", other_attributes=[])  # wasInformedBy
    bundle.influence(activity2, activity, identifier="example:influence1",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    # Inference 5 / 6 applied
    bundle.wasGeneratedBy(entity4, activity, identifier="example:gen2", time=datetime(2025, 1, 5, 16, 8, 24, 78915),
                          other_attributes=[])
    bundle.influence(entity4, activity, identifier="example:influence2",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    bundle.usage(activity2, entity4, identifier="example:usage2", time=datetime(2025, 1, 5, 17, 8, 24, 78915),
                 other_attributes=[])
    bundle.influence(activity2, entity4, identifier="example:influence3",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    # Inference 8 applied
    bundle.start(activity2, trigger=entity, starter=activity, time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                 identifier="example:startact2", other_attributes=[])  # wasStartedBy, Inference 9
    bundle.influence(activity2, entity, identifier="example:influence4",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    bundle.start(activity, trigger="existential:" + existential_variable_generator.get_entity_id(),
                 starter="example:ex_act1",
                 time=datetime(2025, 1, 5, 15, 8, 24, 78915),
                 identifier="example:startact3", other_attributes=[])
    bundle.end(activity2, "existential:" + existential_variable_generator.get_entity_id(),
               ender="existential:" + existential_variable_generator.get_activity_id(),
               time=datetime(2025, 1, 7, 15, 9, 24, 78915),
               identifier="example:endact2", other_attributes=[])  # wasEndedBy
    bundle.end(activity, entity, ender=activity, time=datetime(2025, 1, 7, 15, 8, 24, 78915),
               identifier="example:endact3", other_attributes=[])  # Inference 10
    bundle.influence(activity, entity, identifier="example:influence5",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    bundle.invalidation(entity2, "existential:" + existential_variable_generator.get_activity_id(),
                        time=datetime(2025, 1, 8, 14, 8, 24, 78915),
                        identifier="example:invalid1",
                        other_attributes=[])  # wasInvalidatedBy
    # Inference 7 applied
    bundle.invalidation(entity, "existential:" + existential_variable_generator.get_activity_id(),
                        time=datetime(2025, 1, 10, 15, 8, 24, 78915),
                        identifier="example:invalid2",
                        other_attributes=[])
    bundle.invalidation(entity3, "existential:" + existential_variable_generator.get_activity_id(),
                        time=datetime(2025, 1, 7, 18, 8, 24, 78915),
                        identifier="example:invalid3",
                        other_attributes=[])
    bundle.invalidation(entity4, "existential:" + existential_variable_generator.get_activity_id(),
                        time=datetime(2025, 1, 8, 15, 8, 24, 78915),
                        identifier="example:invalid4",
                        other_attributes=[])
    # no Inference 15 - existential variable
    bundle.generation(entity, activity, identifier="example:gen0",
                      time=datetime(2025, 1, 7, 15, 9, 24, 78915),
                      other_attributes=[])  # Inference 9, 10
    bundle.influence(entity, activity, identifier="example:influence6",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    bundle.generation(entity3, "existential:" + existential_variable_generator.get_activity_id(),
                      identifier="example:gen3",
                      time=datetime(2025, 1, 6, 19, 9, 24, 78915), other_attributes=[])
    # Inference 11
    generation = bundle.generation(entity2, activity2, identifier="example:gen1",
                                   time=datetime(2025, 1, 7, 16, 8, 24, 78915),
                                   other_attributes=[])  # wasGeneratedBy, Inference 13
    bundle.influence(entity2, activity2, identifier="example:influence7",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    usage = bundle.usage(activity2, entity, datetime(2025, 1, 7, 17, 8, 24, 78915), identifier="example:usage1",
                         other_attributes=[])  # used
    bundle.influence(activity2, entity, identifier="example:influence8",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    bundle.derivation(entity2, entity, activity2, generation, usage, identifier="example:derivation0",
                      other_attributes=[])  # wasDerivedFrom, if no activity, generation and usage are non expandable
    bundle.influence(entity2, entity, identifier="example:influence9",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    bundle.attribution(entity2, agent, identifier="example:attribution1",
                       other_attributes=[])  # wasAttributedTo, Inference 13
    bundle.influence(entity2, agent, identifier="example:influence10",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    bundle.association(activity2, agent, identifier="example:association1",
                       plan="existential:" + existential_variable_generator.get_plan_id(),
                       other_attributes=[])  # wasAssociatedWith - Inference 13, Inference 14
    bundle.influence(activity2, agent, identifier="example:influence11",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    # Inference 14
    bundle.delegation(agent2, agent, activity2, identifier="example:delegation1",
                      other_attributes=[])  # actedOnBehalfOf
    bundle.influence(agent2, agent, identifier="example:influence12",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    bundle.association(activity2, agent2, identifier="example:association2",
                       plan="existential:" + existential_variable_generator.get_plan_id(),
                       other_attributes=[])  # wasAssociatedWith
    bundle.influence(activity2, agent2, identifier="example:influence13",
                     other_attributes=[])  # wasInfluencedBy, Inference 15
    # Inference 19
    bundle.specialization(entity2, entity)  # specializationOf
    entity2.add_attributes(entity.attributes)
    bundle.alternate(entity2, entity)  # Inference 20
    bundle.specialization(entity, entity3)
    entity.add_attributes(entity3.attributes)
    bundle.alternate(entity, entity3)  # Inference 20
    bundle.specialization(entity2, entity3)
    entity2.add_attributes(entity3.attributes)
    bundle.alternate(entity2, entity3)  # Inference 20
    bundle.membership(collection, entity3)  # hadMember


# storage_name - name of prov storage
# org name - name of organization registered in prov storage service
def create_cpm_provenance_basic(existential_prefix, storage_name, org_name, timestamp, main_activity_attributes,
                                forward_connector_attributes={PROV_TYPE: CPM_FORWARD_CONNECTOR},
                                bundle_suffix="", smaller_prov=False):
    document = ProvDocument()

    bundle_namespace = Namespace(
        storage_name,
        f"http://prov-storage-{storage_name}:8000/api/v1/organizations/{org_name}/documents/",
    )

    bundle_identifier = f"test_{timestamp}_bundle{bundle_suffix}"
    bundle = document.bundle(bundle_namespace[bundle_identifier])
    bundle.add_namespace("example", "http://example.com#")

    existential_variable_generator = ExistentialVariablesGenerator(existential_prefix)
    if smaller_prov:
        create_domain_prov_complete_namespaces(bundle,
                                               existential_variable_generator,
                                               storage_name)
    else:
        create_domain_prov(bundle,
                           existential_variable_generator,
                           storage_name)
    backbone_parts = []

    domain_activities = bundle.get_records(ProvActivity)

    main_activity = (
        create_main_activity(bundle, domain_activities,
                             main_activity_attributes, storage_name,
                             datetime(2025, 1, 4, 15, 8, 24, 78915),
                             datetime(2025, 1, 8, 16, 8, 24, 78915)))

    # add forward connector
    to_connect = f'{storage_name}:e001'
    sample_forward_connector = bundle.entity(
        identifier=f"{storage_name}:{to_connect}_connector",
        other_attributes=forward_connector_attributes,
    )
    add_general_forward_connector(bundle,
                                  main_activity, sample_forward_connector,
                                  to_connect, backbone_parts)
    meta_bundle_id = first(
        main_activity.get_attribute(CPM_REFERENCED_META_BUNDLE_ID)
    )
    backbone_parts.append(main_activity)

    # create hash value of bundle - needed for backwards connector later
    digest = hashes.Hash(hashes.SHA256())
    digest.update(jcs.canonicalize(provjson.encode_json_document(document)))
    hash_doc = digest.finalize().hex()

    return document, bundle, meta_bundle_id, backbone_parts, [bundle.identifier.namespace.uri,
                                                              bundle.identifier.localpart,
                                                              hash_doc], existential_variable_generator


def create_main_activity(bundle, domain_activities, main_activity_attributes,
                         storage_name, start_time, end_time):
    main_activity = bundle.activity(
        identifier=f"{storage_name}:test_main_activity",
        startTime=start_time,
        endTime=end_time.isoformat(" "),
        # main activity attributes as an argument of this function because of need to have different attributes
        # helps with testing unhappy path regarding main activity and "full" cpm with forward traceability
        other_attributes=main_activity_attributes
    )
    main_activity.add_attributes([
        (DCT_HAS_PART, str(domain_act.identifier))
        for domain_act in domain_activities
    ])
    return main_activity


def add_general_forward_connector(bundle, main_activity, sample_forward_connector,
                                  to_connect, backbone_parts, add_generation_from_main_activity = True, prev_connector = None):
    alt_connector = bundle.alternateOf(sample_forward_connector, sample_forward_connector)
    sample_ent = first(bundle.get_record(to_connect))
    bundle.specializationOf(sample_ent, sample_forward_connector)
    sample_ent.add_attributes(sample_forward_connector.attributes)
    if add_generation_from_main_activity:
        generation_connector = bundle.wasGeneratedBy(sample_forward_connector, main_activity,
                                                 datetime(2025, 1, 4, 15, 8, 24, 78915),
                                                 other_attributes=[])
        backbone_parts.append(generation_connector)
    else:
        deriv = bundle.wasDerivedFrom(sample_forward_connector, prev_connector)
        backbone_parts.append(deriv)
    backbone_parts.append(alt_connector)

    backbone_parts.append(sample_forward_connector)


def create_cpm_provenance_basic_without_fw_connector(existential_prefix, storage_name, org_name, timestamp,
                                                     main_activity_attributes,
                                                     bundle_suffix="", smaller_prov=False):
    document = ProvDocument()

    bundle_namespace = Namespace(
        storage_name,
        f"http://prov-storage-{storage_name}:8000/api/v1/organizations/{org_name}/documents/",
    )

    bundle_identifier = f"test_{timestamp}_bundle{bundle_suffix}"
    bundle = document.bundle(bundle_namespace[bundle_identifier])
    bundle.add_namespace("example", "http://example.com#")

    existential_variable_generator = ExistentialVariablesGenerator(existential_prefix)
    if smaller_prov:
        create_domain_prov_complete_namespaces(bundle,
                                               existential_variable_generator,
                                               storage_name)
    else:
        create_domain_prov(bundle,
                           existential_variable_generator,
                           storage_name)
    backbone_parts = []

    domain_activities = bundle.get_records(ProvActivity)
    main_activity = (
        create_main_activity(bundle, domain_activities,
                             main_activity_attributes,
                             storage_name,
                             datetime(2025, 1, 4, 15, 8, 24, 78915),
                             datetime(2025, 1, 8, 16, 8, 24, 78915)))

    meta_bundle_id = first(
        main_activity.get_attribute(CPM_REFERENCED_META_BUNDLE_ID)
    )
    backbone_parts.append(main_activity)

    # create hash value of bundle - needed for backwards connector later
    digest = hashes.Hash(hashes.SHA256())
    digest.update(jcs.canonicalize(provjson.encode_json_document(document)))
    hash_doc = digest.finalize().hex()

    return document, bundle, meta_bundle_id, backbone_parts, [bundle.identifier.namespace.uri,
                                                              bundle.identifier.localpart, hash_doc]


def create_cpm_provenance_basic_without_fw_connector_with_two_bw_connectors \
                (document, bundle, existential_prefix, storage_name,
                 main_activity_attributes,
                 sender_org_name_1,
                 backward_connector_attributes_1,
                 sender_org_name_2,
                 backward_connector_attributes_2):
    bundle.add_namespace("example", "http://example.com#")

    existential_variable_generator = ExistentialVariablesGenerator(existential_prefix)
    create_domain_prov(bundle,
                       existential_variable_generator,
                       storage_name)
    backbone_parts = []

    domain_activities = bundle.get_records(ProvActivity)

    main_activity = (
        create_main_activity(bundle, domain_activities,
                             main_activity_attributes,
                             storage_name,
                             datetime(2025, 1, 4, 15, 8, 24, 78915),
                             datetime(2025, 1, 8, 16, 8, 24, 78915)))

    meta_bundle_id = first(
        main_activity.get_attribute(CPM_REFERENCED_META_BUNDLE_ID)
    )

    # add backward connectors
    to_connect = f'hospital:e001'
    # redundant connector - main activity did not used it, second connector was derived from it
    sample_backward_connector = add_backward_connector(
        backward_connector_attributes_1, bundle, main_activity,
        sender_org_name_1, to_connect, remote_bundle_namespace="remote_bundle_1", backbone_parts=backbone_parts, with_usage=False)

    to_connect = f'hospital:e002'
    sample_backward_connector2 = add_backward_connector(
        backward_connector_attributes_2, bundle, main_activity,
        sender_org_name_2, to_connect, remote_bundle_namespace="remote_bundle_2", backbone_parts=backbone_parts)

    was_derived_backward_conn = bundle.derivation(sample_backward_connector2, sample_backward_connector)
    backbone_parts.append(main_activity)
    backbone_parts.append(was_derived_backward_conn)

    # create hash value of bundle - needed for backwards connector later
    digest = hashes.Hash(hashes.SHA256())
    digest.update(jcs.canonicalize(provjson.encode_json_document(document)))
    hash_doc = digest.finalize().hex()

    return document, bundle, meta_bundle_id, backbone_parts, [bundle.identifier.namespace.uri,
                                                              bundle.identifier.localpart, hash_doc]


def create_cpm_provenance_with_backward_connector(document, bundle, existential_prefix,
                                                  prev_cpm_bundle_info, prev_meta_bundle_info, sender_org_name,
                                                  main_activity_attributes,
                                                  backward_connector_attributes,
                                                  forward_connector_attributes={PROV_TYPE: CPM_FORWARD_CONNECTOR},
                                                  add_forward_connector_param=False, receiver_org_name=None):
    bundle.add_namespace("example", "http://example.com#")

    existential_variable_generator = ExistentialVariablesGenerator(existential_prefix)
    create_domain_prov(bundle, existential_variable_generator, "pathology")
    backbone_parts = []

    domain_activities = bundle.get_records(ProvActivity)

    main_activity = (
        create_main_activity(bundle, domain_activities, main_activity_attributes,
                             "pathology",
                             datetime(2025, 1, 5, 15, 8, 24, 78915),
                             datetime(2025, 1, 7, 15, 8, 27, 78915)))

    # add forward connector
    to_connect = "example:e003"  # different from backward connector
    sample_forward_connector = bundle.entity(
        identifier=f"pathology:{to_connect}_connector",
        other_attributes={PROV_TYPE: CPM_FORWARD_CONNECTOR},
    )
    add_general_forward_connector(bundle,
                                  main_activity, sample_forward_connector,
                                  to_connect, backbone_parts)

    # add backward connector
    to_connect = f'pathology:e001'
    # prev_cpm_bundle_info - first is uri, second localpart
    bundle.add_namespace("remote_bundle", prev_cpm_bundle_info[0])
    bundle.add_namespace(
        "remote_meta_bundle", prev_meta_bundle_info[0]
    )
    sample_backward_connector = add_backward_connector(
        backward_connector_attributes, bundle, main_activity, sender_org_name,
        to_connect, remote_bundle_namespace="remote_bundle", backbone_parts=backbone_parts)

    # add concrete forward connector
    if add_forward_connector_param:
        to_connect = "example:e003"
        sample_concrete_forward_connector = bundle.entity(
            identifier=f"pathology:{to_connect}_connector_s1",
            other_attributes=forward_connector_attributes,
        )
        add_forward_connector(backbone_parts, bundle, receiver_org_name, sample_concrete_forward_connector,
                              sample_forward_connector, to_connect)

    # add reference between forward, backward connector
    deriv = bundle.wasDerivedFrom(sample_forward_connector, sample_backward_connector)

    meta_bundle_id = first(
        main_activity.get_attribute(CPM_REFERENCED_META_BUNDLE_ID)
    )

    backbone_parts.append(main_activity)
    backbone_parts.append(deriv)

    # create hash value of bundle - needed for backwards connector later
    digest = hashes.Hash(hashes.SHA256())
    digest.update(jcs.canonicalize(provjson.encode_json_document(document)))
    hash_doc = digest.finalize().hex()

    return document, backbone_parts, meta_bundle_id, [bundle.identifier.namespace.uri,
                                                      bundle.identifier.localpart, hash_doc]


def add_forward_connector(backbone_parts, bundle,
                          receiver_org_name, sample_concrete_forward_connector, sample_forward_connector, to_connect):
    spec_bb = bundle.specializationOf(sample_concrete_forward_connector, sample_forward_connector)
    # add receiver agent
    receiver_agent = bundle.agent(
        identifier=f"{to_connect}_receiver_agent",
        other_attributes={
            PROV_TYPE: CPM_RECEIVER_AGENT,
            CPM_CONTACT_ID_PID: f"http://prov-storage-{receiver_org_name}:8000/api/v1/"
        },
    )
    attr_conn = bundle.attribution(
        sample_concrete_forward_connector, receiver_agent)
    backbone_parts.append(sample_concrete_forward_connector)
    backbone_parts.append(receiver_agent)
    backbone_parts.append(spec_bb)
    backbone_parts.append(attr_conn)
    return receiver_agent


def add_backward_connector(backward_connector_attributes, bundle, main_activity,
                           sender_org_name, to_connect, remote_bundle_namespace, backbone_parts, with_usage = True):
    sample_backward_connector = bundle.entity(
        # - cpm:hashAlg: a mandatory attribute
        identifier=f"{remote_bundle_namespace}:{to_connect}_sample_backwards_connector",
        other_attributes=backward_connector_attributes,
    )
    sample_ent = first(bundle.get_record(to_connect))
    bundle.specializationOf(sample_ent, sample_backward_connector)
    if with_usage:
        usage_conn = bundle.used(main_activity, sample_backward_connector, identifier="usage_main_act",
                                 time=datetime(2025, 1, 5, 18, 8, 24, 78915), other_attributes=[])
        backbone_parts.append(usage_conn)
    # add sender agent
    sender_agent = bundle.agent(
        identifier=f"{to_connect}_sender_agent",
        other_attributes={
            PROV_TYPE: CPM_SENDER_AGENT,
            # should be url with info about sender - now missing endpoint for info probably
            CPM_CONTACT_ID_PID: f"http://prov-storage-{sender_org_name}:8000/api/v1/"
        },
    )
    attr_conn = bundle.attribution(
        sample_backward_connector, sender_agent)
    backbone_parts.append(sample_backward_connector)
    backbone_parts.append(sender_agent)
    backbone_parts.append(attr_conn)
    return sample_backward_connector


def create_cpm_with_forward_connector(existential_prefix, storage_name, org_name, timestamp, main_activity_attributes,
                                      receiver_org_name,
                                      forward_connector_attributes,
                                      bundle_suffix="", add_second_forward_connector = False,
                                      second_forward_connector_attributes = None,
                                      second_receiver_org_name = None):  # with concrete forward connector with defined receiver
    document = ProvDocument()

    bundle_namespace = Namespace(
        storage_name,
        f"http://prov-storage-{storage_name}:8000/api/v1/organizations/{org_name}/documents/",
    )

    bundle_identifier = f"test_{timestamp}_bundle{bundle_suffix}"
    bundle = document.bundle(bundle_namespace[bundle_identifier])
    bundle.add_namespace("example", "http://example.com#")

    existential_variable_generator = ExistentialVariablesGenerator(existential_prefix)
    create_domain_prov(bundle,
                       existential_variable_generator,
                       storage_name)
    backbone_parts = []

    domain_activities = bundle.get_records(ProvActivity)

    main_activity = (
        create_main_activity(bundle, domain_activities, main_activity_attributes,
                             storage_name, datetime(2025, 1, 5, 15, 8, 24, 78915),
                             datetime(2025, 1, 7, 16, 8, 24, 78915)))

    # add forward connector
    to_connect = f'{storage_name}:e001'
    sample_general_forward_connector = bundle.entity(
        identifier=f"{storage_name}:{to_connect}_connector",
        other_attributes={PROV_TYPE: CPM_FORWARD_CONNECTOR},
    )
    add_general_forward_connector(bundle,
                                  main_activity, sample_general_forward_connector,
                                  to_connect, backbone_parts)
    meta_bundle_id = first(
        main_activity.get_attribute(CPM_REFERENCED_META_BUNDLE_ID)
    )

    # add concrete forward connector
    sample_concrete_forward_connector = bundle.entity(
        identifier=f"{storage_name}:{to_connect}_connector_s1",
        other_attributes=forward_connector_attributes,
    )
    receiver_agent = add_forward_connector(backbone_parts, bundle,
                                           receiver_org_name, sample_concrete_forward_connector,
                                           sample_general_forward_connector, to_connect)

    backbone_parts.append(main_activity)
    backbone_parts.append(sample_concrete_forward_connector)

    if add_second_forward_connector:
        # add forward connector
        to_connect = f'{storage_name}:e002'
        sample_general_forward_connector_2 = bundle.entity(
            identifier=f"{storage_name}:{to_connect}_connector",
            other_attributes={PROV_TYPE: CPM_FORWARD_CONNECTOR},
        )
        add_general_forward_connector(bundle,
                                      main_activity, sample_general_forward_connector_2,
                                      to_connect, backbone_parts, add_generation_from_main_activity=False,
                                      prev_connector=sample_general_forward_connector)

        # add concrete forward connector
        sample_concrete_forward_connector_2 = bundle.entity(
            identifier=f"{storage_name}:{to_connect}_connector_s1",
            other_attributes=second_forward_connector_attributes,
        )
        receiver_agent = add_forward_connector(backbone_parts, bundle,
                                               second_receiver_org_name, sample_concrete_forward_connector_2,
                                               sample_general_forward_connector_2, to_connect)

    # create hash value of bundle - needed for backwards connector later
    digest = hashes.Hash(hashes.SHA256())
    digest.update(jcs.canonicalize(provjson.encode_json_document(document)))
    hash = digest.finalize().hex()

    return document, bundle, meta_bundle_id, backbone_parts, [bundle.identifier.namespace.uri,
                                                              bundle.identifier.localpart, hash]


def create_prov_cpm_basic():
    storage_name = "provider"
    org_name = "org"
    main_activity_attributes_ok, maa2, timestamp = create_main_act_attributes(storage_name)
    document, bundle, meta_bundle_id, backbone_parts, prev_cpm_bundle_info, _ \
        = create_cpm_provenance_basic("_", storage_name, org_name, timestamp, main_activity_attributes_ok)
    prev_meta_bundle_info = [meta_bundle_id.namespace.uri, meta_bundle_id.localpart]
    (bundle_with_backwards_conn, document_with_backwards_connector,
     backward_connector_attributes, remote_meta_bundle) = prepare_attributes_for_doc_w_backward_connectors(
        bundle,
        prev_cpm_bundle_info,
        prev_meta_bundle_info,
        timestamp)
    return backward_connector_attributes, bundle_with_backwards_conn, document_with_backwards_connector, maa2, prev_cpm_bundle_info, prev_meta_bundle_info, remote_meta_bundle


class Helpers():
    pass
