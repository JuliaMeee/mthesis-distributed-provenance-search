CREATE TABLE org_bundle_access (
                                   bundle_id VARCHAR(64) NOT NULL,
                                   org_id VARCHAR(64) NOT NULL,
                                   ti_access BOOLEAN NOT NULL DEFAULT FALSE,
                                   dsi_access BOOLEAN NOT NULL DEFAULT FALSE,
                                   PRIMARY KEY (bundle_id, org_id)
);