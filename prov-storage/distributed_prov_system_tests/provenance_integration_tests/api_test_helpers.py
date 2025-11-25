import base64
import json
import random
import string
from abc import ABC
from datetime import datetime, timezone
from pathlib import Path

import jcs
import requests
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives._serialization import Encoding, PrivateFormat, NoEncryption
from cryptography.hazmat.primitives.asymmetric import ec
from prov.constants import PROV_TYPE
from prov.identifier import Namespace
from prov.model import ProvDocument
from prov.serializers import provjson

from helpers import *
from provenance.constants import CPM_MAIN_ACTIVITY, CPM_REFERENCED_META_BUNDLE_ID, CPM_BACKWARD_CONNECTOR, \
    CPM_REFERENCED_BUNDLE_ID, CPM_REFERENCED_BUNDLE_HASH_VALUE, CPM_HASH_ALG, CPM_FORWARD_CONNECTOR
from provenance_integration_tests.certificate_helpers import generate_certificate, parse_certificate

provenance_storage_hospital_url = "localhost:8001"
provenance_storage_pathology_url = "localhost:8002"
trusted_party_url = "localhost:8020"
provenance_storage_hospital_name = "hospital"
fqdn_hospital = "prov-storage-hospital:8000"
provenance_storage_pathology_name = "pathology"
fqdn_pathology = "prov-storage-pathology:8000"


def create_json_for_doc_storing(doc, doc_format="json", private_key_location='test_sign.pem',
                                different_format=False):
    """Creates json from data provided that should be sent to storage service to store component

    :param doc: component to be stored
    :param doc_format: format of document
    :param private_key_location: location where key for signing the component is saved (private key of organization that stores the component)
    :param different_format: should be true if component is in format other than json
    :returns: json_data: dictionary containing serialized component, document format, signature, time ot was created at
    """
    if different_format:
        document_json = doc
        document_b64 = base64.b64encode(document_json)
    else:
        document_json = provjson.encode_json_document(doc)
        document_json = jcs.canonicalize(document_json)
        document_b64 = base64.b64encode(document_json)
    with open(private_key_location, 'rb') as file:
        private_ke = file.read()
    private_key = serialization.load_pem_private_key(private_ke, password=None)
    signature = private_key.sign(data=document_json, signature_algorithm=ec.ECDSA(hashes.SHA256()))
    json_data = {}
    json_data["document"] = document_b64.decode("utf-8")
    json_data["documentFormat"] = doc_format
    json_data["signature"] = base64.b64encode(signature).decode("utf-8")
    json_data["createdOn"] = datetime.timestamp(datetime(2025, 1, 21, 2, 2, 4))
    return json_data


def register_org_to_storage(org_id, provenance_storage_url, cert_location="./test_cert.pem",
                            key_location="./test_sign.pem"):
    """Registers organization to storage service

    :param org_id: identifier of organization to register
    :param provenance_storage_url: url of storage to register to
    :param cert_location: file where to save generated certificate
    :param key_location: file where to save generated key
    :returns: res: response from storage service
    """
    with open('int1.pem', 'r') as file:
        intermediate_cert_1 = file.read()
    with open('int2.pem', 'r') as file:
        intermediate_cert_2 = file.read()
    key, client_cert = generate_certificate("SK", "test_cert", auth_key=Path("int2.key"),
                                            auth_cert=Path("int2.pem"),
                                            ca=False,
                                            path_length=None,
                                            )
    with open(cert_location, "w") as file:
        file.seek(0)
        file.write(parse_certificate(client_cert, as_string=True))
    with open(key_location, "w") as file:
        file.seek(0)
        file.write(key.private_bytes(Encoding.PEM, PrivateFormat.PKCS8, NoEncryption()).decode())
    client_cert = parse_certificate(client_cert, as_string=True)
    json_data = {}
    json_data["clientCertificate"] = client_cert
    json_data["intermediateCertificates"] = [intermediate_cert_1, intermediate_cert_2]
    res = requests.post(
        "http://" + provenance_storage_url + "/api/v1/organizations/" + org_id,
        json.dumps(json_data))
    return res


class TestDataCreator(ABC):
    """Class that creates CPM components and their attributes
    the components are created to be saved in the api tests
    """
    time_doc = datetime.timestamp(datetime(2025, 1, 21, 2, 2, 4))
    token = None
    timestamp = int(datetime.now(tz=timezone.utc).timestamp() * 1000)

    existential_variable_prefix = "ex_"
    existential_variable_suffix = ""
    org_id = ''.join(random.choices(string.ascii_uppercase + string.digits, k=8))
    org_id2 = ''.join(random.choices(string.ascii_uppercase + string.digits, k=8))

    # create meta namespace for documents saved to hospital storage
    meta_ns = Namespace(
        "meta", f"http://prov-storage-{provenance_storage_hospital_name}:8000/api/v1/documents/meta/"
    )
    main_activity_attributes_ok = {
        PROV_TYPE: CPM_MAIN_ACTIVITY,
        CPM_REFERENCED_META_BUNDLE_ID: meta_ns[
            # id of meta bundle - used when requesting meta provenance
            f"test_{timestamp}_bundle_meta"
        ],
    }

    main_activity_attributes_ok_2 = {
        PROV_TYPE: CPM_MAIN_ACTIVITY,
        CPM_REFERENCED_META_BUNDLE_ID: meta_ns[
            # different id of meta bundle - different meta component will be used
            f"test_{timestamp}_bundle_meta_2"
        ],
    }

    # create first provenance component - with only general forward connector
    doc, bundle, meta_bundle_id, backbone_basic, first_cpm_bundle_info, _ = create_cpm_provenance_basic(
        existential_variable_prefix,
        storage_name=provenance_storage_hospital_name,
        org_name=org_id,
        main_activity_attributes=main_activity_attributes_ok,
        timestamp=timestamp, smaller_prov=True)

    # create meta namespace for documents saved to pathology storage
    meta_ns_pathology = Namespace(
        "meta", f"http://prov-storage-{provenance_storage_pathology_name}:8000/api/v1/documents/meta/"
    )
    main_activity_attributes_pathology_meta = {
        PROV_TYPE: CPM_MAIN_ACTIVITY,
        # different meta namespace used
        CPM_REFERENCED_META_BUNDLE_ID: meta_ns_pathology[
            f"test_{timestamp}_bundle_w_backward_connector_meta"
        ],
    }

    # create component with one backward connector
    document_with_backwards_connector = ProvDocument()
    # namespace different from first saved document - called with different org name and storage name - pathology
    bundle_namespace_with_backwards_connector = Namespace(
        "pathology",
        f"http://prov-storage-{provenance_storage_pathology_name}:8000/api/v1/organizations/{org_id2}/documents/",
    )

    first_meta_bundle_info = [meta_bundle_id.namespace.uri, meta_bundle_id.localpart]

    bundle_identifier = f"test_{timestamp}_bundle_w_backward_connector"
    bundle_with_backwards_conn = document_with_backwards_connector.bundle(
        bundle_namespace_with_backwards_connector[bundle_identifier])
    # first_cpm_bundle_info - first is uri, second localpart
    remote_bundle_namespace = Namespace("remote_bundle", first_cpm_bundle_info[0])
    remote_meta_bundle_namespace = Namespace(
        "remote_meta_bundle", first_meta_bundle_info[0]
    )

    # create backward connector attributes - values from first component used for references
    backward_connector_attributes = {
        PROV_TYPE: CPM_BACKWARD_CONNECTOR,
        CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace[first_cpm_bundle_info[1]],
        CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace[
            first_meta_bundle_info[1]
        ],
        CPM_REFERENCED_BUNDLE_HASH_VALUE: first_cpm_bundle_info[2],
        # a hash value of a PROV bundle referenced by the connector in which the attribute is present
        CPM_HASH_ALG: "SHA256"
    }
    # call sub-function to create the component with backward connector - will be saved to the pathology provenance storage
    doc2, backbone_with_backwards_connector, next_meta_bundle_id, next_cpm_bundle_info = create_cpm_provenance_with_backward_connector(
        document_with_backwards_connector,
        bundle_with_backwards_conn,
        existential_variable_prefix,
        prev_cpm_bundle_info=first_cpm_bundle_info,
        prev_meta_bundle_info=[meta_bundle_id.namespace.uri, meta_bundle_id.localpart],
        sender_org_name=provenance_storage_hospital_name,
        main_activity_attributes=main_activity_attributes_pathology_meta,
        backward_connector_attributes=backward_connector_attributes)

    # prepare arguments to update first document - add concrete forward connector to it
    next_meta_bundle_info = [next_meta_bundle_id.namespace.uri, next_meta_bundle_id.localpart]

    remote_bundle_namespace_2 = Namespace("remote_bundle", next_cpm_bundle_info[0])
    remote_meta_bundle_namespace_2 = Namespace(
        "remote_meta_bundle", next_meta_bundle_info[0]
    )

    forward_connector_attributes = {
        PROV_TYPE: CPM_FORWARD_CONNECTOR,
        CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_2[next_cpm_bundle_info[1]],
        CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace_2[
            next_meta_bundle_info[1]
        ],
        CPM_REFERENCED_BUNDLE_HASH_VALUE: next_cpm_bundle_info[2],
        CPM_HASH_ALG: "SHA256"
    }

    # create document with concrete forward connector
    doc_fw, bundle_fw, meta_bundle_id_w_forward, backbone_basic_w_forward, cpm_bundle_info_forward_conn_doc = create_cpm_with_forward_connector(
        existential_variable_prefix,
        storage_name=provenance_storage_hospital_name,
        org_name=org_id, main_activity_attributes=main_activity_attributes_ok,
        receiver_org_name=provenance_storage_pathology_name,
        timestamp=timestamp, bundle_suffix="_with_forward_connector",
        forward_connector_attributes=forward_connector_attributes)

    # create document with different identifier from first and different domain provenance - updated
    doc_updated, bundle_updated, meta_bundle_id_updated, backbone_basic_updated, _, _ = create_cpm_provenance_basic(
        existential_variable_prefix,
        storage_name=provenance_storage_hospital_name,
        org_name=org_id,
        main_activity_attributes=main_activity_attributes_ok,
        timestamp=timestamp,
        bundle_suffix="_updated")

    # create component representing end of provenance chain -without forward connector
    doc_end, bundle_end, meta_bundle_id_end, backbone_basic_end, last_cpm_bundle_info = create_cpm_provenance_basic_without_fw_connector(
        existential_variable_prefix,
        storage_name=provenance_storage_hospital_name,
        org_name=org_id,
        main_activity_attributes=main_activity_attributes_ok_2,
        timestamp=timestamp,
        bundle_suffix="_end")

    last_meta_bundle_info = [meta_bundle_id_end.namespace.uri, meta_bundle_id_end.localpart]

    # prepare document with both connectors - add forward connector referencing end of chain
    # leave backward connector as was before  - connecting to same document
    document_with_both_connector = ProvDocument()
    main_activity_attributes_ok_3 = {
        PROV_TYPE: CPM_MAIN_ACTIVITY,
        CPM_REFERENCED_META_BUNDLE_ID: meta_ns_pathology[
            f"test_{timestamp}_bundle_w_backward_connector_meta_2"
        ],
    }
    bundle_identifier = f"test_{timestamp}_bundle_w_both_connectors"
    bundle_with_both_conn = document_with_both_connector.bundle(
        bundle_namespace_with_backwards_connector[bundle_identifier])

    # remote_bundle, in function for creation of whole component there is remote_bundle namespace defined and is not different
    remote_bundle_namespace_3 = bundle_with_both_conn.add_namespace("remote_bundle",
                                                                    last_cpm_bundle_info[0])
    remote_meta_bundle_namespace_3 = bundle_with_both_conn.add_namespace(
        "remote_meta_bundle_2", last_meta_bundle_info[0]
    )

    forward_connector_attributes_2 = {
        PROV_TYPE: CPM_FORWARD_CONNECTOR,
        CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_3[last_cpm_bundle_info[1]],
        CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace_3[
            last_meta_bundle_info[1]
        ],
        CPM_REFERENCED_BUNDLE_HASH_VALUE: last_cpm_bundle_info[2],
        CPM_HASH_ALG: "SHA256"
    }

    # call sub-function with prepared attributes to create component with forward and backward connector
    doc_with_both_connectors, backbone_with_both_connectors, middle_meta_bundle_id, middle_cpm_bundle_info = (
        create_cpm_provenance_with_backward_connector(
        document_with_both_connector,
        bundle_with_both_conn,
        existential_variable_prefix,
        prev_cpm_bundle_info=first_cpm_bundle_info,
        prev_meta_bundle_info=[meta_bundle_id.namespace.uri, meta_bundle_id.localpart],
        sender_org_name=provenance_storage_hospital_name, main_activity_attributes=main_activity_attributes_ok_3,
        backward_connector_attributes=backward_connector_attributes, add_forward_connector_param=True,
        receiver_org_name=provenance_storage_hospital_name,
        forward_connector_attributes=forward_connector_attributes_2))

    middle_meta_bundle_info = [middle_meta_bundle_id.namespace.uri, middle_meta_bundle_id.localpart]

    # prepare component - provenance chain end with added backward connectors referencing previous documents, also add namespaces etc. to it
    document_with_backwards_connector_updated = ProvDocument()

    bundle_namespace = Namespace(
        provenance_storage_hospital_name,
        f"http://prov-storage-{provenance_storage_hospital_name}:8000/api/v1/organizations/{org_id}/documents/",
    )
    bundle_identifier = f"test_{timestamp}_bundle_end_updated"
    bundle = document_with_backwards_connector_updated.bundle(bundle_namespace[bundle_identifier])
    bundle.add_namespace("example", "http://example.com#")

    remote_bundle_namespace_1 = bundle.add_namespace("remote_bundle_1",
                                                     first_cpm_bundle_info[0])
    remote_meta_bundle_namespace_1 = bundle.add_namespace(
        "remote_meta_bundle_1", first_meta_bundle_info[0]
    )
    remote_bundle_namespace_3 = bundle.add_namespace("remote_bundle_2",
                                                     middle_cpm_bundle_info[0])
    remote_meta_bundle_namespace_3 = bundle.add_namespace(
        "remote_meta_bundle_2", middle_meta_bundle_info[0]
    )

    backward_connector_attributes_1 = {
        PROV_TYPE: CPM_BACKWARD_CONNECTOR,
        CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_1[first_cpm_bundle_info[1]],
        CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace_1[
            first_meta_bundle_info[1]
        ],
        CPM_REFERENCED_BUNDLE_HASH_VALUE: first_cpm_bundle_info[2],
        CPM_HASH_ALG: "SHA256"
    }

    backward_connector_attributes_2 = {
        PROV_TYPE: CPM_BACKWARD_CONNECTOR,
        CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_3[middle_cpm_bundle_info[1]],
        CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace_3[
            middle_meta_bundle_info[1]
        ],
        CPM_REFERENCED_BUNDLE_HASH_VALUE: middle_cpm_bundle_info[2],
        CPM_HASH_ALG: "SHA256"
    }

    # call sub-function to create updated component with two backward connectors
    (document_with_backwards_connector_updated, bundle_with_backwards_connector_updated, meta_bundle_id_end,
     backbone_doc_with_backwards_connector_updated, last_cpm_bundle_info_3) = (
        create_cpm_provenance_basic_without_fw_connector_with_two_bw_connectors(
            document_with_backwards_connector_updated, bundle,
            existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            main_activity_attributes=main_activity_attributes_ok_2,
            # add backward connectors to reference basic cpm doc, doc with both connectors
            sender_org_name_1=provenance_storage_hospital_name,
            backward_connector_attributes_1=backward_connector_attributes_1,
            sender_org_name_2=provenance_storage_pathology_name,
            backward_connector_attributes_2=backward_connector_attributes_2))

    # create updated first document with 2 forward connectors - one of them redundant
    remote_bundle_namespace_3 = Namespace("remote_bundle_2",
                                          last_cpm_bundle_info[0])
    remote_meta_bundle_namespace_3 = Namespace(
        "remote_meta_bundle_2", last_meta_bundle_info[0]
    )

    forward_connector_attributes_end = {
        PROV_TYPE: CPM_FORWARD_CONNECTOR,
        CPM_REFERENCED_BUNDLE_ID: remote_bundle_namespace_3[last_cpm_bundle_info[1]],
        CPM_REFERENCED_META_BUNDLE_ID: remote_meta_bundle_namespace_3[
            last_meta_bundle_info[1]
        ],
        CPM_REFERENCED_BUNDLE_HASH_VALUE: last_cpm_bundle_info[2],
        CPM_HASH_ALG: "SHA256"
    }

    # call sub-function to create component with wwo forward connectors
    doc_two_forward_conns, bundle_both_fws, _, backbone_basic_w_two_forward, first_with_two_conns_cpm_bundle_info = (
        create_cpm_with_forward_connector(
            existential_variable_prefix,
            storage_name=provenance_storage_hospital_name,
            org_name=org_id, main_activity_attributes=main_activity_attributes_ok,
            receiver_org_name=provenance_storage_pathology_name,
            timestamp=timestamp, bundle_suffix="_with_both_forward_connectors",
            forward_connector_attributes=forward_connector_attributes, add_second_forward_connector=True,
            second_forward_connector_attributes=forward_connector_attributes_end,
            second_receiver_org_name=provenance_storage_hospital_name))
