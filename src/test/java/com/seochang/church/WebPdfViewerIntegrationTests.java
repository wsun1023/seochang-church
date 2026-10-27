package com.seochang.church;

import com.seochang.church.entity.Notice;
import com.seochang.church.entity.NoticeAttachment;
import com.seochang.church.repository.NoticeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SeochangChurchApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:webpdfviewer;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS seochang_church_db",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class WebPdfViewerIntegrationTests {

    @TempDir
    static Path tempUploadDir;

    @DynamicPropertySource
    static void registerUploadProperties(DynamicPropertyRegistry registry) {
        registry.add("file.upload-dir", () -> tempUploadDir.toString());
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NoticeRepository noticeRepository;

    @Test
    @DisplayName("NoticeAttachment.isPdf()는 PDF 일반 첨부파일만 true를 반환한다")
    void noticeAttachmentDetectsPdfCorrectly() {
        NoticeAttachment pdf = new NoticeAttachment();
        pdf.setOriginalFileName("2026년_10월_주보.PDF");
        pdf.setStoredFileName("notice/file/sample.pdf");
        pdf.setImage(false);
        assertThat(pdf.isPdf()).isTrue();

        NoticeAttachment hwp = new NoticeAttachment();
        hwp.setOriginalFileName("신청서.hwp");
        hwp.setStoredFileName("notice/file/sample.hwp");
        hwp.setImage(false);
        assertThat(hwp.isPdf()).isFalse();

        NoticeAttachment image = new NoticeAttachment();
        image.setOriginalFileName("photo.jpg");
        image.setStoredFileName("notice/img/photo.jpg");
        image.setImage(true);
        assertThat(image.isPdf()).isFalse();
    }

    @Test
    @DisplayName("/pdf/view 엔드포인트는 PDF를 inline으로 응답하고 경로 탐색 및 비PDF 요청을 차단한다")
    void viewPdfInlineServesPdfAndBlocksInvalidPaths() throws Exception {
        Path noticeFileDir = tempUploadDir.resolve("notice/file");
        Files.createDirectories(noticeFileDir);
        Path pdfPath = noticeFileDir.resolve("test-bulletin.pdf");
        byte[] samplePdfBytes = "%PDF-1.4 sample bulletin".getBytes(StandardCharsets.UTF_8);
        Files.write(pdfPath, samplePdfBytes);

        Path txtPath = noticeFileDir.resolve("note.txt");
        Files.writeString(txtPath, "plain text");

        // 정상 PDF inline 조회
        mvc.perform(get("/pdf/view")
                        .param("file", "notice/file/test-bulletin.pdf")
                        .param("originalName", "서창동성당_주보.pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline;")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(samplePdfBytes));

        // 비-PDF 파일 조회 차단 (403)
        mvc.perform(get("/pdf/view")
                        .param("file", "notice/file/note.txt"))
                .andExpect(status().isForbidden());

        // Path Traversal 차단 (403)
        mvc.perform(get("/pdf/view")
                        .param("file", "../../outside.pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("성당소식 상세 페이지(/notices/{id})는 PDF 첨부 시 웹 pdf 뷰어를 렌더링한다")
    void noticeDetailRendersInlineWebBulletinViewerWhenPdfAttached() throws Exception {
        Notice bulletin = new Notice("10월 넷째주 주보", "이번 주 주보입니다.");
        bulletin.setCategory("bulletin");

        NoticeAttachment pdfAttachment = new NoticeAttachment();
        pdfAttachment.setNotice(bulletin);
        pdfAttachment.setOriginalFileName("2026-10-25_주보.pdf");
        pdfAttachment.setStoredFileName("notice/file/2026-10-25.pdf");
        pdfAttachment.setFilePath("/uploads/notice/file/2026-10-25.pdf");
        pdfAttachment.setFileSize(204800L);
        pdfAttachment.setImage(false);
        bulletin.getAttachments().add(pdfAttachment);

        Notice savedBulletin = noticeRepository.saveAndFlush(bulletin);

        mvc.perform(get("/notices/" + savedBulletin.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"webBulletinViewerSection\"")))
                .andExpect(content().string(containsString("웹 pdf 뷰어")))
                .andExpect(content().string(containsString("/pdf/view")))
                .andExpect(content().string(containsString("2026-10-25_주보.pdf")));

        // PDF가 없는 일반 공지는 웹 pdf 뷰어 영역을 표시하지 않는다
        Notice plainNotice = new Notice("일반 공지사항", "본문 내용");
        Notice savedPlain = noticeRepository.saveAndFlush(plainNotice);

        mvc.perform(get("/notices/" + savedPlain.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"webBulletinViewerSection\""))));
    }
}
