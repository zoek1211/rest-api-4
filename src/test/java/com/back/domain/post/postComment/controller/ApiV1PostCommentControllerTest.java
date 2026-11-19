package com.back.domain.post.postComment.controller;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import com.back.domain.post.postComment.entity.PostComment;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1PostCommentControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private PostService postService;

    @Test
    @DisplayName("댓글 단건조회")
    void t1() throws Exception {
        int postId = 1;
        int id = 1;

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/posts/%d/comments/%d".formatted(postId, id))
                )
                .andDo(print());

        Post post = postService.findById(postId).get();
        PostComment postComment = post.findCommentById(id).get();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postComment.getId()))
                .andExpect(jsonPath("$.createDate").value(Matchers.startsWith(postComment.getCreateDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.modifyDate").value(Matchers.startsWith(postComment.getModifyDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.content").value(postComment.getContent()));
    }

    @Test
    @DisplayName("댓글 다건조회")
    void t2() throws Exception {
        int postId = 1;

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/posts/%d/comments".formatted(postId))
                )
                .andDo(print());

        Post post = postService.findById(postId).get();
        List<PostComment> comments = post.getComments();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(comments.size()));

        for (int i = 0; i < comments.size(); i++) {
            PostComment postComment = comments.get(i);

            resultActions
                    .andExpect(jsonPath("$[%d].id".formatted(i)).value(postComment.getId()))
                    .andExpect(jsonPath("$[%d].createDate".formatted(i)).value(Matchers.startsWith(postComment.getCreateDate().toString().substring(0, 20))))
                    .andExpect(jsonPath("$[%d].modifyDate".formatted(i)).value(Matchers.startsWith(postComment.getModifyDate().toString().substring(0, 20))))
                    .andExpect(jsonPath("$[%d].content".formatted(i)).value(postComment.getContent()));
        }
    }

    @Test
    @DisplayName("댓글 삭제")
    void t3() throws Exception {
        int postId = 1;
        int id = 1;

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/posts/%d/comments/%d?actorId=3".formatted(postId, id)) // 댓글 1번의 작성자(user1, id 3)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 삭제되었습니다.".formatted(id)));
    }

    @Test
    @DisplayName("댓글 수정")
    void t4() throws Exception {
        int postId = 1;
        int id = 1;

        ResultActions resultActions = mvc
                .perform(
                        put("/api/v1/posts/%d/comments/%d".formatted(postId, id))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "내용 new"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 수정되었습니다.".formatted(id)));
    }

    @Test
    @DisplayName("댓글 작성")
    void t5() throws Exception {
        int postId = 1;

        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/posts/%d/comments?actorId=3".formatted(postId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "내용"
                                        }
                                        """)
                )
                .andDo(print());

        Post post = postService.findById(postId).get();
        PostComment postComment = post.getComments().getLast();

        resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 댓글이 작성되었습니다.".formatted(postComment.getId())))
                .andExpect(jsonPath("$.data.id").value(postComment.getId()))
                .andExpect(jsonPath("$.data.createDate").value(Matchers.startsWith(postComment.getCreateDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.data.modifyDate").value(Matchers.startsWith(postComment.getModifyDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.data.content").value("내용"));
    }

    @Test
    @DisplayName("댓글 단건조회, 작성자 정보 포함")
    void t10() throws Exception {
        // 샘플 데이터 전제(BaseInitData): 댓글1=user1(3), 2=user2(4), 3=user3(5) 는 글1, 댓글4=user1(3), 5=user2(4) 는 글2.
        // 기대값은 엔티티가 아니라 고정값으로 쓴다. (postId, 댓글 id, authorId, authorName)
        Object[][] expected = {
                {1, 1, 3, "유저1"},
                {1, 2, 4, "유저2"},
                {1, 3, 5, "유저3"},
                {2, 4, 3, "유저1"},
                {2, 5, 4, "유저2"},
        };

        for (Object[] e : expected) {
            mvc.perform(get("/api/v1/posts/%d/comments/%d".formatted(e[0], e[1])))
                    .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                    .andExpect(handler().methodName("getItem"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorId").value(e[2]))
                    .andExpect(jsonPath("$.authorName").value(e[3]));
        }
    }

    @Test
    @DisplayName("댓글 다건조회, 작성자 정보 포함")
    void t11() throws Exception {
        mvc.perform(get("/api/v1/posts/1/comments"))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorId").value(3))
                .andExpect(jsonPath("$[0].authorName").value("유저1"))
                .andExpect(jsonPath("$[1].authorId").value(4))
                .andExpect(jsonPath("$[1].authorName").value("유저2"))
                .andExpect(jsonPath("$[2].authorId").value(5))
                .andExpect(jsonPath("$[2].authorName").value("유저3"));
    }

    @Test
    @DisplayName("댓글 작성, actorId 회원이 작성자가 된다")
    void t12() throws Exception {
        // actorId=4 → 유저2. (임시 고정값 user1(3) 과 달라야 "actorId 를 실제로 쓴다" 는 것이 검증된다)
        mvc.perform(
                        post("/api/v1/posts/1/comments?actorId=4")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "내용"
                                        }
                                        """)
                )
                .andExpect(handler().handlerType(ApiV1PostCommentController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authorId").value(4))
                .andExpect(jsonPath("$.data.authorName").value("유저2"));
    }

    @Test
    @DisplayName("댓글 작성, actorId 누락 시 401")
    void t13() throws Exception {
        mvc.perform(
                        post("/api/v1/posts/1/comments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "내용"
                                        }
                                        """)
                )
                .andExpect(handler().methodName("write"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."));
    }

    @Test
    @DisplayName("댓글 작성, 존재하지 않는 actorId 면 401")
    void t14() throws Exception {
        mvc.perform(
                        post("/api/v1/posts/1/comments?actorId=999999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "content": "내용"
                                        }
                                        """)
                )
                .andExpect(handler().methodName("write"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."));
    }

    @Test
    @DisplayName("댓글 삭제, 작성자가 아닌 회원이면 403")
    void t15() throws Exception {
        // 댓글 1번은 user1(id 3)이 썼다. user2(id 4)는 작성자가 아니다.
        mvc.perform(delete("/api/v1/posts/1/comments/1?actorId=4"))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."));
    }

    @Test
    @DisplayName("댓글 삭제, 글 작성자라도 댓글 작성자가 아니면 403")
    void t16() throws Exception {
        // 댓글 2번은 user2(id 4)가 썼고, 글 1번의 작성자는 user1(id 3)이다. 글 작성자여도 남의 댓글은 못 지운다.
        mvc.perform(delete("/api/v1/posts/1/comments/2?actorId=3"))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("댓글 삭제, actorId 가 없으면 401")
    void t17() throws Exception {
        mvc.perform(delete("/api/v1/posts/1/comments/1"))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."));
    }
}
