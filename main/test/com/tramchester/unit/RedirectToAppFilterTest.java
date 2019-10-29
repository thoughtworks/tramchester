package com.tramchester.unit;


import com.tramchester.RedirectToAppFilter;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RedirectToAppFilterTest extends EasyMockSupport {

    private RedirectToAppFilter filter = new RedirectToAppFilter();

    @Test
    public void shouldFilterIfMatchesRoot() throws IOException, ServletException {
        shouldCheckRedirected("https://www.tramchester.com/", "https://www.tramchester.com/app", "SomeUserAgent");
    }

    @Test
    public void shouldNotFilterIfELBRequest() throws IOException, ServletException {
        shouldCheckNoRedirectAndSendsOk("ELB-HealthChecker/2.0");
    }

    @Test
    public void shouldFilterIfMatchesRootUpper() throws IOException, ServletException {
        shouldCheckRedirected("HTTPS://www.tramchester.COM/", "https://www.tramchester.com/app", "SomeUserAgent");
    }

    @Test
    public void shouldFilterIfRootOnly() throws IOException, ServletException {
        shouldCheckRedirected("https://blue.tramchester.com/", "https://blue.tramchester.com/app", "SomeUserAgent");
    }

    @Test
    public void shouldFilterIfRootOnlyLocalHost() throws IOException, ServletException {
        shouldCheckRedirected("http://localhost:8080/", "http://localhost:8080/app", "SomeUserAgent");
    }

    @Test
    public void shouldNotFilterIfNotRoot() throws IOException, ServletException {
        shouldCheckNoRedirect("https://www.tramchester.com/app", "userAgent");
    }

    @Test
    public void shouldNotFilterIfAPI() throws IOException, ServletException {
        shouldCheckNoRedirect("https://www.tramchester.com/api", "userAgent");
    }

    @Test
    public void shouldNotFilterIfAPIOtherRoos() throws IOException, ServletException {
        shouldCheckNoRedirect("https://blue.tramchester.com/app", "userAgent");
    }

    private void shouldCheckRedirected(String original, String expected, String userAgent) throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("User-Agent")).andReturn(userAgent);
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(original));
        response.sendRedirect(expected);
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
    }

    private void shouldCheckNoRedirect(String urlToCheck, String userAgent) throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("User-Agent")).andReturn(userAgent);
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(urlToCheck));
        chain.doFilter(request,response);
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
    }

    private void shouldCheckNoRedirectAndSendsOk(String userAgent) throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("User-Agent")).andReturn(userAgent);
        response.setStatus(200);
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
    }
}
