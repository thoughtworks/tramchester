package com.tramchester.unit;


import com.tramchester.RedirectToHttpsUsingELBProtoHeader;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class RedirectHttpFilterTest extends EasyMockSupport {

    private RedirectToHttpsUsingELBProtoHeader filter = new RedirectToHttpsUsingELBProtoHeader(new IntegrationTramTestConfig());
    String path = "http://green.tramchester.co.uk/somethingelse";
    private String expected = "https://green.tramchester.com/somethingelse";

    @Test
    public void shouldFormNewURLLower() throws MalformedURLException, URISyntaxException {
        String result = filter.mapUrl(path);
        assertEquals(expected, result);
    }

    @Test
    public void shouldFormNewURLUpper() throws MalformedURLException, URISyntaxException {
        String original = path.replace("http","HTTP");

        String result = filter.mapUrl(original);

        assertEquals(expected, result);
    }

    @Test
    public void shouldFilterIfCorrectHeaderIsSetTramBusterCoUk() throws IOException, ServletException {
        checkUrl("http://green.trambuster.co.uk/somethingelse");
    }

    @Test
    public void shouldFilterIfCorrectHeaderIsSetTramBusterCom() throws IOException, ServletException {
        checkUrl("http://green.trambuster.com/somethingelse");
    }

    @Test
    public void shouldFilterIfCorrectHeaderIsSetTramBusterIndo() throws IOException, ServletException {
        checkUrl("http://green.trambuster.info/somethingelse");
    }

    @Test
    public void shouldFilterIfCorrectHeaderIsSetTramChesterCoUK() throws IOException, ServletException {
        checkUrl("http://green.tramchester.co.uk/somethingelse");
    }

    @Test
    public void shouldFilterIfCorrectHeaderIsSetTramChesterInfo() throws IOException, ServletException {
        checkUrl("http://green.tramchester.info/somethingelse");
    }

    @Test
    public void shouldFilterIfCorrectHeaderIsSetTramChesterCom() throws IOException, ServletException {
        checkUrl("http://green.tramchester.com/somethingelse");
    }

    private void checkUrl(String original) throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("X-Forwarded-Proto")).andReturn("http");
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(original));
        response.sendRedirect(expected);
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
