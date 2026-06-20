package com.hoang.jwtjava.exception;

import com.hoang.jwtjava.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Login failed — {@code code} 1001 + {@code message} (401), no {@code result} field.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiResponse<Void>> handlingInvalidCredentials(InvalidCredentialsException exception) {
        ErrorCode errorCode = ErrorCode.INVALID_LOGIN;
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ApiResponse<Void>> handlingRateLimit(RateLimitExceededException exception) {
        ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(exception.getMessage())
                        .build());
    }

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(JwtException.class)
    ResponseEntity<ApiResponse<Void>> handlingJwtException(JwtException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception) {
        return buildValidationResponse(exception.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiResponse<Void>> handlingBindException(BindException exception) {
        return buildValidationResponse(exception.getBindingResult());
    }

    private ResponseEntity<ApiResponse<Void>> buildValidationResponse(BindingResult bindingResult) {
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        FieldError fieldError = bindingResult != null ? bindingResult.getFieldError() : null;
        String message = fieldErrorMessage(fieldError);
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(message)
                        .build());
    }

    private String fieldErrorMessage(FieldError fieldError) {
        if (fieldError == null)
            return ErrorCode.INVALID_KEY.getMessage();
        String field = fieldError.getField();
        String code = fieldError.getCode() == null ? "" : fieldError.getCode();
        return switch (code) {
            case "NotNull", "NotBlank", "NotEmpty" -> field + " is required";
            case "Positive" -> field + " must be greater than 0";
            case "PositiveOrZero" -> field + " must be greater than or equal to 0";
            case "DecimalMin" -> field + " is below minimum value";
            case "DecimalMax" -> field + " exceeds maximum value";
            case "Size" -> field + " length is out of allowed range";
            default -> field + " is invalid";
        };
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiResponse<Void>> handlingDataAccess(DataAccessException exception) {
        log.error("Database error", exception);
        ErrorCode errorCode = ErrorCode.DATABASE_ERROR;
        String detail = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : exception.getMessage();
        return ResponseEntity
                .status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage() + (detail != null ? ": " + detail : ""))
                        .build());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingException(Exception exception) {
        log.error("Uncategorized error", exception);
        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                        .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                        .build());
    }
}
