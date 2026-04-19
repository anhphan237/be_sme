package com.sme.be_sme.modules.identity.bulk;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.identity.bulk-user-import")
public class IdentityBulkUserImportProperties {

    /**
     * Kill-switch for bulk user import APIs.
     */
    private boolean enabled = false;

    /**
     * Hard limit to protect DB + mail pipeline.
     */
    private int maxRows = 500;

    /**
     * Max accepted upload size for bulk import file.
     */
    private long maxFileSizeBytes = 2 * 1024 * 1024;
}
