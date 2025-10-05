package cz.muni.xmichalk.persistence;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DbSeeder implements CommandLineRunner {

    private final BundleAccessRepository repository;

    public DbSeeder(BundleAccessRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        repository.save(new OrgBundleAccess("org1", "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/SamplingBundle_V0", true, false));
        repository.save(new OrgBundleAccess("org1", "http://prov-storage-2:8000/api/v1/organizations/ORG2/documents/ProcessingBundle_V0", false, false));

        
        System.out.println("=== Access Rights in DB at startup ===");
        repository.findAll().forEach(entry -> {
            System.out.printf(
                    "Org: %s | Bundle: %s | TI: %s | DSI: %s%n",
                    entry.getOrgId(),
                    entry.getBundleId(),
                    entry.hasTiAccess(),
                    entry.hasDsiAccess()
            );
        });
        System.out.println("======================================");
    }
}
