package com.back.domain.member.member.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 컨트롤러 테스트: 실제 HTTP 요청 → 컨트롤러 → 서비스 → H2 까지 한 번에 검증한다.
// @Transactional 덕분에 각 테스트가 끝나면 DB 변경이 롤백되어 테스트끼리 영향을 주지 않는다.
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1MemberControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private MemberService memberService;


    @Test
    @DisplayName("회원가입")
    void t1() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/members/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "user10",
                                            "password": "12345678",
                                            "name": "유저10"
                                        }
                                        """)
                )
                .andDo(print());

        // 응답에는 회원 정보가 없으므로, 가장 최근에 저장된 회원을 조회해 메시지를 비교한다.
        Member member = memberService.findLatest().get();

        resultActions
                .andExpect(handler().handlerType(ApiV1MemberController.class))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%s님 환영합니다. 회원가입이 완료되었습니다.".formatted(member.getName())));
    }

    @Test
    @DisplayName("회원가입 with duplicate username")
    void t2() throws Exception {
        // 먼저 한 번 가입시켜 두고(결과는 확인하지 않는다),
        mvc
                .perform(
                        post("/api/v1/members/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "user10",
                                            "password": "12345678",
                                            "name": "유저10"
                                        }
                                        """)
                )
                .andDo(print());

        // 같은 username 으로 다시 가입하면 409 가 나와야 한다.
        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/members/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "user10",
                                            "password": "12345678",
                                            "name": "유저10"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1MemberController.class))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"))
                .andExpect(jsonPath("$.msg").value("user10(은)는 이미 사용중인 username 입니다."));
    }

    @Test
    @DisplayName("내 정보")
    void t3() throws Exception {
        // 준비: 회원가입 (결과는 검증하지 않는다)
        mvc
                .perform(
                        post("/api/v1/members/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "user10",
                                            "password": "12345678",
                                            "name": "유저10"
                                        }
                                        """)
                )
                .andDo(print());

        Member member = memberService.findLatest().get();

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/members/me?actorId=%d".formatted(member.getId()))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1MemberController.class))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId()))
                // 날짜는 나노초 자릿수 표현이 달라질 수 있어 앞 20자(초 단위까지)만 비교한다.
                .andExpect(jsonPath("$.createDate").value(Matchers.startsWith(member.getCreateDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.modifyDate").value(Matchers.startsWith(member.getModifyDate().toString().substring(0, 20))))
                .andExpect(jsonPath("$.username").value(member.getUsername()))
                .andExpect(jsonPath("$.name").value(member.getName()))
                // 핵심: DTO 덕분에 password 는 응답에 아예 없어야 한다.
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("내 정보 - with wrong actorId")
    void t4() throws Exception {
        // Integer.MAX_VALUE 는 존재하지 않는 회원번호로 쓰기에 충분하다.
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/members/me?actorId=%d".formatted(Integer.MAX_VALUE))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1MemberController.class))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."));
    }

    @Test
    @DisplayName("샘플 회원 6명 - 회원 번호 1~6 순서")
    void t5() throws Exception {
        // BaseInitData 가 테스트 프로필에서도 돌기 때문에, 가입 없이 바로 actorId 로 조회할 수 있어야 한다.
        // 문서와 다음 강의 테스트가 이 번호를 믿고 쓰므로 순서(id)까지 고정해서 검증한다.
        String[][] samples = {
                {"system", "시스템"},
                {"admin", "관리자"},
                {"user1", "유저1"},
                {"user2", "유저2"},
                {"user3", "유저3"},
                {"user4", "유저4"},
        };

        for (int i = 0; i < samples.length; i++) {
            int id = i + 1;

            mvc
                    .perform(get("/api/v1/members/me?actorId=%d".formatted(id)))
                    .andDo(print())
                    .andExpect(handler().handlerType(ApiV1MemberController.class))
                    .andExpect(handler().methodName("me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.username").value(samples[i][0]))
                    .andExpect(jsonPath("$.name").value(samples[i][1]))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }
    }
}
