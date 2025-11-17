# change - whole file added in this thesis

from abc import ABC, abstractmethod

from prov.constants import PROV_ATTR_SPECIFIC_ENTITY, PROV_ATTR_GENERAL_ENTITY, PROV_TYPE
from prov.model import ProvEntity, ProvBundle, ProvSpecialization, first, ProvRecord

from provenance.constants import CPM_FORWARD_CONNECTOR
from provenance.CPM_helpers import contains_non_backbone_attribute, has_any_cpm_type


class IsBackboneStrategy(ABC):

    @abstractmethod
    def is_backbone_element(self, record: ProvRecord, bundle: ProvBundle):
        pass


class IsBackboneStrategyOriginal(IsBackboneStrategy):

    def is_backbone_element(self, record, bundle):

        # domain if some element not from cpm
        if contains_non_backbone_attribute(record):
            return False

        has_cpm_type = has_any_cpm_type(record)

        general_elements = []
        specializations = bundle.get_records(ProvSpecialization)
        for specialization in specializations:
            if next(iter(specialization.get_attribute(PROV_ATTR_SPECIFIC_ENTITY))) == record.identifier:
                general_elements.append(next(iter(specialization.get_attribute(PROV_ATTR_GENERAL_ENTITY))))

        # has CPM type and is not specialization of other element -> BB
        if has_cpm_type and len(general_elements) == 0:
            return True

        # if is specialization of more tan 1 element -> DS
        if len(general_elements) != 1:
            return False

        # if general element from which was this element specialized has non cpm attribute  -> DS
        general_element = bundle.get_record(general_elements[0])[0]
        if contains_non_backbone_attribute(general_element):
            return False

        # if general element is forward connector and specific element does not have type or is FC -> BB
        return (first(general_element.get_attribute(PROV_TYPE)) == CPM_FORWARD_CONNECTOR
                and (not has_cpm_type or
                     first(record.get_attribute(
                         PROV_TYPE)) == CPM_FORWARD_CONNECTOR))
