package com.seochang.church;

import com.seochang.church.entity.*;
import com.seochang.church.repository.*;
import com.seochang.church.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SeochangChurchApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:notifications;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS seochang_church_db",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class NotificationIntegrationTests {
    @Test void deletingReadNotificationsPreservesUnreadAndOtherRecipients() throws Exception {
        User owner = user("delete-owner"), other = user("delete-other");
        Notification read = alert(owner), unread = alert(owner), foreign = alert(other);
        notifications.markRead(owner.getId(), read.getId());
        notifications.markRead(other.getId(), foreign.getId());
        mvc.perform(post("/notifications/" + read.getId() + "/delete").session(session(owner)))
                .andExpect(status().isForbidden());
        for (Long id : java.util.List.of(unread.getId(), foreign.getId(), read.getId())) {
            mvc.perform(post("/notifications/" + id + "/delete").session(session(owner))
                    .param("_csrf", "notification-test-token")).andExpect(status().is3xxRedirection());
        }
        assertThat(repository.existsById(read.getId())).isFalse();
        assertThat(repository.existsById(unread.getId())).isTrue();
        assertThat(repository.existsById(foreign.getId())).isTrue();
        assertThat(notifications.unreadCount(owner.getId())).isEqualTo(1);
    }

    @Test void bulkDeleteOnlyRemovesOwnedReadNotificationsAndRepairsPage() throws Exception {
        User owner = user("bulk-owner"), other = user("bulk-other");
        for (int i = 0; i < 21; i++) {
            Notification n = alert(owner);
            notifications.markRead(owner.getId(), n.getId());
        }
        Notification unread = alert(owner), foreign = alert(other);
        notifications.markRead(other.getId(), foreign.getId());
        mvc.perform(get("/notifications").session(session(owner)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("읽은 알림 모두 삭제")));
        mvc.perform(post("/notifications/delete-read").session(session(owner)).param("_csrf", "notification-test-token"))
                .andExpect(redirectedUrl("/notifications"));
        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.existsById(unread.getId())).isTrue();
        assertThat(repository.existsById(foreign.getId())).isTrue();
        assertThat(notifications.list(owner.getId(), 1, false).getNumber()).isZero();
        assertThat(notifications.deleteAllRead(owner.getId())).isZero();
    }
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BoardRepository boards;
    @Autowired BoardCommentRepository comments;
    @Autowired NotificationRepository repository;
    @Autowired NoticeService notices;
    @Autowired UserService userService;
    @Autowired NotificationService notifications;
    @Autowired EntityManager entityManager;

    private User user(String name) {
        User user = new User(name, "unused", name, null, null);
        user.setApproved(true);
        return users.saveAndFlush(user);
    }
    private MockHttpSession session(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", user);
        session.setAttribute("csrfToken", "notification-test-token");
        return session;
    }
    private Board board(User user) { return boards.saveAndFlush(new Board("게시글", "내용", user.getName(), user.getId())); }
    private Notification alert(User user) {
        return repository.saveAndFlush(new Notification(user.getId(), Notification.Type.APPROVAL, "승인되었습니다.", null));
    }
    private void comment(Board board, User writer, Long parent, boolean secret) throws Exception {
        String body = "{\"content\":\"알림에 노출하면 안 되는 내용\",\"secret\":" + secret
                + (parent == null ? "" : ",\"parentId\":" + parent) + "}";
        mvc.perform(post("/api/boards/" + board.getId() + "/comments").session(session(writer))
                .header("X-CSRF-TOKEN", "notification-test-token").contentType("application/json").content(body))
                .andExpect(status().isOk());
    }

    @Test void newNoticeNotifiesOnlyActiveApprovedMembersAndEditDoesNotRepeat() {
        User active = user("active");
        User pending = user("pending"); pending.setApproved(false);
        User deleted = user("deleted"); deleted.setDelYn("Y");
        users.flush();
        Notice notice = notices.saveNotice(new Notice("새 공지", "내용"));
        assertThat(notifications.unreadCount(active.getId())).isEqualTo(1);
        assertThat(notifications.unreadCount(pending.getId())).isZero();
        assertThat(notifications.unreadCount(deleted.getId())).isZero();
        notice.setTitle("수정한 공지"); notices.saveNotice(notice);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test void firstApprovalCreatesOneNotification() {
        User pending = user("pending"); pending.setApproved(false); users.flush();
        userService.approveUser(pending.getId()); userService.approveUser(pending.getId());
        assertThat(notifications.list(pending.getId(), 0, false).getContent()).singleElement()
                .extracting(Notification::getType).isEqualTo(Notification.Type.APPROVAL);
    }

    @Test void commentNotifiesOwnerButDoesNotNotifyItsAuthorOrExposeContent() throws Exception {
        User owner = user("owner"); User writer = user("writer"); Board board = board(owner);
        comment(board, writer, null, false);
        assertThat(notifications.unreadCount(owner.getId())).isEqualTo(1);
        assertThat(notifications.unreadCount(writer.getId())).isZero();
        assertThat(notifications.list(owner.getId(), 0, false).getContent().get(0).getMessage())
                .doesNotContain("알림에 노출하면 안 되는 내용");
        comment(board, owner, null, false);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test void replyNotifiesBothOwnersOnceAndDeduplicatesSameRecipient() throws Exception {
        User owner = user("owner"); User parentWriter = user("parent"); User writer = user("writer");
        Board board = board(owner);
        BoardComment parent = comments.saveAndFlush(new BoardComment(board, "원댓글", "parent", parentWriter.getId(), "N"));
        comment(board, writer, parent.getId(), false);
        assertThat(notifications.unreadCount(owner.getId())).isEqualTo(1);
        assertThat(notifications.list(parentWriter.getId(), 0, false).getContent()).singleElement()
                .extracting(Notification::getType).isEqualTo(Notification.Type.REPLY);
        BoardComment ownerComment = comments.saveAndFlush(new BoardComment(board, "원댓글", "owner", owner.getId(), "N"));
        comment(board, writer, ownerComment.getId(), false);
        assertThat(notifications.unreadCount(owner.getId())).isEqualTo(2);
        assertThat(notifications.unreadCount(writer.getId())).isZero();
    }

    @Test void secretReplyDoesNotNotifyMemberWhoCannotReadIt() throws Exception {
        User owner = user("owner"); User parentWriter = user("parent"); User writer = user("writer");
        Board board = board(owner);
        BoardComment parent = comments.saveAndFlush(new BoardComment(board, "원댓글", "parent", parentWriter.getId(), "N"));
        comment(board, writer, parent.getId(), true);
        assertThat(notifications.unreadCount(owner.getId())).isEqualTo(1);
        assertThat(notifications.unreadCount(parentWriter.getId())).isZero();
    }

    @Test void foreignParentCannotGenerateNotification() throws Exception {
        User owner = user("owner"); User writer = user("writer"); Board first = board(owner); Board other = board(writer);
        BoardComment parent = comments.saveAndFlush(new BoardComment(other, "다른 글 댓글", "writer", writer.getId(), "N"));
        mvc.perform(post("/api/boards/" + first.getId() + "/comments").session(session(writer))
                .header("X-CSRF-TOKEN", "notification-test-token").contentType("application/json")
                .content("{\"content\":\"reply\",\"parentId\":" + parent.getId() + "}"))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test void notificationReadsAndCountsAreRecipientScoped() throws Exception {
        User first = user("first"); User other = user("other"); Notification own = alert(first); Notification foreign = alert(other);
        mvc.perform(get("/api/notifications/unread-count").session(session(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(post("/notifications/" + foreign.getId() + "/read").session(session(first)).param("_csrf", "notification-test-token"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/notifications/" + foreign.getId() + "/open").session(session(first)).param("_csrf", "notification-test-token"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/notifications/" + own.getId() + "/read").session(session(first)).param("_csrf", "notification-test-token"))
                .andExpect(redirectedUrl("/notifications"));
        assertThat(notifications.unreadCount(first.getId())).isZero();
        assertThat(notifications.unreadCount(other.getId())).isEqualTo(1);
    }

    @Test void markAllReadDoesNotAffectOtherMembersAndUnreadFilterIsEmpty() throws Exception {
        User first = user("first"); User other = user("other"); alert(first); alert(first); alert(other);
        mvc.perform(post("/notifications/read-all").session(session(first)).param("_csrf", "notification-test-token"))
                .andExpect(redirectedUrl("/notifications"));
        entityManager.clear();
        assertThat(notifications.list(first.getId(), 0, true)).isEmpty();
        assertThat(notifications.list(first.getId(), 0, false)).hasSize(2);
        assertThat(notifications.unreadCount(other.getId())).isEqualTo(1);
    }

    @Test void anonymousRequestsAndMissingCsrfAreRejected() throws Exception {
        mvc.perform(get("/notifications")).andExpect(redirectedUrl("/login?error=login-required"));
        mvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
        mvc.perform(post("/notifications/read-all").session(session(user("member")))).andExpect(status().isForbidden());
    }

    @Test void openMarksReadAndHandlesDeletedTargets() throws Exception {
        User member = user("member"); Notice notice = notices.saveNotice(new Notice("공지", "내용"));
        Notification notification = notifications.list(member.getId(), 0, false).getContent().get(0);
        mvc.perform(post("/notifications/" + notification.getId() + "/open").session(session(member)).param("_csrf", "notification-test-token"))
                .andExpect(redirectedUrl("/notices/" + notice.getId()));
        assertThat(notifications.unreadCount(member.getId())).isZero();
        notices.deleteNotice(notice.getId());
        mvc.perform(post("/notifications/" + notification.getId() + "/open").session(session(member)).param("_csrf", "notification-test-token"))
                .andExpect(redirectedUrl("/notifications")).andExpect(flash().attributeExists("notificationMessage"));
    }

    @Test void listsOnlyOwnNotificationsEscapesMessagesAndPaginates() throws Exception {
        User first = user("first"); User other = user("other");
        repository.save(new Notification(other.getId(), Notification.Type.APPROVAL, "다른 회원 전용", null));
        repository.save(new Notification(first.getId(), Notification.Type.APPROVAL, "<img src=x onerror=alert(1)>", null));
        for (int i = 0; i < 24; i++) alert(first);
        assertThat(notifications.list(first.getId(), 0, false)).hasSize(20);
        assertThat(notifications.list(first.getId(), 1, false)).hasSize(5);
        mvc.perform(get("/notifications?page=1").session(session(first))).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;img")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("다른 회원 전용"))));
    }

    @Test void businessRollbackAlsoRollsBackNotifications() {
        user("member"); notices.saveNotice(new Notice("롤백 공지", "내용"));
        assertThat(repository.count()).isEqualTo(1);
        TestTransaction.flagForRollback(); TestTransaction.end();
        assertThat(repository.count()).isZero();
    }
}
