from subprocess import call
from time import sleep

from OpenSSL.crypto import X509StoreContextError
from cryptography.exceptions import InvalidSignature

from non_repudiation.controller import *
from non_repudiation.models import *
from django.test import TestCase


class ControllerTestCase(TestCase):
    time1 = None
    time2 = None

    #uncomment if database is not running
    """success = call("docker compose up -d --wait", shell=True)
    sleep(10)
    # Need to sleep to wait for the test instance to completely come up

    @classmethod
    def tearDownClass(cls):
        success = call("docker compose down", shell=True)
        assert (success == 0)"""

    #populate database
    @classmethod
    def setUpTestData(cls):
        org = Organization.objects.create(org_name="org1")
        org2 = Organization.objects.create(org_name="org2")

        time = datetime.now().timestamp()
        cls.time1 = time
        time2 = datetime.now().timestamp()
        cls.time2 = time
        #max 1 non revoked certificate per organization
        cert = Certificate.objects.create(cert_digest="cert", cert="certificate1: string...", certificate_type="client",
                                          received_on=int(time), organization=org)
        cert2 = Certificate.objects.create(cert_digest="cert1", cert="certificate2: string...", certificate_type="client",
                                          received_on=int(time), organization=org2, is_revoked = True)
        cert3 = Certificate.objects.create(cert_digest="cert2", cert="certificate3: string...", certificate_type="client",
                                          received_on=int(time), organization=org2)
        cert4 = Certificate.objects.create(cert_digest="cert4", cert="certificate4: string...",
                                           certificate_type="client",
                                           received_on=int(time), organization=org2, is_revoked=True)

        doc = Document.objects.create(identifier="id", certificate=cert, organization=org, document_type="backbone",
                                      document_text="text...", created_on=int(time), doc_format="json")
        doc2 = Document.objects.create(identifier="id2", certificate=cert, organization=org, document_type="graph",
                                      document_text="text...", created_on=int(time), doc_format="json")
        doc3 = Document.objects.create(identifier="id3", certificate=cert, organization=org2, document_type="graph",
                                       document_text="text...", created_on=int(time), doc_format="json")

        Token.objects.create(document=doc, hash="hash", hash_function="SHA512", created_on=int(time),
                                     signature="signature...")
        Token.objects.create(document=doc2, hash="hash2", hash_function="SHA512", created_on=int(time2),
                             signature="signature2...")
        Token.objects.create(document=doc3, hash="hash3", hash_function="SHA512", created_on=int(time),
                             signature="signature3...")
        Token.objects.create(document=doc3, hash="hash4", hash_function="SHA3-512", created_on=int(time2),
                             signature="signature4...")

    def test_retrieve_organizations_returns_organizations(self):
        result = retrieve_organizations()

        self.assertEqual(2, len(result))

        org1 = result[0]
        org2 = result[1]
        self.assertEqual("org1", org1["id"])
        self.assertEqual("certificate1: string...", org1["certificate"])
        self.assertEqual("org2", org2["id"])
        self.assertEqual("certificate3: string...", org2["certificate"])

    def test_retrieve_organization_ok_without_revoked_certs_returns_organizations(self):
        result = retrieve_organization("org1")

        self.assertEqual("org1", result["id"])
        self.assertEqual("certificate1: string...", result["certificate"])

    def test_retrieve_organization_ok_with_revoked_certs_returns_revoked(self):
        result = retrieve_organization("org2", include_revoked=True)

        revoked = result["revokedCertificates"]

        self.assertEqual("org2", result["id"])
        self.assertEqual("certificate3: string...", result["certificate"])
        self.assertEqual(2, len(revoked))
        self.assertEqual("certificate2: string...", revoked[0])
        self.assertEqual("certificate4: string...", revoked[1])

    def test_get_sorted_certificates_returns_certificates(self):
        active_cert, revoked_certs = get_sorted_certificates("org2")

        self.assertEqual("certificate3: string...", active_cert.cert)
        self.assertEqual("cert2", active_cert.cert_digest)
        self.assertEqual(2, len(revoked_certs))
        self.assertEqual("certificate2: string...", revoked_certs[0].cert)
        self.assertEqual("certificate4: string...", revoked_certs[1].cert)

    def test_verify_chain_of_trust_ok_no_exception(self):
        # for these tests need to add root cert of CA1 to trustedCertsDirPath defined in config
        #same certificates used as in simalation used
        with open('certificates/uni_graz/uni_graz.pem', 'r') as file:
            client_cert = file.read()
        with open('certificates/uni_graz/intermediate_1.pem', 'r') as file:
            intermediate_cert_1 = file.read()
        with open('certificates/uni_graz/intermediate_2.pem', 'r') as file:
            intermediate_cert_2 = file.read()

        verify_chain_of_trust(client_cert, [intermediate_cert_1, intermediate_cert_2])

    def test_verify_chain_of_trust_nok_throws_exception(self):
        with open('certificates/uni_graz/uni_graz.pem', 'r') as file:
            client_cert = file.read()
        with open('certificates/uni_graz/intermediate_1.pem', 'r') as file:
            intermediate_cert_1 = file.read()

        # missing second intermediate certificate - throws error
        self.assertRaises(X509StoreContextError, verify_chain_of_trust, client_cert, [intermediate_cert_1])

    def test_store_organization_stores_organization_and_certs_to_database(self):
        with open('certificates/uni_graz/uni_graz.pem', 'r') as file:
            client_cert = file.read()
        with open('certificates/uni_graz/intermediate_1.pem', 'r') as file:
            intermediate_cert_1 = file.read()
        with open('certificates/uni_graz/intermediate_2.pem', 'r') as file:
            intermediate_cert_2 = file.read()

        store_organization("uni_graz", client_cert, [intermediate_cert_1, intermediate_cert_2])

        org = Organization.objects.filter(org_name="uni_graz").first()
        self.assertEqual("uni_graz", org.org_name)

        certs = Certificate.objects.filter(organization = org).all()
        self.assertEqual(3, len(certs))

        cert1 = certs[0]
        self.assertEqual(client_cert, cert1.cert)
        serialized_client_cert = crypto.load_certificate(crypto.FILETYPE_PEM, client_cert.encode('utf-8'))
        self.assertEqual(serialized_client_cert.digest("sha256").decode("utf-8").replace(":", ""),
                         cert1.cert_digest)
        self.assertEqual("client", cert1.certificate_type)
        self.assertFalse(cert1.is_revoked)
        self.assertEqual(org, cert1.organization)

        cert2 = certs[1]
        self.assertEqual(intermediate_cert_1, cert2.cert)
        serialized_cert1 = crypto.load_certificate(crypto.FILETYPE_PEM, intermediate_cert_1.encode('utf-8'))
        self.assertEqual(serialized_cert1.digest("sha256").decode("utf-8").replace(":", ""),
                         cert2.cert_digest)
        self.assertEqual("intermediate", cert2.certificate_type)
        self.assertFalse(cert2.is_revoked)
        self.assertEqual(org, cert2.organization)

        cert3 = certs[2]
        self.assertEqual(intermediate_cert_2, cert3.cert)
        serialized_cert2 = crypto.load_certificate(crypto.FILETYPE_PEM, intermediate_cert_2.encode('utf-8'))
        self.assertEqual(serialized_cert2.digest("sha256").decode("utf-8").replace(":", ""),
                         cert3.cert_digest)
        self.assertEqual("intermediate", cert3.certificate_type)
        self.assertFalse(cert3.is_revoked)
        self.assertEqual(org, cert3.organization)

    def test_update_certificate_updates_cert(self):
        with open('certificates/uni_munich/uni_munchen.pem', 'r') as file:
            client_cert_old = file.read()
        with open('certificates/uni_munich/intermediate_1.pem', 'r') as file:
            intermediate_cert_old_1 = file.read()
        with open('certificates/uni_graz/intermediate_2.pem', 'r') as file:
            intermediate_cert_old_2 = file.read()

        store_organization("uni_graz", client_cert_old, [intermediate_cert_old_1,
                                                         intermediate_cert_old_2])

        with open('certificates/uni_graz/uni_graz.pem', 'r') as file:
            client_cert = file.read()
        with open('certificates/uni_graz/intermediate_1.pem', 'r') as file:
            intermediate_cert_1 = file.read()
        with open('certificates/uni_graz/intermediate_2.pem', 'r') as file:
            intermediate_cert_2 = file.read()

        update_certificate("uni_graz", client_cert, [intermediate_cert_1, intermediate_cert_2])

        org = Organization.objects.filter(org_name="uni_graz").first()
        self.assertEqual("uni_graz", org.org_name)

        # test original certificates - revoked now
        certs = Certificate.objects.filter(organization=org, is_revoked=True).all()
        self.assertEqual(2, len(certs))

        cert1 = certs[0]
        self.assertEqual(client_cert_old, cert1.cert)
        self.assertEqual("client", cert1.certificate_type)
        self.assertTrue(cert1.is_revoked)
        self.assertEqual(org, cert1.organization)

        cert2 = certs[1]
        self.assertEqual(intermediate_cert_old_1, cert2.cert)
        self.assertEqual("intermediate", cert2.certificate_type)
        self.assertTrue(cert2.is_revoked)
        self.assertEqual(org, cert2.organization)

        #test changed certificates
        certs = Certificate.objects.filter(organization=org, is_revoked = False).all()
        self.assertEqual(3, len(certs))

        cert1 = [t for t in certs if t.cert == client_cert][0]
        self.assertEqual(client_cert, cert1.cert)
        serialized_client_cert = crypto.load_certificate(crypto.FILETYPE_PEM, client_cert.encode('utf-8'))
        self.assertEqual(serialized_client_cert.digest("sha256").decode("utf-8").replace(":", ""),
                         cert1.cert_digest)
        self.assertEqual("client", cert1.certificate_type)
        self.assertFalse(cert1.is_revoked)
        self.assertEqual(org, cert1.organization)

        cert2 = [t for t in certs if t.cert == intermediate_cert_1][0]
        self.assertEqual(intermediate_cert_1, cert2.cert)
        serialized_cert1 = crypto.load_certificate(crypto.FILETYPE_PEM, intermediate_cert_1.encode('utf-8'))
        self.assertEqual(serialized_cert1.digest("sha256").decode("utf-8").replace(":", ""),
                         cert2.cert_digest)
        self.assertEqual("intermediate", cert2.certificate_type)
        self.assertFalse(cert2.is_revoked)
        self.assertEqual(org, cert2.organization)

        cert3 = [t for t in certs if t.cert == intermediate_cert_2][0]
        self.assertEqual(intermediate_cert_2, cert3.cert)
        serialized_cert2 = crypto.load_certificate(crypto.FILETYPE_PEM, intermediate_cert_2.encode('utf-8'))
        self.assertEqual(serialized_cert2.digest("sha256").decode("utf-8").replace(":", ""),
                         cert3.cert_digest)
        self.assertEqual("intermediate", cert3.certificate_type)
        self.assertFalse(cert3.is_revoked)
        self.assertEqual(org, cert3.organization)

    def test_revoke_all_stored_certificates_revokes_certs(self):
        revoke_all_stored_certificates("org2")

        certs = list(
            Certificate.objects.filter(organization="org2").all()
        )

        self.assertEqual(3, len(certs))
        self.assertTrue(certs[0].is_revoked)
        self.assertTrue(certs[1].is_revoked)
        self.assertTrue(certs[2].is_revoked)

    def test_retrieve_document_returns_document(self):
        doc = retrieve_document("org1", "id")
        org =Organization.objects.filter(org_name="org1").first()
        doc_from_db = Document.objects.filter(organization=org, identifier="id").first()

        self.assertEqual(doc_from_db.identifier, doc.identifier)
        self.assertEqual(doc_from_db.certificate, doc.certificate)
        self.assertEqual(doc_from_db.document_text, doc.document_text)

    def test_retrieve_tokens_returns_tokens_of_organization(self):
        tokens = retrieve_tokens("org1")

        with open('./../resources/cert.pem', 'r') as file:
            my_cert = file.read()

        self.assertEqual(2, len(tokens))
        self.assertEqual("org1", tokens[0]["data"]["originatorId"])
        self.assertEqual("TrustedParty", tokens[0]["data"]["authorityId"])
        self.assertEqual(int(self.time1), tokens[0]["data"]["tokenTimestamp"])
        self.assertEqual(int(self.time1), tokens[0]["data"]["documentCreationTimestamp"])
        self.assertEqual("hash", tokens[0]["data"]["documentDigest"])
        self.assertEqual("id", tokens[0]["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", tokens[0]["data"]["additionalData"]["hashFunction"])
        self.assertEqual("127.0.0.1:8020", tokens[0]["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(my_cert, tokens[0]["data"]["additionalData"]["trustedPartyCertificate"])
        self.assertEqual("signature...", tokens[0]["signature"])

        self.assertEqual("org1", tokens[1]["data"]["originatorId"])
        self.assertEqual("TrustedParty", tokens[1]["data"]["authorityId"])
        self.assertEqual(int(self.time2), tokens[1]["data"]["tokenTimestamp"])
        self.assertEqual(int(self.time1), tokens[1]["data"]["documentCreationTimestamp"])
        self.assertEqual("hash2", tokens[1]["data"]["documentDigest"])
        self.assertEqual("id2", tokens[1]["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", tokens[1]["data"]["additionalData"]["hashFunction"])
        self.assertEqual("127.0.0.1:8020", tokens[1]["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(my_cert, tokens[1]["data"]["additionalData"]["trustedPartyCertificate"])
        self.assertEqual("signature2...", tokens[1]["signature"])

    def test_retrieve_specific_token_exist_returns_token(self):
        token = retrieve_specific_token("org1", "id", "backbone")

        with open('./../resources/cert.pem', 'r') as file:
            my_cert = file.read()

        self.assertEqual("org1", token["data"]["originatorId"])
        self.assertEqual("TrustedParty", token["data"]["authorityId"])
        self.assertEqual(int(self.time1), token["data"]["tokenTimestamp"])
        self.assertEqual(int(self.time1), token["data"]["documentCreationTimestamp"])
        self.assertEqual("hash", token["data"]["documentDigest"])
        self.assertEqual("id", token["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", token["data"]["additionalData"]["hashFunction"])
        self.assertEqual("127.0.0.1:8020", token["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(my_cert, token["data"]["additionalData"]["trustedPartyCertificate"])
        self.assertEqual("signature...", token["signature"])

    def test_retrieve_specific_token_does_not_exist_throws_exception(self):
        self.assertRaises(Document.DoesNotExist, retrieve_specific_token,"org1", "id", "graph")

    def test_retrieve_specific_multiple_tokens_exist_returns_all_tokens(self):
        tokens = retrieve_specific_token("org2", "id3", "graph")

        with open('./../resources/cert.pem', 'r') as file:
            my_cert = file.read()

        self.assertEqual(2, len(tokens))
        self.assertEqual("org2", tokens[0]["data"]["originatorId"])
        self.assertEqual("TrustedParty", tokens[0]["data"]["authorityId"])
        self.assertEqual(int(self.time1), tokens[0]["data"]["tokenTimestamp"])
        self.assertEqual(int(self.time1), tokens[0]["data"]["documentCreationTimestamp"])
        self.assertEqual("hash3", tokens[0]["data"]["documentDigest"])
        self.assertEqual("id3", tokens[0]["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", tokens[0]["data"]["additionalData"]["hashFunction"])
        self.assertEqual("127.0.0.1:8020", tokens[0]["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(my_cert, tokens[0]["data"]["additionalData"]["trustedPartyCertificate"])
        self.assertEqual("signature3...", tokens[0]["signature"])

        self.assertEqual("org2", tokens[1]["data"]["originatorId"])
        self.assertEqual("TrustedParty", tokens[1]["data"]["authorityId"])
        self.assertEqual(int(self.time2), tokens[1]["data"]["tokenTimestamp"])
        self.assertEqual(int(self.time1), tokens[1]["data"]["documentCreationTimestamp"])
        self.assertEqual("hash4", tokens[1]["data"]["documentDigest"])
        self.assertEqual("id3", tokens[1]["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", tokens[1]["data"]["additionalData"]["hashFunction"])
        self.assertEqual("127.0.0.1:8020", tokens[1]["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(my_cert, tokens[1]["data"]["additionalData"]["trustedPartyCertificate"])
        self.assertEqual("signature4...", tokens[1]["signature"])

    def test_verify_signature_ok_no_exception(self):
        with open('./../resources/cert.pem', 'r') as file:
            client_cert = file.read()

        store_organization("uni_graz", client_cert, [])

        signature = private_key.sign(
            "graph".encode('utf-8'), ec.ECDSA(hashes.SHA256())
        )

        json_data = {}
        json_data["organizationId"] = "uni_graz"
        json_data["document"] = base64.b64encode("graph".encode('utf-8'))
        json_data["signature"] = base64.b64encode(signature)

        # does not throw error - ok
        verify_signature(json_data)

    def test_verify_signature_nok_throws_exception(self):
        with open('./../resources/cert.pem', 'r') as file:
            client_cert = file.read()

        store_organization("uni_graz", client_cert, [])

        signature = private_key.sign(
            "graphh".encode('utf-8'), ec.ECDSA(hashes.SHA256()) # different graph signed - nok signature
        )

        json_data = {}
        json_data["organizationId"] = "uni_graz"
        json_data["document"] = base64.b64encode("graph".encode('utf-8'))
        json_data["signature"] = base64.b64encode(signature)

        # does not throw error - ok
        with self.assertRaises(InvalidSignature):
            verify_signature(json_data)

    def test_get_serialized_token_returns_serialized_token(self):
        json_data = {}
        json_data["organizationId"] = "uni_graz"
        json_data["document"] = base64.b64encode("graph".encode('utf-8'))
        json_data["createdOn"] = int(self.time1)

        digest = hashes.Hash(hashes.SHA256())
        digest.update(base64.b64decode(json_data["document"]))
        hash = digest.finalize().hex()

        result = get_serialized_token(json_data, "doc1")

        self.assertEqual(json_data["organizationId"], result["data"]["originatorId"])
        self.assertEqual("TrustedParty", result["data"]["authorityId"])
        self.assertIsNotNone(result["data"]["tokenTimestamp"])
        self.assertEqual(json_data["createdOn"], result["data"]["documentCreationTimestamp"])
        self.assertEqual(hash, result["data"]["documentDigest"])
        self.assertEqual("doc1", result["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", result["data"]["additionalData"]["hashFunction"])
        self.assertEqual(config.fqdn, result["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(config.cert, result["data"]["additionalData"]["trustedPartyCertificate"])

        loaded_cert = load_pem_x509_certificate(
            config.cert.encode("utf-8"), backend=default_backend()
        )
        public_key = loaded_cert.public_key()

        #verify that the signature is ok
        public_key.verify(
            base64.b64decode(result["signature"]), (jcs.canonicalize(result["data"])), ec.ECDSA(hashes.SHA256())
        )

    def test_create_new_token_creates_saves_returns_token(self):
        json_data = {}
        json_data["organizationId"] = "uni_graz"
        json_data["document"] = base64.b64encode("graph".encode('utf-8'))
        json_data["createdOn"] = int(self.time1)

        org = Organization.objects.filter(org_name="org1").first()
        cert = Certificate.objects.filter(cert_digest="cert").first()
        doc = Document.objects.create(identifier="id6", certificate=cert, organization=org, document_type="backbone",
                                      document_text="text...", created_on=int(self.time1))

        result = create_new_token(json_data, doc)

        self.assertEqual(json_data["organizationId"], result["data"]["originatorId"])
        self.assertEqual("TrustedParty", result["data"]["authorityId"])
        self.assertIsNotNone(result["data"]["tokenTimestamp"])
        self.assertEqual(json_data["createdOn"], result["data"]["documentCreationTimestamp"])
        self.assertEqual("id6", result["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", result["data"]["additionalData"]["hashFunction"])
        self.assertEqual(config.fqdn, result["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(config.cert, result["data"]["additionalData"]["trustedPartyCertificate"])

        loaded_cert = load_pem_x509_certificate(
            config.cert.encode("utf-8"), backend=default_backend()
        )
        public_key = loaded_cert.public_key()

        # verify that the signature is ok
        public_key.verify(
            base64.b64decode(result["signature"]), (jcs.canonicalize(result["data"])), ec.ECDSA(hashes.SHA256())
        )

        # asserts for token from db - saved by function
        token = Token.objects.filter(document=doc).first()
        self.assertEqual(result["data"]["documentDigest"], token.hash)
        self.assertEqual("SHA256", token.hash_function)
        self.assertEqual(doc, token.document)
        self.assertEqual(result["data"]["tokenTimestamp"], token.created_on)
        self.assertEqual(result["signature"], token.signature)

    def test_check_is_subgraph_ok_returns_true(self):
        prov_bundle = ProvBundle()
        prov_bundle_original = ProvBundle()

        ns = prov_bundle.add_namespace("example", 'http://example.org/2/')
        prov_bundle.entity('example:e001')

        ns = prov_bundle_original.add_namespace("example", 'http://example.org/2/')
        prov_bundle_original.entity('example:e001')
        prov_bundle_original.agent('example:a001')

        result = check_is_subgraph(prov_bundle, prov_bundle_original)
        self.assertTrue(result)

    def test_check_is_subgraph_nok_namespace_returns_false(self):
        prov_bundle = ProvBundle()
        prov_bundle_original = ProvBundle()

        ns = prov_bundle.add_namespace("example", 'http://example.org/2/')
        prov_bundle.entity('example:e001')

        ns = prov_bundle_original.add_namespace("example2", 'http://example.org/2/')
        prov_bundle_original.entity('example2:e001')
        prov_bundle_original.agent('example2:a001')

        result = check_is_subgraph(prov_bundle, prov_bundle_original)
        self.assertEqual(False, result)

    def test_check_is_subgraph_nok_added_entity_returns_false(self):
        prov_bundle = ProvBundle()
        prov_bundle_original = ProvBundle()

        ns = prov_bundle.add_namespace("example", 'http://example.org/2/')
        prov_bundle.entity('example:e001')
        prov_bundle.entity('example:e002')


        ns = prov_bundle_original.add_namespace("example", 'http://example.org/2/')
        prov_bundle_original.entity('example:e001')
        prov_bundle_original.agent('example:a001')

        result = check_is_subgraph(prov_bundle, prov_bundle_original)
        self.assertEqual(False, result)

    def test_check_is_subgraph_nok_added_activity_returns_false(self):
        prov_bundle = ProvBundle()
        prov_bundle_original = ProvBundle()

        ns = prov_bundle.add_namespace("example", 'http://example.org/2/')
        prov_bundle.entity('example:e001')
        prov_bundle.activity('example:a002')


        ns = prov_bundle_original.add_namespace("example", 'http://example.org/2/')
        prov_bundle_original.entity('example:e001')
        prov_bundle_original.agent('example:a001')

        result = check_is_subgraph(prov_bundle, prov_bundle_original)
        self.assertEqual(False, result)

    def test_check_is_subgraph_nok_added_relation_returns_false(self):
        prov_bundle = ProvBundle()
        prov_bundle_original = ProvBundle()

        ns = prov_bundle.add_namespace("example", 'http://example.org/2/')
        e1 = prov_bundle.entity('example:e001')
        e2 = prov_bundle.entity('example:e002')
        prov_bundle.specialization(e1, e2)


        ns = prov_bundle_original.add_namespace("example", 'http://example.org/2/')
        prov_bundle_original.entity('example:e001')
        prov_bundle_original.entity('example:e002')
        prov_bundle_original.agent('example:a001')

        result = check_is_subgraph(prov_bundle, prov_bundle_original)
        self.assertEqual(False, result)

    def test_issue_token_and_store_doc_graph_ok_stores_data_returns_token(self):
        with open('01_sample_acquisition.json', 'r') as file:
            graph = file.read()

        json_data = {}
        json_data["organizationId"] = "org1"
        json_data["document"] = base64.b64encode(graph.encode('utf-8'))
        json_data["documentFormat"] = "json"
        json_data["createdOn"] = int(self.time1)
        json_data["type"] = "graph"
        json_data["signature"] = "signature"

        result = issue_token_and_store_doc(json_data)

        #asserts similar to create token test
        self.assertEqual(json_data["organizationId"], result["data"]["originatorId"])
        self.assertEqual("TrustedParty", result["data"]["authorityId"])
        self.assertIsNotNone(result["data"]["tokenTimestamp"])
        self.assertEqual(json_data["createdOn"], result["data"]["documentCreationTimestamp"])
        self.assertEqual("http://127.0.1.1.com:8000/api/v1/organizations/UniGraz/documents/01_sample_acquisition",
                         result["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", result["data"]["additionalData"]["hashFunction"])
        self.assertEqual(config.fqdn, result["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(config.cert, result["data"]["additionalData"]["trustedPartyCertificate"])

        loaded_cert = load_pem_x509_certificate(
            config.cert.encode("utf-8"), backend=default_backend()
        )
        public_key = loaded_cert.public_key()

        # verify that the signature is ok
        public_key.verify(
            base64.b64decode(result["signature"]), (jcs.canonicalize(result["data"])), ec.ECDSA(hashes.SHA256())
        )

        doc = Document.objects.filter(identifier = result["data"]["additionalData"]["bundle"]).first()
        org = Organization.objects.filter(org_name=json_data["organizationId"]).first()
        cert = Certificate.objects.filter(organization=org, is_revoked=False).first()

        # asserts for token from db - saved by function
        token = Token.objects.filter(document=doc).first()
        self.assertEqual(result["data"]["documentDigest"], token.hash)
        self.assertEqual("SHA256", token.hash_function)
        self.assertEqual(doc, token.document)
        self.assertEqual(result["data"]["tokenTimestamp"], token.created_on)
        self.assertEqual(result["signature"], token.signature)

        # asserts for doc
        self.assertEqual("http://127.0.1.1.com:8000/api/v1/organizations/UniGraz/documents/01_sample_acquisition",
                         doc.identifier)
        self.assertEqual(cert, doc.certificate)
        self.assertEqual(org, doc.organization)
        self.assertEqual("graph", doc.document_type)
        self.assertEqual(str(base64.b64encode(graph.encode('utf-8'))), doc.document_text)
        self.assertEqual(int(self.time1), doc.created_on)
        self.assertEqual("signature", doc.signature)

    def test_issue_token_and_store_doc_meta_ok_returns_token(self):
        with open('01_sample_acquisition.json', 'r') as file:
            graph = file.read()

        json_data = {}
        json_data["organizationId"] = "org1"
        json_data["document"] = base64.b64encode(graph.encode('utf-8'))
        json_data["documentFormat"] = "json"
        json_data["createdOn"] = int(self.time1)
        json_data["type"] = "meta"
        json_data["signature"] = "signature"

        result = issue_token_and_store_doc(json_data)

        # asserts similar to create token test
        self.assertEqual(json_data["organizationId"], result["data"]["originatorId"])
        self.assertEqual("TrustedParty", result["data"]["authorityId"])
        self.assertIsNotNone(result["data"]["tokenTimestamp"])
        self.assertEqual(json_data["createdOn"], result["data"]["documentCreationTimestamp"])
        self.assertEqual("http://127.0.1.1.com:8000/api/v1/organizations/UniGraz/documents/01_sample_acquisition",
                         result["data"]["additionalData"]["bundle"])
        self.assertEqual("SHA256", result["data"]["additionalData"]["hashFunction"])
        self.assertEqual(config.fqdn, result["data"]["additionalData"]["trustedPartyUri"])
        self.assertEqual(config.cert, result["data"]["additionalData"]["trustedPartyCertificate"])

        def find_doc():
            return Document.objects.get(identifier=result["data"]["additionalData"]["bundle"])

        self.assertRaises(Document.DoesNotExist, find_doc)

        def find_token():
            return Token.objects.get(hash=result["data"]["documentDigest"])

        self.assertRaises(Token.DoesNotExist, find_token)

    def test_issue_token_and_store_doc_backbone_ok_returns_token(self):
        with open('01_sample_acquisition.json', 'r') as file:
            graph = file.read()

        json_data = {}
        json_data["organizationId"] = "org1"
        json_data["document"] = base64.b64encode(graph.encode('utf-8'))
        json_data["documentFormat"] = "json"
        json_data["createdOn"] = int(self.time1)
        json_data["type"] = "backbone"
        json_data["signature"] = "signature"

        result = issue_token_and_store_doc(json_data)

        # asserts similar to create token test
        self.assertEqual(json_data["organizationId"], result["data"]["originatorId"])
        self.assertEqual("TrustedParty", result["data"]["authorityId"])
        self.assertIsNotNone(result["data"]["tokenTimestamp"])

        doc = Document.objects.filter(identifier=result["data"]["additionalData"]["bundle"]).first()
        org = Organization.objects.filter(org_name=json_data["organizationId"]).first()
        cert = Certificate.objects.filter(organization=org, is_revoked=False).first()

        # asserts for doc - change - signature is none
        self.assertEqual("http://127.0.1.1.com:8000/api/v1/organizations/UniGraz/documents/01_sample_acquisition",
                         doc.identifier)
        self.assertEqual(cert, doc.certificate)
        self.assertEqual(org, doc.organization)
        self.assertEqual("backbone", doc.document_type)
        self.assertEqual(str(base64.b64encode(graph.encode('utf-8'))), doc.document_text)
        self.assertEqual(int(self.time1), doc.created_on)
        self.assertEqual(None, doc.signature)
