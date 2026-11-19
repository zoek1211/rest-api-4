package com.back.domain.post.postComment.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.entity.Post;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
public class PostComment extends BaseEntity {
    @ManyToOne(fetch = LAZY)
    private Post post;
    // 글과 같은 이유로 다대일 + 지연 로딩. DTO 에서 id/name 을 읽을 때 로딩된다.
    @ManyToOne(fetch = LAZY)
    private Member author;
    private String content;

    public PostComment(Post post, Member author, String content) {
        this.post = post;
        this.author = author;
        this.content = content;
    }

    public void modify(String content) {
        this.content = content;
    }
}