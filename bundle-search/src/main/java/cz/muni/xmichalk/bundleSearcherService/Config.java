package cz.muni.xmichalk.bundleSearcherService;

import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.bundleSearch.BundleSearcherProvider;
import cz.muni.xmichalk.bundleSearch.ETargetType;
import cz.muni.xmichalk.bundleSearch.ISearchBundle;
import cz.muni.xmichalk.bundleVersionPicker.EVersionPreferrence;
import cz.muni.xmichalk.bundleVersionPicker.IVersionPicker;
import cz.muni.xmichalk.bundleVersionPicker.VersionPickerProvider;
import cz.muni.xmichalk.documentLoader.IDocumentLoader;
import cz.muni.xmichalk.documentLoader.StorageDocumentLoader;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

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
    public Map<ETargetType, ISearchBundle<?>> bundleSearcherRegistry() {
        return BundleSearcherProvider.getBundleSearchers();
    }

    @Bean
    public Map<EVersionPreferrence, IVersionPicker> versionPickerRegistry(IDocumentLoader documentLoader) {
        return VersionPickerProvider.getVersionPickers(documentLoader);
    }

    @Bean
    public BundleSearchService bundleSearchService(
            IDocumentLoader documentLoader,
            Map<ETargetType, ISearchBundle<?>> bundleSearchers
    ) {
        return new BundleSearchService(documentLoader, bundleSearchers);
    }
}
