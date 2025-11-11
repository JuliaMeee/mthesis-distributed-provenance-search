package cz.muni.xmichalk.Traverser;

import cz.muni.xmichalk.DocumentValidity.EValiditySpecification;
import cz.muni.xmichalk.DocumentValidity.ValidityVerifier.DemoValidityVerifier;
import cz.muni.xmichalk.DocumentValidity.ValidityVerifier.ValidityVerifierRegistry;
import cz.muni.xmichalk.ProvServiceTable.IProvServiceTable;
import cz.muni.xmichalk.ProvServiceTable.ProvServiceTable;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
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
        var table = new ProvServiceTable();
        try {
            var resource = new ClassPathResource("config" + File.separator + "provServiceTable.json");
            table.loadFromJson(resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load traverser table", e);
        }
        return table;
    }

    @Bean
    public ValidityVerifierRegistry validityVerifierRegistry(IProvServiceTable provServiceTable) throws IOException {
        var simpleSemanticResource = new ClassPathResource("validitySpecifications/simpleSemanticConstraints.json");
        var isSamplingBundleResource = new ClassPathResource("validitySpecifications/isSamplingBundle.json");
        var isProcessingBundleResource = new ClassPathResource("validitySpecifications/isProcessingBundle.json");

        return new ValidityVerifierRegistry(
                Map.of(
                        EValiditySpecification.DEMO_SIMPLE_CONSTRAINTS, new DemoValidityVerifier(provServiceTable, simpleSemanticResource.getInputStream()),
                        EValiditySpecification.DEMO_IS_SAMPLING_BUNDLE, new DemoValidityVerifier(provServiceTable, isSamplingBundleResource.getInputStream()),
                        EValiditySpecification.DEMO_IS_PROCESSING_BUNDLE, new DemoValidityVerifier(provServiceTable, isProcessingBundleResource.getInputStream())
                )
        );
    }

    @Bean
    public Traverser traverser(IProvServiceTable provServiceTable, ValidityVerifierRegistry validityVerifierRegistry) {
        return new Traverser(provServiceTable, traverserConcurrencyDegree, validityVerifierRegistry);
    }
}
