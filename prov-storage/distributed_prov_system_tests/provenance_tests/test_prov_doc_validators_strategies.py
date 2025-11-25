from datetime import datetime
from unittest import TestCase

import requests
from prov.constants import PROV_TYPE
from prov.model import ProvDocument

import helpers
from provenance.constants import CPM_MAIN_ACTIVITY
from provenance.prov_doc_validators_strategies import ProvValidatorExternal

class MyTestCase(TestCase):
    prov_validator = ProvValidatorExternal()

    def test_validate_with_external_validator_ok_returns_true(self):
        document, _, _, _, _, _ = helpers.create_cpm_provenance_basic("ex", "storage", "org",
                                                                      datetime.now().timestamp(), {PROV_TYPE: CPM_MAIN_ACTIVITY},
                                                                      smaller_prov=True)
        result = self.prov_validator.is_valid(document)
        self.assertTrue(result)

    def test_validate_with_external_validator_ok_2_returns_true(self):
        document, _, _, _, _, _ = helpers.create_cpm_provenance_basic("ex", "storage", "org",
                                                                      datetime.now().timestamp(), {PROV_TYPE: CPM_MAIN_ACTIVITY},
                                                                      smaller_prov=False)
        result = self.prov_validator.is_valid(document)

        self.assertTrue(result)

    def test_validate_with_external_validator_prov_test_cases(self):
        case = 0

        # load test cases one by one, check what validator returns
        with open("prov_test_cases_xml.txt", "r") as file:
            for line in file:
                case+=1
                prov_file = requests.get(line.rstrip())
                prov_file = prov_file.content.decode("utf-8")

                document = ProvDocument().deserialize(content=prov_file, format="xml")

                result = self.prov_validator.is_valid(document)
                if "PASS" in line.rstrip():
                    if not result:
                        print(line)
                    self.assertTrue(result, msg=f"test case: {case} file: {line}")
                else:
                    if result:
                        print(line)
                    self.assertFalse(result, msg=f"test case: {case} file: {line}")
