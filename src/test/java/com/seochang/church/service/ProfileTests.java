package com.seochang.church.service;

import com.seochang.church.controller.ProfileController;
import com.seochang.church.entity.User;
import com.seochang.church.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfileTests {
    @Test void adminProfileUsesAdminPageAndRedirectsAfterSaving() throws Exception {
        User user = member();
        user.setRole("ADMIN");
        var mvc = MockMvcBuilders.standaloneSetup(new ProfileController(service)).build();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", user);
        mvc.perform(get("/profile").session(session)).andExpect(redirectedUrl("/admin/profile"));
        mvc.perform(get("/admin/profile").session(session)).andExpect(view().name("admin/profile"))
                .andExpect(model().attribute("profileAction", "/admin/profile"));
        mvc.perform(post("/admin/profile").session(session).param("name", "관리자")
                .param("currentPassword", "old-password")).andExpect(redirectedUrl("/admin/profile"));
        mvc.perform(post("/admin/profile").session(session).param("name", "관리자")
                .param("currentPassword", "wrong")).andExpect(view().name("admin/profile"))
                .andExpect(model().attribute("profileAction", "/admin/profile"));
    }
    private final UserRepository repository = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserService service = new UserService(repository, encoder, mock(NotificationService.class));

    private User member() {
        User user = new User("member", encoder.encode("old-password"), "기존 이름", "", "");
        user.setId(7L); user.setApproved(true);
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        return user;
    }

    @Test void updatesProfileAndHashesNewPassword() {
        User user = member();
        service.updateProfile(7L, " 새 이름 ", "마리아", "test@example.com", "1구역", "old-password", "new-password", "new-password");
        assertThat(user.getName()).isEqualTo("새 이름");
        assertThat(encoder.matches("new-password", user.getPassword())).isTrue();
        assertThat(user.getUsername()).isEqualTo("member");
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(user.isApproved()).isTrue();
    }

    @Test void wrongPasswordAndInvalidFieldsDoNotChangeMember() {
        User user = member();
        assertThatThrownBy(() -> service.updateProfile(7L, "변경", "", "", "", "wrong", "", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateProfile(7L, "변경", "", "invalid", "", "old-password", "", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateProfile(7L, "변경", "", "", "", "old-password", "new-password", "mismatch")).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getName()).isEqualTo("기존 이름");
        verify(repository, never()).save(any());
    }

    @Test void blankNewPasswordPreservesPasswordAndInactiveMemberIsRejected() {
        User user = member();
        String hash = user.getPassword();
        service.updateProfile(7L, "수정", "", "", "", "old-password", "", "");
        assertThat(user.getPassword()).isEqualTo(hash);
        user.setDelYn("Y");
        assertThatThrownBy(() -> service.updateProfile(7L, "변경", "", "", "", "old-password", "", "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void controllerRequiresLoginAndIgnoresPostedIdentityAndRole() throws Exception {
        User user = member();
        var mvc = MockMvcBuilders.standaloneSetup(new ProfileController(service)).build();
        mvc.perform(get("/profile")).andExpect(redirectedUrl("/login"));
        MockHttpSession session = new MockHttpSession(); session.setAttribute("loginUser", user);
        mvc.perform(post("/profile").session(session).param("id", "99").param("role", "ADMIN")
                .param("name", "변경").param("currentPassword", "old-password"))
                .andExpect(redirectedUrl("/profile"));
        assertThat(((User) session.getAttribute("loginUser")).getName()).isEqualTo("변경");
        assertThat(user.getRole()).isEqualTo("USER");
        verify(repository, never()).findById(99L);
    }
}
