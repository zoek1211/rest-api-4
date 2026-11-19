package com.back.global.initData;

import com.back.domain.member.member.service.MemberService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {
    @Autowired
    @Lazy
    private BaseInitData self;
    private final MemberService memberService;
    private final PostService postService;

    @Bean
    ApplicationRunner baseInitDataApplicationRunner() {
        return args -> {
            // 회원을 글보다 먼저 만들어야 다음 강에서 글의 작성자로 쓸 수 있다.
            self.work1();
            self.work2();
        };
    }

    // 샘플 회원: id 가 저장 순서대로 1~6 이 되므로 아래 순서를 바꾸면 문서/테스트의 회원 번호가 어긋난다.
    @Transactional
    public void work1() {
        // 회원 수 기준으로 막아, 앱을 재시작해도 중복 생성되지 않게 한다.
        if (memberService.count() > 0) return;

        memberService.join("system", "12345678", "시스템");
        memberService.join("admin", "12345678", "관리자");
        memberService.join("user1", "12345678", "유저1");
        memberService.join("user2", "12345678", "유저2");
        memberService.join("user3", "12345678", "유저3");
        memberService.join("user4", "12345678", "유저4");
    }

    @Transactional
    public void work2() {
        if (postService.count() > 0) return;

        // work1 에서 만든 샘플 회원을 id 로 꺼내 작성자로 지정한다. (user1=3, user2=4, user3=5)
        Member user1 = memberService.findById(3).get();
        Member user2 = memberService.findById(4).get();
        Member user3 = memberService.findById(5).get();

        Post post1 = postService.write(user1, "제목 1", "내용 1");
        Post post2 = postService.write(user2, "제목 2", "내용 2");
        Post post3 = postService.write(user3, "제목 3", "내용 3");

        post1.addComment("댓글 1-1");
        post1.addComment("댓글 1-2");
        post1.addComment("댓글 1-3");
        post2.addComment("댓글 2-1");
        post2.addComment("댓글 2-2");
    }
}