package com.example.manage_activities.configuration;

import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import jakarta.servlet.ServletException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.exception.ErrorCode;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {
                ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
                response.setStatus(errorCode.getHttpStatus().value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                APIResponse<?> apiResponse = APIResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build();

                response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
                response.flushBuffer();

        }
    
}
