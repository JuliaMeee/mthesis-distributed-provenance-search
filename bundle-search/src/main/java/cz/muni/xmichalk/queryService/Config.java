package cz.muni.xmichalk.queryService;

import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.documentLoader.IDocumentLoader;
import cz.muni.xmichalk.documentLoader.StorageDocumentLoader;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Bundle query API",
                version = "1.0.0",
                description = "REST API for answering queries about bundles."
        ),
        externalDocs = @ExternalDocumentation(
                description = "Query structure documentation",
                url = "https://github.com/JuliaMeee/mthesis-distributed-provenance-search#query-structure"
        )
)
public class Config {

    @Bean
    public ProvFactory provFactory() {
        return new ProvFactory();
    }

    @Bean
    public ICpmFactory cpmFactory() {
        return new CpmMergedFactory();
    }

    @Bean
    public ICpmProvFactory cpmProvFactory(ProvFactory provFactory) {
        return new CpmProvFactory(provFactory);
    }

    @Bean
    public IDocumentLoader documentLoader(
            ProvFactory provFactory,
            ICpmFactory cpmFactory,
            ICpmProvFactory cpmProvFactory
    ) {
        return new StorageDocumentLoader(provFactory, cpmFactory, cpmProvFactory);
    }

    @Bean
    public BundleQueryService bundleQueryService(
            IDocumentLoader documentLoader
    ) {
        return new BundleQueryService(documentLoader);
    }
}
