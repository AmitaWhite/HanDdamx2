package com.white.handdam.global.security;

import tools.jackson.databind.ObjectMapper;
import com.white.handdam.global.exception.ErrorCode;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(
                ErrorResponse.of(errorCode, UUID.randomUUID().toString().substring(0, 8)));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
