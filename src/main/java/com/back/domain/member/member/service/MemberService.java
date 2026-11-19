package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

// 비즈니스 로직은 컨트롤러가 아니라 서비스가 맡는다.
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public Member join(String username, String password, String name) {
        // 저장하기 전에 같은 username 이 이미 있는지 확인한다.
        // (DB unique 제약만 믿으면 500 이 나므로 서비스에서 먼저 의미 있는 예외로 막는다.)
        Optional<Member> opMember = memberRepository.findByUsername(username);

        if (opMember.isPresent()) {
            throw new IllegalArgumentException(
                    "%s(은)는 이미 사용중인 username 입니다.".formatted(username)
            );
        }

        Member member = new Member(username, password, name);

        return memberRepository.save(member);
    }
}
