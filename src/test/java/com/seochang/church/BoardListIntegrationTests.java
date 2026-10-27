package com.seochang.church;

import com.seochang.church.entity.*;
import com.seochang.church.repository.*;
import com.seochang.church.service.BoardService;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SeochangChurchApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:boardux;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS seochang_church_db",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class BoardListIntegrationTests {
    @Autowired EntityManager em;
    @Autowired BoardRepository boards;
    @Autowired UserRepository users;
    @Autowired BoardService service;
    @Autowired MockMvc mvc;

    private User member() {
        User user = new User("list-member", "unused", "교우", null, null);
        user.setApproved(true); return users.saveAndFlush(user);
    }
    private Board post(User user, String title) {
        return boards.saveAndFlush(new Board(title, "본문", user.getName(), user.getId()));
    }

    @Test void aggregatesUseTwoQueriesAndNeverLoadCommentBodiesOrAttachmentCollections() {
        User user = member(); Board current = post(user, "새 글"), old = post(user, "이전 글");
        old.setCreatedAt(LocalDateTime.now().minusDays(2));
        BoardComment parent = new BoardComment(current, "비밀 본문", "교우", user.getId(), "Y"); em.persist(parent);
        BoardComment reply = new BoardComment(current, "답글", "교우", user.getId(), "N");
        reply.setParentId(parent.getId()); em.persist(reply);
        BoardComment deleted = new BoardComment(current, "삭제 댓글", "교우", user.getId(), "N");
        deleted.setDelYn("Y"); em.persist(deleted);
        for (boolean image : new boolean[] {true, false}) {
            BoardAttachment attachment = new BoardAttachment(); attachment.setBoard(current);
            attachment.setOriginalFileName("첨부"); attachment.setStoredFileName("test");
            attachment.setFilePath("/uploads/board/test"); attachment.setFileSize(1L); attachment.setImage(image); em.persist(attachment);
        }
        em.flush(); em.clear();
        List<Board> page = service.getActiveBoardsByCategory("free", 0, null).getContent();
        var statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled(); statistics.setStatisticsEnabled(true);
        long before = statistics.getPrepareStatementCount();
        try {
            var counts = service.listStats(page);
            assertThat(statistics.getPrepareStatementCount() - before).isEqualTo(2);
            assertThat(counts.get(current.getId()).commentCount()).isEqualTo(2);
            assertThat(counts.get(current.getId()).attachmentCount()).isEqualTo(2);
            assertThat(counts.get(current.getId()).newPost()).isTrue();
            assertThat(counts.get(old.getId()).commentCount()).isZero();
            assertThat(counts.get(old.getId()).attachmentCount()).isZero();
            assertThat(counts.get(old.getId()).newPost()).isFalse();
            for (Board board : page) {
                assertThat(Hibernate.isInitialized(board.getComments())).isFalse();
                assertThat(Hibernate.isInitialized(board.getAttachments())).isFalse();
            }
            long after = statistics.getPrepareStatementCount();
            assertThat(service.listStats(List.of())).isEmpty();
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(after);
        } finally { statistics.setStatisticsEnabled(enabled); }
    }

    @Test void listRendersMetricsAndPreservesSearchAndCategory() throws Exception {
        User user = member(); Board post = post(user, "목록 표시 확인"); post.setLikeCount(7);
        BoardComment comment = new BoardComment(post, "노출 금지 댓글 본문", "교우", user.getId(), "N"); em.persist(comment);
        BoardAttachment file = new BoardAttachment(); file.setBoard(post); file.setOriginalFileName("자료.pdf");
        file.setStoredFileName("test.pdf"); file.setFilePath("/uploads/board/file/test.pdf"); file.setFileSize(1L); em.persist(file);
        Board council = post(user, "다른 분류"); council.setCategory("council"); em.flush();
        MockHttpSession session = new MockHttpSession(); session.setAttribute("loginUser", user);
        String html = mvc.perform(get("/boards").session(session).param("category", "free").param("keyword", "목록 표시"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var document = Jsoup.parse(html);
        assertThat(document.select(".board-new").size()).isEqualTo(1);
        assertThat(document.select("[data-comment-count]").text()).isEqualTo("1");
        assertThat(document.select("[data-like-count]").text()).isEqualTo("7");
        assertThat(document.select("[data-attachment-count]").text()).isEqualTo("1");
        assertThat(document.select(".board-title-link").text()).contains("목록 표시 확인").doesNotContain("다른 분류");
        assertThat(html).doesNotContain("노출 금지 댓글 본문");
        mvc.perform(get("/boards").session(session).param("keyword", "없는 검색어")).andExpect(status().isOk());
    }

    @Test void newBadgeExcludesOldAndFutureDates() {
        User user = member(); Board old = post(user, "만료"), future = post(user, "미래");
        old.setCreatedAt(LocalDateTime.now().minusHours(25)); future.setCreatedAt(LocalDateTime.now().plusHours(1));
        assertThat(service.listStats(List.of(old, future)).values()).allMatch(stats -> !stats.newPost());
    }
}
