package com.tramchester;


import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RedirectHttpFilterTest extends EasyMockSupport {

    private RedirectHttpFilter filter = new RedirectHttpFilter();

    @Test
    public void shouldFormNewURLLower() {
        String path = "someurl.com/somethingelse";
        String original = "http://" + path;

        String result = filter.mapUrl(original);

        assertEquals("https://"+path, result);
    }

    @Test
    public void shouldFormNewURLUpper() {
        String path = "someurl.com/somethingelse";
        String original = "HTTP://" + path;

        String result = filter.mapUrl(original);

        assertEquals("https://"+path, result);
    }

    @Test
    public void shouldFilterIfCorrectHeaderIsSet() throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("X-Forwarded-Proto")).andReturn("http");
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer("http://something.com/someotherstuff"));
        response.sendRedirect("https://something.com/someotherstuff");
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
    }

    @Test
    public void shouldNotFilterIfCHeaderIsMissing() throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("X-Forwarded-Proto")).andReturn(null);
        chain.doFilter(request,response);
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
    }
}
