package cz.muni.xmichalk;

import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.DocumentLoader.StorageDocumentLoader;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
    public ICpmProvFactory cpmProvFactory(ProvFactory pf) {
        return new CpmProvFactory(pf);
    }

    @Bean
    public IDocumentLoader documentLoader() {
        return new StorageDocumentLoader();
    }

    @Bean
    public BundleSearchService bundleSearchService(IDocumentLoader documentLoader, ProvFactory provFactory,  ICpmFactory cpmFactory,  ICpmProvFactory cpmProvFactory) {return new BundleSearchService(documentLoader, provFactory, cpmFactory, cpmProvFactory);}
}
