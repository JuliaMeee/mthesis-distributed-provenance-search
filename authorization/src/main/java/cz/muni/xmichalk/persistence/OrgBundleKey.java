package cz.muni.xmichalk.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OrgBundleKey implements Serializable {

    @Column(nullable = false)
    private String orgId;

    @Column(nullable = false)
    private String bundleId;

    public OrgBundleKey() {}

    public OrgBundleKey(String orgId, String bundleId) {
        this.orgId = orgId;
        this.bundleId = bundleId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getBundleId() {
        return bundleId;
    }

    public void setBundleId(String bundleId) {
        this.bundleId = bundleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrgBundleKey)) return false;
        OrgBundleKey that = (OrgBundleKey) o;
        return Objects.equals(orgId, that.orgId) && Objects.equals(bundleId, that.bundleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, bundleId);
    }
}

