package com.sme.be_sme.shared.security;

import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminSeeder implements ApplicationRunner {

    private static final String PLATFORM_ADMIN_USER_ID = "00000000-0000-0000-0000-000000000003";

    @Value("${app.platform.admin-password:Admin@123}")
    private String adminPassword;

    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;

    @Override
    public void run(ApplicationArguments args) {
        UserEntity user = userMapper.selectByPrimaryKey(PLATFORM_ADMIN_USER_ID);
        if (user == null) {
            log.warn("Platform admin user not found; skipping password seed");
            return;
        }
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            return;
        }
        user.setPasswordHash(passwordHasher.hash(adminPassword));
        user.setUpdatedAt(new Date());
        userMapper.updateByPrimaryKey(user);
        log.info("Platform admin password has been set");
    }
}
