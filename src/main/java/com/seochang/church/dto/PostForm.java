package com.seochang.church.dto;

import com.seochang.church.entity.*;

/** Only user-editable fields can enter a post through an HTML form. */
public class PostForm {
    private String title;
    private String content;
    private String category;
    private boolean pinned;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isPinned() { return pinned; }
    public boolean getPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public void validate() {
        if (title == null || title.isBlank() || title.length() > 255 || content == null || content.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "제목(255자 이하)과 내용을 입력해주세요.");
        }
        if (category != null && category.length() > 30) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "잘못된 분류입니다.");
    }
    public Board toBoard() {
        validate();
        Board board = new Board();
        board.setTitle(title);
        board.setContent(content);
        if (category != null) board.setCategory(category);
        return board;
    }
    public Notice toNotice() {
        validate();
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        if (category != null) notice.setCategory(category);
        notice.setPinned(pinned);
        return notice;
    }
    public Gallery toGallery() {
        validate();
        Gallery gallery = new Gallery();
        gallery.setTitle(title);
        gallery.setContent(content);
        return gallery;
    }
}

