package com.seochang.church.config;

import com.seochang.church.entity.User;
import com.seochang.church.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
@Profile("docker")
@ConditionalOnProperty(name = "app.initial-admin.enabled", havingValue = "true")
public class InitialAdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InitialAdminInitializer.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String name;

    public InitialAdminInitializer(UserRepository users, PasswordEncoder encoder,
                                   @Value("${app.initial-admin.username:}") String username,
                                   @Value("${app.initial-admin.password:}") String password,
                                   @Value("${app.initial-admin.name:관리자}") String name) {
        this.users = users;
        this.encoder = encoder;
        this.username = username.trim();
        this.password = password;
        this.name = name.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Bootstrap only; never reset credentials or promote an existing account.
        if (users.existsByRoleAndApprovedTrueAndDelYn("ADMIN", "N")) {
            log.info("Initial admin creation skipped: an active administrator already exists.");
            return;
        }
        if (!username.matches("[A-Za-z0-9_.-]{4,50}")) {
            throw new IllegalStateException("INITIAL_ADMIN_USERNAME은 영문·숫자·밑줄·점·하이픈 4~50자로 지정해주세요.");
        }
        if (password.isBlank() || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD는 12자 이상, UTF-8 기준 72바이트 이하로 지정해주세요.");
        }
        if (name.isEmpty() || name.length() > 255) {
            throw new IllegalStateException("INITIAL_ADMIN_NAME은 1~255자로 지정해주세요.");
        }
        if (users.existsByUsername(username)) {
            throw new IllegalStateException("초기 관리자 아이디가 기존 회원과 중복됩니다. 다른 INITIAL_ADMIN_USERNAME을 지정해주세요.");
        }
        User admin = new User(username, encoder.encode(password), name, null, null, "ADMIN");
        admin.setApproved(true);
        users.saveAndFlush(admin);
        log.info("Initial administrator created. Disable INITIAL_ADMIN_ENABLED after first login.");
    }
}
