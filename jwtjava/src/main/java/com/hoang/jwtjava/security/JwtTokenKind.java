package com.hoang.jwtjava.security;

/**
 * Phân biệt access JWT và refresh JWT (cùng signing key, khác claim).
 */
public final class JwtTokenKind {

    public static final String CLAIM_NAME = "token_kind";

    public static final String ACCESS = "ACCESS";
    public static final String REFRESH = "REFRESH";

    private JwtTokenKind() {
    }
}
