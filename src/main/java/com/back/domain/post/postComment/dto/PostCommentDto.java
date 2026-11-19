package com.back.domain.post.postComment.dto;

import com.back.domain.post.postComment.entity.PostComment;

import java.time.LocalDateTime;

public record PostCommentDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String content,
        int authorId,
        String authorName
) {
    public PostCommentDto(PostComment postComment) {
        this(
                postComment.getId(),
                postComment.getCreateDate(),
                postComment.getModifyDate(),
                postComment.getContent(),
                // Member 통째가 아니라 id/name 만 노출한다. (비밀번호 등 유출 방지)
                postComment.getAuthor().getId(),
                postComment.getAuthor().getName()
        );
    }
}