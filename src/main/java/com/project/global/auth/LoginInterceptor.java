package com.project.global.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        try {
            final String token = AuthorizationExtractor.extract(request);
            jwtTokenUtil.verify(token);
            return true;

        } catch (IllegalArgumentException e) {

            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType("application/json;charset=UTF-8");

            String msg = e.getMessage();

            response.getWriter().write("{\"message\":\"" + msg + "\"}");

            return false;
        }
    }
}
