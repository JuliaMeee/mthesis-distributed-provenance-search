from datetime import datetime
from subprocess import call
from time import sleep

from non_repudiation.models import *
from django.test import TestCase
from trusted_party.wsgi import *


class ModelsTestCase(TestCase):
    # uncomment if database is not running
    """success = call("docker compose up -d", shell=True)
    sleep(10)
    # Need to sleep to wait for the test instance to completely come up

    @classmethod
    def tearDownClass(cls):
        success = call("docker compose down", shell=True)
        assert (success == 0)"""

    def test_models(self):
        org = Organization.objects.create(org_name="name")

        time = datetime.now().timestamp()
        cert = Certificate.objects.create(cert_digest="id", cert="string", certificate_type="client",
                                          received_on=int(time), organization=org)

        doc = Document.objects.create(identifier="id", certificate=cert, organization=org, document_type="backbone",
                                      document_text="text...", created_on=int(time), doc_format="json")

        token = Token.objects.create(document=doc, hash="hash", hash_function="SHA512", created_on=int(time),
                                     signature="signature...")

        self.assertEqual("name", org.org_name)

        self.assertEqual(org, cert.organization)
        self.assertEqual(int(time), cert.received_on)
        self.assertEqual(False, cert.is_revoked)
        self.assertEqual("client", cert.certificate_type)

        self.assertEqual(org, doc.organization)
        self.assertEqual(cert, doc.certificate)
        self.assertEqual("backbone", doc.document_type)
        self.assertEqual(None, doc.signature)
        self.assertEqual(int(time), doc.created_on)
        self.assertEqual("json", doc.doc_format)

        self.assertEqual(doc, token.document)
        self.assertEqual("hash", token.hash)
        self.assertEqual("SHA512", token.hash_function)
        self.assertEqual(int(time), token.created_on)
        self.assertEqual("signature...", token.signature)
