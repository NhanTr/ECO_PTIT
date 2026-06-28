package com.example.manage_activities.exception;

import com.example.manage_activities.dto.response.APIResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<APIResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(APIResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<APIResponse<?>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(APIResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable root = ex.getMostSpecificCause();
        log.warn("Malformed request body: {}", root != null ? root.getMessage() : ex.getMessage());
        String message;
        if (root != null && root.getMessage() != null && root.getMessage().startsWith("Unexpected character ('u'")) {
            message = "Dữ liệu gửi lên không hợp lệ (undefined). Vui lòng kiểm tra lại form trước khi gửi.";
        } else if (root instanceof com.fasterxml.jackson.core.JsonParseException) {
            message = "Định dạng dữ liệu JSON không hợp lệ. Vui lòng kiểm tra lại payload.";
        } else {
            message = "Yêu cầu không hợp lệ: thiếu hoặc sai định dạng dữ liệu gửi lên.";
        }
        return ResponseEntity
                .status(400)
                .body(APIResponse.builder()
                        .code(ErrorCode.BAD_REQUEST.getCode())
                        .message(message)
                        .build());
    }

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<APIResponse<?>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(400)
                .body(APIResponse.builder()
                        .code(ErrorCode.BAD_REQUEST.getCode())
                        .message(ErrorCode.BAD_REQUEST.getMessage())
                        .build());
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<APIResponse<?>> handleException(Exception ex) {
        log.error("Exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(500)
                .body(APIResponse.builder()
                        .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                        .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                        .build());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String enumKey = ex.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.valueOf(enumKey);

        APIResponse<?> apiResponse
                = APIResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }
        
}
