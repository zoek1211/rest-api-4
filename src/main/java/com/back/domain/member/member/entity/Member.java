package com.back.domain.member.member.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원(Member): 가입한 사용자. 액터/작성자는 "역할"일 뿐 별도 엔티티가 아니다. (GLOSSARY 참고)
@Entity
@Getter
// JPA 는 기본 생성자가 필요하다.
@NoArgsConstructor
public class Member extends BaseEntity {
    // username 은 로그인 아이디 역할이라 DB 레벨에서도 유일해야 한다.
    @Column(unique = true)
    private String username;
    // 학습용 프로젝트이므로 비밀번호는 평문 저장한다. (ADR 0001 참고)
    private String password;
    private String name;

    // 가입에 필요한 값만 받는 생성자. id/생성시각은 BaseEntity 가 관리한다.
    public Member(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }
}
