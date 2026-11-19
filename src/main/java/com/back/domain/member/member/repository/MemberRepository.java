package com.back.domain.member.member.repository;

import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    // 쿼리 메서드: 메서드 이름만으로 SELECT ... WHERE username = ? 이 만들어진다.
    Optional<Member> findByUsername(String username);
}
