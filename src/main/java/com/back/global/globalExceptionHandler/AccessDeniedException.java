package com.back.global.globalExceptionHandler;

// "누구인지는 알지만 이 일을 할 권한이 없음" 을 뜻하는 전용 예외. (권한 부족 = 403, 인증 실패 = 401 과 구분: GLOSSARY.md)
// 주의: Spring Security 의 org.springframework.security.access.AccessDeniedException 과는 이름만 같을 뿐 무관한 우리 프로젝트의 클래스다.
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException() {
        super("권한이 없습니다.");
    }
}
