package com.tramchester.unit;


import com.tramchester.RedirectToAppFilter;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RedirectToAppFilterTest extends EasyMockSupport {

    private RedirectToAppFilter filter = new RedirectToAppFilter(new IntegrationTramTestConfig());

    @Test
    public void shouldFilterIfMatchesRoot() throws IOException, ServletException {
        shouldCheckRedirected("https://www.tramchester.com/", "https://www.tramchester.com/app");
    }

    @Test
    public void shouldFilterIfMatchesRootUpper() throws IOException, ServletException {
        shouldCheckRedirected("HTTPS://www.tramchester.COM/", "https://www.tramchester.com/app");
    }

    @Test
    public void shouldFilterIfRootOnly() throws IOException, ServletException {
        shouldCheckRedirected("https://blue.tramchester.com/", "https://blue.tramchester.com/app");
    }

    @Test
    public void shouldFilterIfRootOnlyLocalHost() throws IOException, ServletException {
        shouldCheckRedirected("http://localhost:8080/", "http://localhost:8080/app");
    }

    @Test
    public void shouldNotFilterIfNotRoot() throws IOException, ServletException {
        shouldCheckNoRedirect("https://www.tramchester.com/app");
    }

    @Test
    public void shouldNotFilterIfAPI() throws IOException, ServletException {
        shouldCheckNoRedirect("https://www.tramchester.com/api");
    }

    @Test
    public void shouldNotFilterIfAPIOtherRoos() throws IOException, ServletException {
        shouldCheckNoRedirect("https://blue.tramchester.com/app");
    }

    private void shouldCheckRedirected(String original, String expected) throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(original));
        response.sendRedirect(expected);
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
    }

    private void shouldCheckNoRedirect(String urlToCheck) throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(urlToCheck));
        chain.doFilter(request,response);
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
    }
}
