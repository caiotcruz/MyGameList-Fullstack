package com.caiotcruz.mygamelist.infra.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.CorsFilter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsAdmissionTest {
    private CorsFilter cors() {
        var config = new SecurityConfigurations(mock(SecurityFilter.class),
            mock(AuthRateLimitFilter.class), mock(AccountRateLimitFilter.class));
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:4200");
        return new CorsFilter(config.corsConfigurationSource());
    }
    private ApiAdmissionFilter admission(RateLimits limits) {
        return new ApiAdmissionFilter(limits, mock(SecurityEvents.class), new ClientAddress(""), 1, 120);
    }
    private MockHttpServletRequest preflight(String origin, String headers) {
        var request = new MockHttpServletRequest("OPTIONS", "/auth/login");
        request.setLocalPort(8080);
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", headers);
        return request;
    }
    @Test void loginPreflightIncludesRequestedHeadersAndStopsBeforeController() throws Exception {
        var request = preflight("http://localhost:4200", "content-type,authorization");
        var response = new MockHttpServletResponse();
        admission(mock(RateLimits.class)).doFilter(request, response, (req, res) ->
            cors().doFilter(req, res, (r, s) -> fail("Preflight must not reach controller")));
        assertEquals(200, response.getStatus());
        assertEquals("http://localhost:4200", response.getHeader("Access-Control-Allow-Origin"));
        var headers = response.getHeader("Access-Control-Allow-Headers");
        assertNotNull(headers);
        assertTrue(headers.toLowerCase().contains("content-type"));
        assertTrue(headers.toLowerCase().contains("authorization"));
        assertTrue(response.getHeader("Access-Control-Allow-Methods").contains("POST"));
    }
    @Test void rejectsUntrustedOrigin() throws Exception {
        var response = new MockHttpServletResponse();
        admission(mock(RateLimits.class)).doFilter(preflight("https://untrusted.invalid", "content-type"), response,
            (req, res) -> cors().doFilter(req, res, (r, s) -> fail()));
        assertEquals(403, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }
    @Test void rejectsUnapprovedHeader() throws Exception {
        var response = new MockHttpServletResponse();
        admission(mock(RateLimits.class)).doFilter(preflight("http://localhost:4200", "x-unapproved"), response,
            (req, res) -> cors().doFilter(req, res, (r, s) -> fail()));
        assertEquals(403, response.getStatus());
    }
    @Test void actualJsonRequestReachesControllerWithCorsHeaders() throws Exception {
        var request = new MockHttpServletRequest("POST", "/auth/login");
        request.setLocalPort(8080);
        request.addHeader("Origin", "http://localhost:4200");
        request.setContentType("application/json");
        request.setContent("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        var reached = new java.util.concurrent.atomic.AtomicBoolean();
        admission(mock(RateLimits.class)).doFilter(request, response, (req, res) ->
            cors().doFilter(req, res, (r, s) -> reached.set(true)));
        assertTrue(reached.get());
        assertEquals("http://localhost:4200", response.getHeader("Access-Control-Allow-Origin"));
    }
    @Test void earlyRateLimitResponseRemainsReadableOnlyForAllowedOrigin() throws Exception {
        for (String origin : new String[]{"http://localhost:4200", "https://untrusted.invalid"}) {
            var limits = mock(RateLimits.class);
            doThrow(new SecurityRejection(429, "RATE_LIMITED", "Tente novamente.", 10))
                .when(limits).check("global", "all", 120, 1);
            var request = new MockHttpServletRequest("GET", "/users/1");
            request.setLocalPort(8080);
            request.addHeader("Origin", origin);
            var response = new MockHttpServletResponse();
            admission(limits).doFilter(request, response, (req, res) -> fail());
            assertEquals(429, response.getStatus());
            assertEquals(origin.equals("http://localhost:4200") ? origin : null,
                response.getHeader("Access-Control-Allow-Origin"));
        }
    }
}
