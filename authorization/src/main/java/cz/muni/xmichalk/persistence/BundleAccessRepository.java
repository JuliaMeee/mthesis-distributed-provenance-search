package cz.muni.xmichalk.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface BundleAccessRepository extends JpaRepository<OrgBundleAccess, OrgBundleKey> {
    
    Optional<OrgBundleAccess> findById(OrgBundleKey id);
    
    default Optional<OrgBundleAccess> findByOrgIdAndBundleId(String orgId, String bundleId) {
        return findById(new OrgBundleKey(orgId, bundleId));
    }
}
