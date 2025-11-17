package cz.muni.xmichalk.traverser;

import cz.muni.xmichalk.models.ItemToSearch;
import cz.muni.xmichalk.provServiceTable.IProvServiceTable;
import cz.muni.xmichalk.provServiceTable.ProvServiceTable;
import cz.muni.xmichalk.searchPriority.ESearchPriority;
import cz.muni.xmichalk.searchPriority.IntegrityThenOrderedValidity;
import cz.muni.xmichalk.validity.DemoValidityVerifier;
import cz.muni.xmichalk.validity.EValidityCheck;
import cz.muni.xmichalk.validity.IValidityVerifier;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Provenance search API",
                version = "1.0.0",
                description = "REST API for searching through the provenance chain."
        )
)
public class TraverserConfig {
    @Value("${traverser.concurrencyDegree:10}")
    private int traverserConcurrencyDegree;

    @Bean
    public IProvServiceTable provServiceTable() {
        ProvServiceTable table = new ProvServiceTable();
        try {
            ClassPathResource resource = new ClassPathResource("provServiceTable.json");
            table.loadFromJson(resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load traverser table", e);
        }
        return table;
    }

    @Bean
    public Map<EValidityCheck, IValidityVerifier> validityVerifiers(IProvServiceTable provServiceTable) throws IOException {
        ClassPathResource simpleSemanticResource =
                new ClassPathResource("validitySpecifications" + File.separator + "simpleSemanticConstraints.json");
        ClassPathResource isSamplingBundleResource =
                new ClassPathResource("validitySpecifications" + File.separator + "isSamplingBundle.json");
        ClassPathResource isProcessingBundleResource =
                new ClassPathResource("validitySpecifications" + File.separator + "isProcessingBundle.json");

        return Map.of(
                EValidityCheck.DEMO_SIMPLE_CONSTRAINTS, new DemoValidityVerifier(provServiceTable, simpleSemanticResource.getInputStream()),
                EValidityCheck.DEMO_IS_SAMPLING_BUNDLE, new DemoValidityVerifier(provServiceTable, isSamplingBundleResource.getInputStream()),
                EValidityCheck.DEMO_IS_PROCESSING_BUNDLE, new DemoValidityVerifier(provServiceTable, isProcessingBundleResource.getInputStream())
        );
    }

    @Bean
    public Map<ESearchPriority, Comparator<ItemToSearch>> searchPriorities() {
        return Map.of(
                ESearchPriority.INTEGRITY_THEN_ORDERED_VALIDITY_CHECKS, new IntegrityThenOrderedValidity()
        );
    }

    @Bean
    public Traverser traverser(
            IProvServiceTable provServiceTable,
            Map<EValidityCheck, IValidityVerifier> validityVerifiers,
            Map<ESearchPriority, Comparator<ItemToSearch>> searchPriorityComparators) {
        return new Traverser(provServiceTable, traverserConcurrencyDegree, validityVerifiers, searchPriorityComparators);
    }
}
