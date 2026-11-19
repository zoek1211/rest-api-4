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
                // RED 스텁: 댓글 엔티티에 작성자가 아직 없어 컴파일만 되도록 빈 값을 둔다. (022 에서 실제 값으로 교체)
                0,
                null
        );
    }
}