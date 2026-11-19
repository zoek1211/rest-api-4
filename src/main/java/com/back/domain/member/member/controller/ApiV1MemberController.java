package com.back.domain.member.member.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 기존 Post/PostComment 컨트롤러와 같은 패턴: /api/v1/<복수형 리소스>
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "ApiV1MemberController", description = "API 회원 컨트롤러")
public class ApiV1MemberController {
    private final MemberService memberService;

    // 요청 본문 전용 record. 검증 규칙을 DTO 에 선언해 두면 @Valid 위반 시
    // MethodArgumentNotValidException → 400 으로 처리된다.
    public record MemberJoinReqBody(
            @NotBlank
            @Size(min = 4, max = 30)
            String username,
            @NotBlank
            @Size(min = 8, max = 30)
            String password,
            @NotBlank
            @Size(min = 2, max = 30)
            String name
    ) {
    }

    @PostMapping("/join")
    @Transactional
    @Operation(summary = "회원가입")
    public RsData<Void> join(
            @RequestBody @Valid MemberJoinReqBody reqBody
    ) {
        Member member = memberService.join(reqBody.username, reqBody.password, reqBody.name);

        // resultCode 앞자리(201)가 곧 HTTP 상태코드가 된다. (RsData + ResponseAspect)
        return new RsData<>(
                "201-1",
                "%s님 환영합니다. 회원가입이 완료되었습니다.".formatted(member.getName())
        );
    }
}
