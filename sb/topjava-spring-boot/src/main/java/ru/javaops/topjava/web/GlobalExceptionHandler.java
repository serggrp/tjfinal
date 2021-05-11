package ru.javaops.topjava.web;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.javaops.topjava.error.AppException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    public static final String EXCEPTION_DUPLICATE_EMAIL = "User with this email already exists";

    private final ErrorAttributes errorAttributes;

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status, WebRequest request) {
        return handleBindingErrors(ex.getBindingResult(), request);
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return handleBindingErrors(ex.getBindingResult(), request);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> appException(WebRequest request, AppException ex) {
        log.error("Application Exception", ex);
        return createResponseEntity(getDefaultBody(request, ex.getOptions(), null), ex.getStatus());
    }

    private ResponseEntity<Object> handleBindingErrors(BindingResult result, WebRequest request) {
        String msg = result.getFieldErrors().stream()
                .map(fe -> String.format("[%s] %s", fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.joining("\n"));
        return createResponseEntity(getDefaultBody(request, ErrorAttributeOptions.defaults(), msg), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private Map<String, Object> getDefaultBody(WebRequest request, ErrorAttributeOptions options, String msg) {
        Map<String, Object> body = errorAttributes.getErrorAttributes(request, options);
        if (msg != null) {
            body.put("message", msg);
        }
        return body;
    }

    private <T> ResponseEntity<T> createResponseEntity(Map<String, Object> body, HttpStatus status) {
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        return (ResponseEntity<T>) ResponseEntity.status(status).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("Exception", ex);
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }
}
