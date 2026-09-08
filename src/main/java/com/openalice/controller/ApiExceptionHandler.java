package com.openalice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openalice.dto.ChatStreamEvent;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常翻译：把非法入参等错误统一转成客户端能理解的结构。
 *
 * <p>对 SSE 请求返回 {@code event:error} 流，对普通 JSON 请求返回
 * {@code 400 + {"error": ...}}。更完整的错误码体系后续再补。</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private final ObjectMapper objectMapper;

    public ApiExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleInvalidArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        if (acceptsEventStream(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .body(formatSseError(exception));
        }
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", exception.getMessage()));
    }

    private String formatSseError(IllegalArgumentException exception) {
        try {
            String json = objectMapper.writeValueAsString(ChatStreamEvent.error(exception.getMessage()));
            return "event:error\ndata:" + json + "\n\n";
        } catch (JsonProcessingException serializationError) {
            return "event:error\ndata:{\"type\":\"error\",\"error\":\"invalid request\"}\n\n";
        }
    }

    private static boolean acceptsEventStream(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
