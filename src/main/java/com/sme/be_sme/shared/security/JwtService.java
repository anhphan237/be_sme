package com.sme.be_sme.shared.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    public String issueAccessToken(String userId, String tenantId, Set<String> roles) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plusSeconds(props.getAccessTtlSeconds());

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId)                 // sub
                    .claim("tid", tenantId)          // tenantId
                    .claim("roles", new ArrayList<>(roles))
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            SignedJWT jwt = new SignedJWT(header, claims);

            JWSSigner signer = new MACSigner(secretBytes());
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Issue JWT failed", e);
        }
    }

    public JwtPrincipal verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(secretBytes());
            if (!jwt.verify(verifier)) {
                throw new SecurityException("Invalid JWT signature");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            Date exp = claims.getExpirationTime();
            if (exp == null || exp.before(new Date())) {
                throw new SecurityException("JWT expired");
            }

            String userId = claims.getSubject();
            String tenantId = (String) claims.getClaim("tid");

            @SuppressWarnings("unchecked")
            List<String> rolesList = (List<String>) claims.getClaim("roles");
            Set<String> roles = rolesList == null ? Set.of() : new HashSet<>(rolesList);

            if (userId == null || tenantId == null) {
                throw new SecurityException("Missing required claims");
            }

            return new JwtPrincipal(userId, tenantId, roles);
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new SecurityException("Invalid JWT", e);
        }
    }

    private byte[] secretBytes() {
        return props.getHmacSecret().getBytes(StandardCharsets.UTF_8);
    }
}
