package com.example.aikb.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.debug.AiDebugAccessGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DebugApiAccessFilterTest {

    @Test
    void shouldRejectProtectedDebugPathBeforeControllerWhenDisabled() throws ServletException, IOException {
        AiDebugAccessGuard guard = mock(AiDebugAccessGuard.class);
        doThrow(new BusinessException(40301, "debug 接口未开启"))
                .when(guard).ensureAccessible();
        DebugApiAccessFilter filter = new DebugApiAccessFilter(guard, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/debug/ai/embedding");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":40301"));
        verify(guard).ensureAccessible();
    }

    @Test
    void shouldProtectApiDebugTestAndInternalPathPrefixes() throws ServletException, IOException {
        AiDebugAccessGuard guard = mock(AiDebugAccessGuard.class);
        DebugApiAccessFilter filter = new DebugApiAccessFilter(guard, new ObjectMapper());

        filter.doFilter(new MockHttpServletRequest("GET", "/api/debug/ping"),
                new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilter(new MockHttpServletRequest("GET", "/test/ping"),
                new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilter(new MockHttpServletRequest("GET", "/api/internal/ping"),
                new MockHttpServletResponse(), new MockFilterChain());

        verify(guard, org.mockito.Mockito.times(3)).ensureAccessible();
    }

    @Test
    void shouldSkipNonDebugPath() throws ServletException, IOException {
        AiDebugAccessGuard guard = mock(AiDebugAccessGuard.class);
        DebugApiAccessFilter filter = new DebugApiAccessFilter(guard, new ObjectMapper());

        filter.doFilter(new MockHttpServletRequest("GET", "/api/chat/conversations"),
                new MockHttpServletResponse(), new MockFilterChain());

        verify(guard, never()).ensureAccessible();
    }
}
