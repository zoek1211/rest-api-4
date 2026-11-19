package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 비즈니스 로직은 컨트롤러가 아니라 서비스가 맡는다.
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public Member join(String username, String password, String name) {
        Member member = new Member(username, password, name);

        return memberRepository.save(member);
    }
}
