package com.seochang.church.service;

import com.seochang.church.entity.Notice;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/** Homepage cards receive plain text and local image URLs, never rendered HTML. */
@Component("noticePreview")
public class NoticePreview {
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().sanitizeUrls(true).build();

    public String imageUrl(Notice notice) {
        if (notice.getAttachments() != null) {
            for (var attachment : notice.getAttachments()) {
                if (attachment.isImage() && localImage(attachment.getFilePath())) return attachment.getFilePath();
            }
        }
        // Only same-site uploaded images become homepage thumbnails; don't load external trackers.
        for (var image : document(notice.getContent()).select("img[src]")) {
            String url = image.attr("src");
            if (localImage(url)) return url;
        }
        return null;
    }

    public String summary(String content) {
        Document document = document(content);
        document.select("img,script,style,iframe,object,embed").remove();
        String text = document.body().text().strip();
        if (text.isEmpty()) return "내용은 게시글에서 확인해주세요.";
        int count = text.codePointCount(0, text.length());
        return count > 240 ? text.substring(0, text.offsetByCodePoints(0, 240)) + "…" : text;
    }

    private Document document(String content) {
        return Jsoup.parseBodyFragment(renderer.render(parser.parse(content == null ? "" : content)));
    }

    private boolean localImage(String url) {
        return url != null && url.matches("/uploads/(?:notice|editor)/img/[A-Za-z0-9_-]+\\.(?i:jpg|jpeg|png|gif|webp)");
    }
}
