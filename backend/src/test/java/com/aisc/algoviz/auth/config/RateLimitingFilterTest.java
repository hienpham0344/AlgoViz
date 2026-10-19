package com.aisc.algoviz.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    @Test
    @DisplayName("Should bypass rate limiting for swagger, api-docs and h2-console")
    void testBypassStaticEndpoints() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimiterService, never()).checkLimit(anyString(), anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should parse first client IP from comma-separated X-Forwarded-For header")
    void testExtractClientIpFromForwardedHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/problems");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1, 172.16.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.checkLimit("203.0.113.195", "/api/problems"))
                .thenReturn(new RateLimiterService.RateLimitResult(true, 120, 119));

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        assertEquals("120", response.getHeader("X-RateLimit-Limit"));
        assertEquals("119", response.getHeader("X-RateLimit-Remaining"));
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should fallback to getRemoteAddr when X-Forwarded-For is missing")
    void testFallbackToRemoteAddr() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/problems");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.checkLimit("192.168.1.50", "/api/problems"))
                .thenReturn(new RateLimiterService.RateLimitResult(true, 120, 100));

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimiterService, times(1)).checkLimit("192.168.1.50", "/api/problems");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests and not invoke filterChain when limit exceeded")
    void testRejectExceededLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.checkLimit("10.0.0.5", "/api/auth/login"))
                .thenReturn(new RateLimiterService.RateLimitResult(false, 20, 0));

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertEquals("20", response.getHeader("X-RateLimit-Limit"));
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        assertTrue(response.getContentAsString().contains("\"code\":4290"));
        verify(filterChain, never()).doFilter(request, response);
    }
}
