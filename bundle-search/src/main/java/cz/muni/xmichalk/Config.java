package cz.muni.xmichalk;

import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.BundleSearch.BundleSearcherRegistry;
import cz.muni.xmichalk.BundleVersionPicker.VersionPickerRegistry;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.DocumentLoader.StorageDocumentLoader;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Bundle search API",
                version = "1.0.0",
                description = "REST API for searching through the contents of a bundle."
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
    public BundleSearcherRegistry bundleSearcherRegistry() {
        return new BundleSearcherRegistry();
    }
    
    @Bean
    public VersionPickerRegistry versionPickerRegistry(
            IDocumentLoader documentLoader
    ) {
        
        return new VersionPickerRegistry(documentLoader);
    }

    @Bean
    public BundleSearchService bundleSearchService(
            IDocumentLoader documentLoader,
            BundleSearcherRegistry bundleSearcherRegistry
    ) {
        return new BundleSearchService(documentLoader, bundleSearcherRegistry);
    }
}
