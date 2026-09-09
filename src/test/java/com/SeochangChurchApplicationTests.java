package com;

import com.seochang.church.SeochangChurchApplication;
import com.seochang.church.entity.*;
import com.seochang.church.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SeochangChurchApplication.class, properties = {
    "spring.datasource.url=jdbc:h2:mem:church;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS seochang_church_db",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class SeochangChurchApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BoardRepository boards;
    @Autowired com.seochang.church.service.BoardLikeService likes;
    @Autowired com.seochang.church.service.UserService userService;

    private User user(String name) {
        User user = new User(name, "unused", name, null, null);
        user.setApproved(true);
        return users.saveAndFlush(user);
    }
    private MockHttpSession session(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", user);
        session.setAttribute("csrfToken", "test-token");
        return session;
    }
    @Test void loginRendersCsrfTokenAndRejectsMissingToken() throws Exception {
        var result = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        String token = (String) result.getRequest().getSession().getAttribute("csrfToken");
        assertThat(token).isNotBlank();
        assertThat(result.getResponse().getContentAsString()).contains(token);
        mvc.perform(post("/signup").param("username", "test")).andExpect(status().isForbidden());
    }
    @Test void createIgnoresExistingIdAndServerManagedFields() throws Exception {
        User owner = user("owner");
        Board original = boards.saveAndFlush(new Board("original", "body", owner.getName(), owner.getId()));
        User other = user("other");
        mvc.perform(post("/boards/new").session(session(other)).param("_csrf", "test-token")
                .param("id", original.getId().toString()).param("title", "new post").param("content", "new body")
                .param("writerId", owner.getId().toString()).param("likeCount", "999").param("delYn", "Y"))
                .andExpect(status().is3xxRedirection());
        assertThat(boards.findById(original.getId()).orElseThrow().getTitle()).isEqualTo("original");
        Board created = boards.findAll().stream().filter(b -> "new post".equals(b.getTitle())).findFirst().orElseThrow();
        assertThat(created.getWriterId()).isEqualTo(other.getId());
        assertThat(created.getLikeCount()).isZero();
        assertThat(created.getDelYn()).isEqualTo("N");
    }
    @Test void anonymousUploadIsRejectedEvenWithCsrfToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("csrfToken", "test-token");
        mvc.perform(multipart("/api/images/upload").file("image", new byte[]{1}).session(session)
                .param("_csrf", "test-token")).andExpect(status().isUnauthorized());
    }
    @Test void removedMemberCannotUseExistingSession() throws Exception {
        User user = user("removed");
        MockHttpSession session = session(user);
        user.setDelYn("Y");
        users.saveAndFlush(user);
        mvc.perform(get("/boards").session(session)).andExpect(redirectedUrl("/login?error=login-required"));
    }
    @Test void demotedAdminCannotOpenAdminPage() throws Exception {
        User current = user("demoted");
        User stale = new User();
        stale.setId(current.getId());
        stale.setRole("ADMIN");
        mvc.perform(get("/admin").session(session(stale))).andExpect(redirectedUrl("/?error=admin-only"));
    }
    @Test void likeToggleKeepsCountConsistent() {
        User user = user("liker");
        Board board = boards.saveAndFlush(new Board("post", "body", user.getName(), user.getId()));
        assertThat(likes.toggle(board.getId(), user).likeCount()).isEqualTo(1);
        assertThat(likes.toggle(board.getId(), user).likeCount()).isZero();
    }
    @Test void ajaxCsrfHeaderAllowsAuthenticatedComment() throws Exception {
        User user = user("commenter");
        Board board = boards.saveAndFlush(new Board("post", "body", user.getName(), user.getId()));
        mvc.perform(post("/api/boards/" + board.getId() + "/comments").session(session(user))
                .header("X-CSRF-TOKEN", "test-token").contentType("application/json").content("{\"content\":\"hello\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
    @Test void blankPostIsRejected() throws Exception {
        mvc.perform(post("/boards/new").session(session(user("blank"))).param("_csrf", "test-token")
                .param("title", " ").param("content", "body")).andExpect(status().isBadRequest());
        assertThat(boards.count()).isZero();
    }
    @Test void logoutRequiresPostAndCsrf() throws Exception {
        MockHttpSession session = session(user("logout"));
        mvc.perform(get("/logout").session(session)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/logout").session(session).param("_csrf", "test-token")).andExpect(redirectedUrl("/"));
        assertThat(session.isInvalid()).isTrue();
    }
    @Test void adminLayoutAndBoardFormRenderTokens() throws Exception {
        User admin = user("admin"); admin.setRole("ADMIN"); users.saveAndFlush(admin);
        mvc.perform(get("/admin/banners").session(session(admin))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("test-token")));
        mvc.perform(get("/boards/new").session(session(admin))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("toastui-editor-all.min.js")));
    }
    @Test void adminCanResetUserPasswordAndUserCanLoginWithTempPassword() throws Exception {
        User admin = user("admin-reset"); admin.setRole("ADMIN"); users.saveAndFlush(admin);
        User member = user("member-reset");
        var result = mvc.perform(post("/admin/users/" + member.getId() + "/reset-password")
                .session(session(admin)).param("_csrf", "test-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("resetPasswordSuccess", true))
                .andExpect(flash().attributeExists("tempPassword"))
                .andReturn();
        String tempPassword = (String) result.getFlashMap().get("tempPassword");
        assertThat(tempPassword).startsWith("sc").hasSize(8);
        User loggedIn = userService.login(member.getUsername(), tempPassword);
        assertThat(loggedIn.getId()).isEqualTo(member.getId());
    }
    @Test void nonAdminCannotResetPassword() throws Exception {
        User member = user("member-regular");
        User target = user("member-target");
        mvc.perform(post("/admin/users/" + target.getId() + "/reset-password")
                .session(session(member)).param("_csrf", "test-token"))
                .andExpect(redirectedUrl("/?error=admin-only"));
    }
    @Test void cannotResetPasswordForDeletedUser() {
        User deleted = user("deleted-member");
        deleted.setDelYn("Y");
        users.saveAndFlush(deleted);
        assertThatThrownBy(() -> userService.resetPassword(deleted.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}
