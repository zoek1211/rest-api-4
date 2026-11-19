package com.back.domain.post.postComment.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.entity.Post;
import com.back.global.globalExceptionHandler.AccessDeniedException;
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

    // 댓글 삭제 권한은 댓글(PostComment)이 안다. 글 작성자라도 남의 댓글은 지울 수 없다.
    public void checkActorCanDelete(Member actor) {
        checkActorIsAuthor(actor);
    }

    // Post 와 같은 이유로 엔티티 equals 대신 id 로 비교한다(author 가 LAZY 프록시일 수 있음).
    private void checkActorIsAuthor(Member actor) {
        if (actor.getId() != author.getId()) throw new AccessDeniedException();
    }
}
