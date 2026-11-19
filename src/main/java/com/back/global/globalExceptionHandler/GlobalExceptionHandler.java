package com.back.global.globalExceptionHandler;

import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handle(NoSuchElementException ex) {
        return new ResponseEntity<>(
                new RsData<>(
                        "404-1",
                        "해당 데이터가 존재하지 않습니다."
                ),
                NOT_FOUND
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handle(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> (FieldError) error)
                .map(error -> error.getField() + "-" + error.getCode() + "-" + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("\n"));

        return new ResponseEntity<>(
                new RsData<>(
                        "400-1",
                        msg
                ),
                BAD_REQUEST
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<Void>> handle(HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(
                new RsData<>(
                        "400-1",
                        "요청 본문이 올바르지 않습니다."
                ),
                BAD_REQUEST
        );
    }

    // 중복 username 은 요청과 서버 상태가 충돌한 것이므로 409 Conflict.
    // 전용 예외를 두면 다른 곳의 IllegalArgumentException 이 409 로 오인되지 않는다.
    @ExceptionHandler(MemberDuplicateUsernameException.class)
    public ResponseEntity<RsData<Void>> handle(MemberDuplicateUsernameException ex) {
        return new ResponseEntity<>(
                new RsData<>(
                        "409-1",
                        ex.getMessage()
                ),
                CONFLICT
        );
    }

    // 로그인(회원 확인)이 안 된 요청은 401 Unauthorized. 권한 부족(403)과는 다르다.
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<RsData<Void>> handle(UnauthenticatedException ex) {
        return new ResponseEntity<>(
                new RsData<>(
                        "401-1",
                        ex.getMessage()
                ),
                UNAUTHORIZED
        );
    }

    // 로그인은 됐지만 권한이 없는 요청은 403 Forbidden. 인증 실패(401)와 다르다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RsData<Void>> handle(AccessDeniedException ex) {
        return new ResponseEntity<>(
                new RsData<>(
                        "403-1",
                        ex.getMessage()
                ),
                FORBIDDEN
        );
    }
}
