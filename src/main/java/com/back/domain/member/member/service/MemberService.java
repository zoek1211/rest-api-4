package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.globalExceptionHandler.MemberDuplicateUsernameException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

// 비즈니스 로직은 컨트롤러가 아니라 서비스가 맡는다.
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public Member join(String username, String password, String name) {
        // Optional 을 isPresent() 로 꺼내 검사하는 대신 ifPresent 로 "있으면 예외" 를 한 흐름으로 표현한다.
        // 람다 파라미터는 쓰지 않으므로 _ (unnamed variable, Java 22+) 로 둔다.
        memberRepository.findByUsername(username)
                .ifPresent(_ -> {
                    throw new MemberDuplicateUsernameException(username);
                });

        Member member = new Member(username, password, name);

        return memberRepository.save(member);
    }

    public Optional<Member> findLatest() {
        return memberRepository.findTopByOrderByIdDesc();
    }

    // 조회 결과가 없을 수도 있으므로 Optional 그대로 돌려주고, 없을 때의 처리는 호출하는 쪽이 정한다.
    public Optional<Member> findById(int id) {
        return memberRepository.findById(id);
    }
}
