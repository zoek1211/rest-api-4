package com.back.domain.post.postComment.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.domain.post.postComment.dto.PostCommentDto;
import com.back.domain.post.postComment.entity.PostComment;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
@Tag(name = "ApiV1PostCommentController", description = "API 댓글 컨트롤러")
public class ApiV1PostCommentController {
    private final PostService postService;
    private final MemberService memberService;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회")
    public List<PostCommentDto> getItems(
            @PathVariable int postId
    ) {
        Post post = postService.findById(postId).get();

        return post.getComments()
                .stream()
                .map(PostCommentDto::new)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "단건 조회")
    public PostCommentDto getItem(
            @PathVariable int postId,
            @PathVariable int id
    ) {
        Post post = postService.findById(postId).get();
        PostComment postComment = post.findCommentById(id).get();

        return new PostCommentDto(postComment);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "삭제")
    public RsData<Void> delete(
            @PathVariable int postId,
            @PathVariable int id
    ) {
        Post post = postService.findById(postId).get();
        PostComment postComment = post.findCommentById(id).get();

        postService.deleteComment(post, postComment);

        return new RsData<>(
                "200-1",
                "%d번 댓글이 삭제되었습니다.".formatted(postComment.getId())
        );
    }


    public record PostCommentModifyReqBody(
            @NotBlank
            @Size(min = 2, max = 100)
            String content
    ) {
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "수정")
    public RsData<Void> modify(
            @PathVariable int postId,
            @PathVariable int id,
            @RequestBody @Valid PostCommentModifyReqBody reqBody
    ) {
        Post post = postService.findById(postId).get();
        PostComment postComment = post.findCommentById(id).get();

        postService.modifyComment(postComment, reqBody.content);

        return new RsData<>(
                "200-1",
                "%d번 댓글이 수정되었습니다.".formatted(postComment.getId())
        );
    }


    public record PostCommentWriteReqBody(
            @NotBlank
            @Size(min = 2, max = 100)
            String content
    ) {
    }

    @PostMapping
    @Transactional
    @Operation(summary = "작성")
    public RsData<PostCommentDto> write(
            @PathVariable int postId,
            @Valid @RequestBody PostCommentWriteReqBody reqBody
    ) {
        Post post = postService.findById(postId).get();

        // TODO(75강): 임시 처리. 댓글 작성 API 도 아직 액터(actorId)를 받지 않으므로 샘플 회원 user1(id 3)을
        //  작성자로 고정해 컴파일·동작만 유지한다. 75강에서 actorId 로 찾은 회원으로 교체한다.
        Member author = memberService.findById(3).get();

        PostComment postComment = postService.writeComment(post, author, reqBody.content);

        postService.flush();

        return new RsData<>(
                "201-1",
                "%d번 댓글이 작성되었습니다.".formatted(postComment.getId()),
                new PostCommentDto(postComment)
        );
    }
}