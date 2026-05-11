package com.example.aikb.security;

import com.example.aikb.common.Result;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.debug.AiDebugAccessGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Path-level guard for debug/test/internal HTTP endpoints.
 */
@Component
@RequiredArgsConstructor
public class DebugApiAccessFilter extends OncePerRequestFilter {

    private static final List<String> PROTECTED_PATTERNS = List.of(
            "/debug",
            "/debug/**",
            "/api/debug",
            "/api/debug/**",
            "/test",
            "/test/**",
            "/api/test",
            "/api/test/**",
            "/internal",
            "/internal/**",
            "/api/internal",
            "/api/internal/**");

    private final AiDebugAccessGuard debugAccessGuard;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!isProtectedDebugPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            debugAccessGuard.ensureAccessible();
        } catch (BusinessException ex) {
            writeJsonError(response, ex.getHttpStatus(), ex.getCode(), ex.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedDebugPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        String servletPath = path.isBlank() ? "/" : path;
        return PROTECTED_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, servletPath));
    }

    private void writeJsonError(HttpServletResponse response, int httpStatus, int code, String message)
            throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.fail(code, message));
    }
}
