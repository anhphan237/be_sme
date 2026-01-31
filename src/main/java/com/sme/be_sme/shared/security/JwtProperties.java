package com.sme.be_sme.shared.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String hmacSecret;     // >= 32 chars recommended for HS256
    private long accessTtlSeconds; // e.g. 3600
}
