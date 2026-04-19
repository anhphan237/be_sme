package com.sme.be_sme.shared;

import com.sme.be_sme.modules.identity.invite.InviteProperties;
import com.sme.be_sme.modules.identity.bulk.IdentityBulkUserImportProperties;
import com.sme.be_sme.shared.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, InviteProperties.class, IdentityBulkUserImportProperties.class})
public class SecurityConfig {}
