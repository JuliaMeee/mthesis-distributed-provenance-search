package cz.muni.xmichalk;

import cz.muni.xmichalk.config.Settings;
import cz.muni.xmichalk.persistence.BundleAccessRepository;
import cz.muni.xmichalk.persistence.OrgBundleAccess;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final BundleAccessRepository repository;
    private final Settings settings;

    public AuthorizationService(BundleAccessRepository repository, Settings settings) {
        this.repository = repository;
        this.settings = settings;
    }

    public OrgBundleAccess getAccess(String orgId, String bundleId) {
        return repository.findByOrgIdAndBundleId(orgId, bundleId)
                .orElse(
                        new OrgBundleAccess(
                                orgId,
                                bundleId,
                                settings.isAccessGrantedByDefault(),
                                settings.isAccessGrantedByDefault()
                        ));
    }
}
                                       