package com.tramchester.unit;


import com.tramchester.RedirectToAppFilter;
import com.tramchester.RedirectToHttpsUsingELBProtoHeader;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

class RedirectToAppFilterTest extends EasyMockSupport {

    private final RedirectToAppFilter filter = new RedirectToAppFilter();

    @Test
    void shouldFilterIfMatchesRoot() throws IOException {
        shouldCheckRedirected("https://www.tramchester.com/", "https://www.tramchester.com/app");
    }

    @Test
    void shouldNotFilterIfELBRequest() {
        shouldCheckNoRedirectAndSendsOk();
    }

    @Test
    void shouldFilterIfMatchesRootUpper() throws IOException {
        shouldCheckRedirected("HTTPS://www.tramchester.COM/", "https://www.tramchester.com/app");
    }

    @Test
    void shouldFilterIfRootOnly() throws IOException {
        shouldCheckRedirected("https://blue.tramchester.com/", "https://blue.tramchester.com/app");
    }

    @Test
    void shouldFilterIfRootOnlyLocalHost() throws IOException {
        shouldCheckRedirected("http://localhost:8080/", "http://localhost:8080/app");
    }

    @Test
    void shouldNotFilterIfNotRoot() throws IOException, ServletException {
        shouldCheckNoRedirect("https://www.tramchester.com/app");
    }

    @Test
    void shouldNotFilterIfAPI() throws IOException, ServletException {
        shouldCheckNoRedirect("https://www.tramchester.com/api");
    }

    @Test
    void shouldNotFilterIfAPIOtherRoos() throws IOException, ServletException {
        shouldCheckNoRedirect("https://blue.tramchester.com/app");
    }

    private void shouldCheckRedirected(String original, String expected) throws IOException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("User-Agent")).andReturn("SomeUserAgent");
        EasyMock.expect(request.getHeader(RedirectToHttpsUsingELBProtoHeader.X_FORWARDED_PROTO)).andReturn("http");
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(original));
        response.sendRedirect(expected);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> filter.doFilter(request, response, chain));
        verifyAll();
    }

    private void shouldCheckNoRedirect(String urlToCheck) throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("User-Agent")).andReturn("userAgent");
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(urlToCheck));
        chain.doFilter(request,response);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> filter.doFilter(request, response, chain));
        verifyAll();
    }

    private void shouldCheckNoRedirectAndSendsOk() {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("User-Agent")).andReturn("ELB-HealthChecker/2.0");
        response.setStatus(200);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> filter.doFilter(request, response, chain));
        verifyAll();
    }
}
