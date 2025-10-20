package cz.muni.xmichalk.Traverser;

import cz.muni.fi.cpm.merged.CpmMergedFactory;
import cz.muni.fi.cpm.model.ICpmFactory;
import cz.muni.fi.cpm.model.ICpmProvFactory;
import cz.muni.fi.cpm.vanilla.CpmProvFactory;
import cz.muni.xmichalk.DocumentLoader.IDocumentLoader;
import cz.muni.xmichalk.DocumentLoader.StorageDocumentLoader;
import cz.muni.xmichalk.Traverser.ProvServiceTable.IProvServiceTable;
import cz.muni.xmichalk.Traverser.ProvServiceTable.ProvServiceTable;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Provenance search API",
                version = "1.0.0",
                description = "REST API for searching through the provenance chain."
        )
)
public class TraverserConfig {

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
    public IProvServiceTable traverserTable() {
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
    public Traverser traverser(IDocumentLoader loader, ProvFactory pf, ICpmFactory cf, ICpmProvFactory cpf, IProvServiceTable tt) {
        return new Traverser(loader, pf, cf, cpf, tt);
    }
}
