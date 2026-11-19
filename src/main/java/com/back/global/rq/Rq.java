package com.back.global.rq;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.globalExceptionHandler.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

// "현재 요청"에 대한 정보를 한 곳에서 꺼내 쓰기 위한 컴포넌트.
//
// 왜 @RequestScope 인가?
// - 싱글톤 빈은 요청마다 달라지는 HttpServletRequest 를 필드로 가질 수 없다.
//   요청 스코프 빈은 요청마다 새로 만들어지고 그 요청의 HttpServletRequest 를 주입받으므로,
//   컨트롤러에서 rq.getActor() 처럼 "이 요청의 액터" 를 바로 물을 수 있다.
//   (싱글톤 컨트롤러에는 프록시가 주입되어 요청마다 알맞은 인스턴스로 연결된다)
//
// 왜 인증 교체의 이음새(seam)인가? (docs/adr/0001)
// - 지금은 ?actorId 로 액터를 얻지만, 컨트롤러는 어떻게 얻는지 모르고 rq.getActor() 만 호출한다.
//   나중에 세션/JWT/Spring Security 로 바꿀 때는 이 클래스의 getActor() 만 고치면 된다.
@Component
@RequestScope
@RequiredArgsConstructor
public class Rq {
    private final HttpServletRequest req;
    private final MemberService memberService;

    // 액터 = 이 요청을 보낸 회원. 아래 경우는 모두 "인증 실패" 로 보고 401 을 던진다.
    // actorId 누락 / 빈 값 / 숫자가 아님 / 존재하지 않는 회원
    public Member getActor() {
        String actorIdStr = req.getParameter("actorId");

        if (actorIdStr == null || actorIdStr.isBlank()) {
            throw new UnauthenticatedException();
        }

        int actorId;
        try {
            actorId = Integer.parseInt(actorIdStr.trim());
        } catch (NumberFormatException e) {
            throw new UnauthenticatedException();
        }

        return memberService.findById(actorId).orElseThrow(
                UnauthenticatedException::new
        );
    }
}
