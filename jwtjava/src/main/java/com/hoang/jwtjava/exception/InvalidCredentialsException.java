package com.hoang.jwtjava.exception;

/**
 * Thrown when login fails — same message for unknown user or wrong password (security).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_LOGIN.getMessage());
    }
}
