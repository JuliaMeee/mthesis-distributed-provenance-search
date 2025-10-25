package cz.muni.xmichalk.Traverser;

import cz.muni.xmichalk.ProvServiceTable.IProvServiceTable;
import cz.muni.xmichalk.ProvServiceTable.ProvServiceTable;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
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
    public Traverser traverser(IProvServiceTable tt) {
        return new Traverser(tt);
    }
}
