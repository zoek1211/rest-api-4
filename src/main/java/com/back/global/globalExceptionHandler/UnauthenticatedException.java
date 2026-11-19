package com.back.global.globalExceptionHandler;

// "누구인지 확인되지 않음" 을 뜻하는 전용 예외. (인증 실패 = 401, 권한 부족 = 403 과 구분: GLOSSARY.md)
// NoSuchElementException 은 404 로 처리되므로, 회원 조회 실패를 401 로 보내려면 별도 타입이 필요하다.
public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("로그인 후 이용해주세요.");
    }
}
