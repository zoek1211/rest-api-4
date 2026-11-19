package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.Post;

import java.time.LocalDateTime;

public record PostDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String title,
        String content,
        int authorId,
        String authorName
) {
    public PostDto(Post post) {
        this(
                post.getId(),
                post.getCreateDate(),
                post.getModifyDate(),
                post.getTitle(),
                post.getContent(),
                // RED 스텁: 작성자 필드가 아직 엔티티에 없어 컴파일만 되도록 빈 값을 둔다. (020 에서 실제 값으로 교체)
                0,
                null
        );
    }
}