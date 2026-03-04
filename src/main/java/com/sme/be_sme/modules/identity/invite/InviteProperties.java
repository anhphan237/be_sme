package com.sme.be_sme.modules.identity.invite;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.invite")
public class InviteProperties {
    /**
     * Frontend base URL for invite links, e.g. https://app.example.com
     */
    private String baseUrl = "http://localhost:3000";

    /**
     * Token validity in hours (default 72)
     */
    private int tokenTtlHours = 72;
}
