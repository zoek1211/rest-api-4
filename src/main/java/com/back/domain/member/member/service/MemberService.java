package com.back.domain.member.member.service;

import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 비즈니스 로직은 컨트롤러가 아니라 서비스가 맡는다. (아직은 뼈대만)
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
}
