package com.seochang.church.dto;

public class BoardCommentRequest {
    private String content;
    private boolean secret;

    public BoardCommentRequest() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }
}
