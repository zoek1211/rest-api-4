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
                // 작성자는 id 와 name 만 노출한다. Member 통째로 내보내면 password 등이 응답에 섞인다.
                post.getAuthor().getId(),
                post.getAuthor().getName()
        );
    }
}