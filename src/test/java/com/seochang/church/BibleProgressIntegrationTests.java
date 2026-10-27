package com.seochang.church;

import com.seochang.church.entity.User;
import com.seochang.church.repository.BibleReadingProgressRepository;
import com.seochang.church.repository.UserRepository;
import com.seochang.church.service.BibleProgressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SeochangChurchApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:bibleprogress;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS seochang_church_db",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class BibleProgressIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BibleReadingProgressRepository progressRepository;

    @Autowired
    private BibleProgressService progressService;

    private User createUser(String username) {
        User u = new User(username, "encoded-pass", "교우_" + username, "요셉", username + "@example.com");
        u.setApproved(true);
        return userRepository.saveAndFlush(u);
    }

    private MockHttpSession sessionFor(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", user);
        session.setAttribute("csrfToken", "bible-csrf-token");
        return session;
    }

    @Test
    @DisplayName("가톨릭 성경 73권(구약 46권 1,068장, 신약 27권 260장, 합계 1,328장) 상수 검증")
    void verifyCatholicBibleCanonCounts() {
        assertThat(BibleProgressService.OLD_TESTAMENT_BOOKS).isEqualTo(46);
        assertThat(BibleProgressService.NEW_TESTAMENT_BOOKS).isEqualTo(27);
        assertThat(BibleProgressService.TOTAL_BOOKS).isEqualTo(73);
        assertThat(BibleProgressService.OLD_TESTAMENT_TOTAL_CHAPTERS).isEqualTo(1068);
        assertThat(BibleProgressService.NEW_TESTAMENT_TOTAL_CHAPTERS).isEqualTo(260);
        assertThat(BibleProgressService.TOTAL_CHAPTERS).isEqualTo(1328);
    }

    @Test
    @DisplayName("성경 읽기 페이지(/bible)에 나의 성경 통독 진도표 UI가 렌더링된다")
    void biblePageRendersProgressTracker() throws Exception {
        mvc.perform(get("/bible"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("나의 성경 통독 진도표")))
                .andExpect(content().string(containsString("구약성경 (46권)")))
                .andExpect(content().string(containsString("신약성경 (27권)")));
    }

    @Test
    @DisplayName("비로그인 상태에서 진도 조회는 가능하고, 저장 API는 401을 반환한다")
    void guestCanQueryProgressButCannotMutateServerSide() throws Exception {
        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("csrfToken", "guest-csrf");

        mvc.perform(get("/api/bible/progress").session(guestSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.readChapters").isEmpty())
                .andExpect(jsonPath("$.stats.totalChapters").value(1328));

        mvc.perform(post("/api/bible/progress/toggle")
                        .session(guestSession)
                        .header("X-CSRF-TOKEN", "guest-csrf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testament\":1,\"book\":1,\"chapter\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 회원의 장별 읽음 체크/해제 및 권 전체 완독 처리·퍼센트 계산 검증")
    void authenticatedUserCanToggleChaptersAndCompleteBooks() throws Exception {
        User user = createUser("bible-reader");
        User other = createUser("other-reader");
        MockHttpSession session = sessionFor(user);

        // 1. 창세기(구약 1권) 1장 읽음 체크
        mvc.perform(post("/api/bible/progress/toggle")
                        .session(session)
                        .header("X-CSRF-TOKEN", "bible-csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testament\":1,\"book\":1,\"chapter\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.key").value("1-1-1"))
                .andExpect(jsonPath("$.stats.oldTestamentRead").value(1))
                .andExpect(jsonPath("$.stats.totalRead").value(1));

        // 2. 요한 둘째 서간(신약 24권, 총 1장짜리 성경) 전체 읽음 처리 -> 신약 완독 권수 1 증가 확인
        mvc.perform(post("/api/bible/progress/book")
                        .session(session)
                        .header("X-CSRF-TOKEN", "bible-csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testament\":2,\"book\":24,\"read\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.newTestamentRead").value(1))
                .andExpect(jsonPath("$.stats.newTestamentCompletedBooks").value(1))
                .andExpect(jsonPath("$.stats.completedBooks").value(1))
                .andExpect(jsonPath("$.readChapters", hasItems("1-1-1", "2-24-1")));

        // 3. 다른 회원의 진도에는 영향을 주지 않는지 확인
        assertThat(progressService.getProgressSummary(other.getId()).get("readChapters"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .isEmpty();

        // 4. 창세기 1장 다시 토글 -> 읽음 해제 확인
        mvc.perform(post("/api/bible/progress/toggle")
                        .session(session)
                        .header("X-CSRF-TOKEN", "bible-csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testament\":1,\"book\":1,\"chapter\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(false))
                .andExpect(jsonPath("$.stats.oldTestamentRead").value(0));

        assertThat(progressRepository.findByUser_IdOrderByTestamentAscBookAscChapterAsc(user.getId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("유효하지 않은 권·장 번호 요청 시 400 Bad Request를 반환한다")
    void invalidBibleCoordinatesReturnBadRequest() throws Exception {
        User user = createUser("validator-user");
        MockHttpSession session = sessionFor(user);

        // 창세기(50장까지 있음)에 51장 요청
        mvc.perform(post("/api/bible/progress/toggle")
                        .session(session)
                        .header("X-CSRF-TOKEN", "bible-csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testament\":1,\"book\":1,\"chapter\":51}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 신약 28권(신약은 27권까지) 요청
        mvc.perform(post("/api/bible/progress/book")
                        .session(session)
                        .header("X-CSRF-TOKEN", "bible-csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testament\":2,\"book\":28,\"read\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
