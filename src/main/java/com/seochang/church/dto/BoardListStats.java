package com.seochang.church.dto;

public record BoardListStats(long commentCount, long attachmentCount, boolean newPost) {
    public long getCommentCount() { return commentCount; }
    public long getAttachmentCount() { return attachmentCount; }
    public boolean isNewPost() { return newPost; }
}
