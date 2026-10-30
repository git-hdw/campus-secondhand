// 全局异常处理器
package com.campus.secondhand.exception;

import com.campus.secondhand.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, ArithmeticException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<?>> invalidParameter(Exception e) {
        return ResponseEntity.ok(ApiResponse.error(400, "请求参数格式不正确"));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> duplicateData(Exception e) {
        return ResponseEntity.ok(ApiResponse.error(409, "数据已存在或不符合保存要求"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<?>> missingHeader(MissingRequestHeaderException e) {
        if ("Authorization".equals(e.getHeaderName())) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        return ResponseEntity.ok(ApiResponse.error(400, "缺少必要请求头"));
    }

    @ExceptionHandler(TradeStateException.class)
    public ResponseEntity<ApiResponse<?>> tradeState(TradeStateException e) {
        return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> invalidBody(MethodArgumentNotValidException e) {
        FieldError field = e.getBindingResult().getFieldError();
        String message = field != null && field.getDefaultMessage() != null
                ? field.getDefaultMessage()
                : "请求参数不正确";
        return ResponseEntity.ok(ApiResponse.error(400, message));
    }

    @ExceptionHandler(org.springframework.web.ErrorResponseException.class)
    public ResponseEntity<ApiResponse<?>> frameworkError(org.springframework.web.ErrorResponseException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error(e.getStatusCode().value(), "请求不符合接口要求"));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> methodNotAllowed(Exception e) {
        return ResponseEntity.status(405).body(ApiResponse.error(405, "请求方法不支持"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled API exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误"));
    }
}
