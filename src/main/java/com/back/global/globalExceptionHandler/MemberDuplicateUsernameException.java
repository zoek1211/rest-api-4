package com.back.global.globalExceptionHandler;

// IllegalArgumentException 은 너무 추상적이라 "무슨 일이 일어났는지" 가 이름에 드러나는 전용 예외를 둔다.
// 메시지 포맷을 예외 안에 모아 두면 던지는 쪽은 username 만 넘기면 된다.
public class MemberDuplicateUsernameException extends RuntimeException {
    public MemberDuplicateUsernameException(String username) {
        super("%s(은)는 이미 사용중인 username 입니다.".formatted(username));
    }
}
