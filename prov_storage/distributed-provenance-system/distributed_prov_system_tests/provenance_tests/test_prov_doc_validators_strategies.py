from datetime import datetime
from unittest import TestCase

import requests
from prov.constants import PROV_TYPE
from prov.model import ProvDocument

import helpers
from constants import CPM_MAIN_ACTIVITY
from prov_doc_validators_strategies import ProvValidatorExternal

class MyTestCase(TestCase):
    prov_validator = ProvValidatorExternal()

    # prefix for recognizing existential variables
    existential_variable_prefix = "_"
    # create class for generating existential identifiers
    existential_variable_id_generator = helpers.ExistentialVariablesGenerator(existential_variable_prefix)
    # placeholder time - use as existential variable in next tests - change to needed time
    placeholder_time = datetime.now()

    def test_validate_with_external_validator_ok(self):
        document, _, _, _, _, _ = helpers.create_cpm_provenance_basic("ex", "storage", "org",
                                                       datetime.now().timestamp(), {PROV_TYPE: CPM_MAIN_ACTIVITY},
                                                       smaller_prov=True)
        result = self.prov_validator.is_valid(document)
        self.assertTrue(result)

    def test_validate_with_external_validator_ok_2(self):
        validator = ProvValidatorExternal()
        document, _, _, _, _, _ = helpers.create_cpm_provenance_basic("ex", "storage", "org",
                                                       datetime.now().timestamp(), {PROV_TYPE: CPM_MAIN_ACTIVITY},
                                                       smaller_prov=False)
        result = validator.is_valid(document)

        self.assertTrue(result)

    def test_validate_with_external_validator_prov_test_cases(self):
        case = 0

        with open("prov_test_cases_xml.txt", "r") as file:
            for line in file:
                case+=1
                #print(case)
                prov_file = requests.get(line.rstrip())
                prov_file = prov_file.content.decode("utf-8")

                document = ProvDocument().deserialize(content=prov_file, format="xml") #loading of document seems ok

                result = self.prov_validator.is_valid(document)
                if "PASS" in line.rstrip():
                    if not result:
                        print(line)
                    self.assertTrue(result, msg=f"test case: {case} file: {line}")
                else:
                    if result:
                        print(line)
                    self.assertFalse(result, msg=f"test case: {case} file: {line}")
