package com.seochang.church.config;

import com.seochang.church.entity.User;
import com.seochang.church.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitialAdminInitializerTests {
    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private InitialAdminInitializer initializer(String username, String password) {
        return new InitialAdminInitializer(users, encoder, username, password, "관리자");
    }

    @Test void createsApprovedAdminWithHashedPasswordAndDoesNotRepeat() {
        when(users.existsByRoleAndApprovedTrueAndDelYn("ADMIN", "N")).thenReturn(false, true);
        var initializer = initializer("admin", "unique-password-2026");
        initializer.run(null);
        initializer.run(null);
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users, times(1)).saveAndFlush(saved.capture());
        User admin = saved.getValue();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getRole()).isEqualTo("ADMIN");
        assertThat(admin.isApproved()).isTrue();
        assertThat(admin.getDelYn()).isEqualTo("N");
        assertThat(admin.getPassword()).isNotEqualTo("unique-password-2026");
        assertThat(encoder.matches("unique-password-2026", admin.getPassword())).isTrue();
    }

    @Test void existingAdminIsPreservedEvenIfBootstrapCredentialsAreEmpty() {
        when(users.existsByRoleAndApprovedTrueAndDelYn("ADMIN", "N")).thenReturn(true);
        initializer("", "").run(null);
        verify(users, never()).saveAndFlush(any());
        verify(users, never()).existsByUsername(any());
    }

    @Test void existingUsernameIsNeverPromotedOrReset() {
        when(users.existsByUsername("member")).thenReturn(true);
        assertThatThrownBy(() -> initializer("member", "unique-password-2026").run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("중복");
        verify(users, never()).saveAndFlush(any());
    }

    @Test void missingWeakAndOverlongPasswordsAreRejectedWithoutLeakingValues() {
        for (String password : new String[] {"", "short", " ".repeat(12), "가".repeat(25)}) {
            assertThatThrownBy(() -> initializer("admin", password).run(null))
                    .isInstanceOf(IllegalStateException.class).hasMessageStartingWith("INITIAL_ADMIN_PASSWORD는");
        }
        verify(users, never()).saveAndFlush(any());
    }

    @Test void invalidUsernameAndEmptyNameAreRejected() {
        assertThatThrownBy(() -> initializer("bad name", "unique-password-2026").run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("INITIAL_ADMIN_USERNAME");
        assertThatThrownBy(() -> new InitialAdminInitializer(users, encoder, "admin", "unique-password-2026", " ").run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("INITIAL_ADMIN_NAME");
        verify(users, never()).saveAndFlush(any());
    }
}
