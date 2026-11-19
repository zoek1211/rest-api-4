package com.back.domain.member.member.dto;

import com.back.domain.member.member.entity.Member;

import java.time.LocalDateTime;

// 응답 전용 DTO. 엔티티 대신 "내보낼 필드만" 골라 담아 password 같은 민감 정보를 차단한다.
// 엔티티가 바뀌어도 API 응답 스펙은 이 record 로 고정된다.
public record MemberDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String username,
        String name
) {
    // 엔티티 → DTO 변환은 보조 생성자로 모아 둔다.
    public MemberDto(Member member) {
        this(
                member.getId(),
                member.getCreateDate(),
                member.getModifyDate(),
                member.getUsername(),
                member.getName()
        );
    }
}
