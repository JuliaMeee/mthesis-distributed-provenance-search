package cz.muni.xmichalk.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "bundle_access")
public class OrgBundleAccess {

    @EmbeddedId
    private OrgBundleKey id;

    @Column(nullable = false)
    private boolean tiAccess;

    @Column(nullable = false)
    private boolean dsiAccess;
    
    public OrgBundleAccess() {}

    public OrgBundleAccess(String orgId, String bundleId, boolean tiAccess, boolean dsiAccess) {
        this.id = new OrgBundleKey(orgId, bundleId);
        this.tiAccess = tiAccess;
        this.dsiAccess = dsiAccess;
    }

    public OrgBundleKey getId() {
        return id;
    }

    public void setId(OrgBundleKey id) {
        this.id = id;
    }

    public boolean hasTiAccess() {
        return tiAccess;
    }

    public void setTiAccess(boolean tiAccess) {
        this.tiAccess = tiAccess;
    }

    public boolean hasDsiAccess() {
        return dsiAccess;
    }

    public void setDsiAccess(boolean dsiAccess) {
        this.dsiAccess = dsiAccess;
    }

    public String getOrgId() {
        return id.getOrgId();
    }

    public String getBundleId() {
        return id.getBundleId();
    }
}


