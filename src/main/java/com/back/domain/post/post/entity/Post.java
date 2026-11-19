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

    // 삭제 권한 규칙은 글(Post)이 가장 잘 안다. 컨트롤러에 if 를 두면 수정/다른 진입점마다 중복되기 쉽다.
    public void checkActorCanDelete(Member actor) {
        if (!actor.equals(author)) throw new AccessDeniedException();
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