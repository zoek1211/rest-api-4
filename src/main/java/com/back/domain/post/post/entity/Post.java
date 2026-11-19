package com.back.domain.post.post.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.post.postComment.entity.PostComment;
import com.back.global.globalExceptionHandler.AccessDeniedException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor
public class Post extends BaseEntity {
    // 다대일 + 지연 로딩: 글 목록만 읽을 때 회원 테이블까지 불필요하게 조회하지 않는다.
    // DTO 에서 getAuthor().getId()/getName() 을 쓰는 시점에 (트랜잭션 안에서) 로딩된다.
    @ManyToOne(fetch = LAZY)
    private Member author;
    private String title;
    private String content;

    @OneToMany(mappedBy = "post", cascade = {PERSIST, REMOVE}, orphanRemoval = true)
    private final List<PostComment> comments = new ArrayList<>();

    public Post(Member author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;
    }

    public void modify(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // 삭제/수정 권한 규칙은 글(Post)이 가장 잘 안다. 컨트롤러에 if 를 두면 진입점마다 중복되기 쉽다.
    public void checkActorCanDelete(Member actor) {
        checkActorIsAuthor(actor);
    }

    public void checkActorCanModify(Member actor) {
        checkActorIsAuthor(actor);
    }

    // [학습] 엔티티 equals 대신 id 로 비교한다.
    // author 는 LAZY 라서 Hibernate 프록시일 수 있고, BaseEntity.equals 는 getClass() 를 비교하므로
    // 프록시(Member$HibernateProxy)와 실제 Member 가 서로 다른 클래스라 같은 회원인데도 false 가 될 수 있다.
    // id 는 프록시에서도 DB 조회 없이 읽을 수 있어 안전하다.
    private void checkActorIsAuthor(Member actor) {
        if (actor.getId() != author.getId()) throw new AccessDeniedException();
    }

    public PostComment addComment(Member author, String content) {
        PostComment postComment = new PostComment(this, author, content);
        comments.add(postComment);

        return postComment;
    }

    public Optional<PostComment> findCommentById(int id) {
        return comments
                .stream()
                .filter(comment -> comment.getId() == id)
                .findFirst();
    }

    public boolean deleteComment(PostComment postComment) {
        if (postComment == null) return false;

        return comments.remove(postComment);
    }
}