package com.hoang.jwtjava.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR(9998, "Database error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_LOGIN(1001, "Invalid email or password", HttpStatus.UNAUTHORIZED),
    INVALID_KEY(1010, "Invalid message key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "Email already exists", HttpStatus.CONFLICT),
    EMAIL_INVALID(1003, "Invalid email", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1005, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated: invalid credentials or token", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission to perform this action", HttpStatus.FORBIDDEN),
    PRODUCT_NOT_FOUND(1008, "Product not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(1009, "Category not found", HttpStatus.NOT_FOUND),
    IMAGE_IMPORT_FAILED(1011, "Could not download or save product image", HttpStatus.BAD_REQUEST),
    ROLE_INVALID(1012, "Invalid role", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1013, "Order not found", HttpStatus.NOT_FOUND),
    ORDER_ITEM_INVALID(1014, "Order items are invalid", HttpStatus.BAD_REQUEST),
    OUT_OF_STOCK(1015, "Product is out of stock", HttpStatus.BAD_REQUEST),
    PAYMENT_INVALID(1016, "Payment request is invalid", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_EXISTS(1019, "Order has already been paid or has a pending payment", HttpStatus.CONFLICT),
    PAYMENT_GATEWAY_ERROR(1020, "Payment gateway error", HttpStatus.BAD_GATEWAY),
    PAYMENT_SIGNATURE_INVALID(1021, "Payment signature is invalid", HttpStatus.BAD_REQUEST),
    TOO_MANY_REQUESTS(1017, "Too many requests", HttpStatus.TOO_MANY_REQUESTS),
    INVALID_RESET_TOKEN(1018, "Invalid or expired password reset link", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode httpStatusCode;
}
