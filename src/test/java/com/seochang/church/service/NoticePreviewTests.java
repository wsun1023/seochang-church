package com.seochang.church.service;

import com.seochang.church.entity.Notice;
import com.seochang.church.entity.NoticeAttachment;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NoticePreviewTests {
    private final NoticePreview preview = new NoticePreview();
    private final String image = "/uploads/editor/img/e6192b24-3dbe-42fd-bcc8-4bd557c17076.jpg";

    @Test void inlineImageBecomesThumbnailWithoutDisplayingMarkdownOrFileName() {
        Notice notice = new Notice("사진 소식", "![따뜻한 무대의 여성 교미 음악가.png](" + image + ")");
        assertThat(preview.imageUrl(notice)).isEqualTo(image);
        assertThat(preview.summary(notice.getContent())).isEqualTo("내용은 게시글에서 확인해주세요.");
    }

    @Test void imageAttachmentsTakePriorityAndDocumentsAreNotThumbnails() {
        Notice notice = new Notice("소식", "![본문](" + image + ")");
        NoticeAttachment pdf = new NoticeAttachment(); pdf.setFilePath("/uploads/notice/file/test.pdf");
        notice.getAttachments().add(pdf);
        assertThat(preview.imageUrl(notice)).isEqualTo(image);
        NoticeAttachment photo = new NoticeAttachment(); photo.setImage(true);
        photo.setFilePath("/uploads/notice/img/photo.jpg"); notice.getAttachments().add(photo);
        assertThat(preview.imageUrl(notice)).isEqualTo(photo.getFilePath());
    }

    @Test void summariesRemoveFormattingImagesAndLinkDestinations() {
        String content = "# 행사 안내\n\n**환영**합니다. [신청](https://example.com/private)\n\n![사진](" + image + ")\n\n- 첫 번째\n- 두 번째";
        assertThat(preview.summary(content)).isEqualTo("행사 안내 환영합니다. 신청 첫 번째 두 번째");
    }

    @Test void referencedImagesAndParenthesesInAltTextAreParsed() {
        Notice notice = new Notice("소식", "![사진 (본당)][photo]\n\n[photo]: " + image);
        assertThat(preview.imageUrl(notice)).isEqualTo(image);
        assertThat(preview.summary(notice.getContent())).doesNotContain("/uploads", "photo", "사진");
    }

    @Test void externalAndUnsafeImageDestinationsAreNotHomepageThumbnails() {
        for (String url : new String[] {"https://tracker.example/a.jpg", "//tracker.example/a.jpg", "javascript:alert(1)",
                "/uploads/editor/img/../secret.jpg", "/uploads/editor/img/image.svg"}) {
            assertThat(preview.imageUrl(new Notice("소식", "![사진](" + url + ")"))).isNull();
        }
    }

    @Test void htmlDoesNotLeakMarkupOrActiveContentAndLongSummariesAreBounded() {
        assertThat(preview.summary("<script>alert('secret')</script>\n\n<b>환영</b>"))
                .isEqualTo("환영");
        assertThat(preview.summary(null)).isEqualTo("내용은 게시글에서 확인해주세요.");
        assertThat(preview.summary("가".repeat(300))).hasSize(241).endsWith("…");
    }
}
