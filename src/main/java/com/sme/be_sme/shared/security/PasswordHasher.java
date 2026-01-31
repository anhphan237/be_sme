package com.sme.be_sme.shared.security;

public interface PasswordHasher {
    String hash(String raw);
    boolean matches(String raw, String hashed);
}
