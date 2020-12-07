package com.tramchester.unit;


import com.tramchester.RedirectToHttpsUsingELBProtoHeader;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

class RedirectHttpFilterTest extends EasyMockSupport {

    private final RedirectToHttpsUsingELBProtoHeader filter = new RedirectToHttpsUsingELBProtoHeader(new IntegrationTramTestConfig());
    private final String path = "http://green.tramchester.co.uk/somethingelse";
    private final String expected = "https://green.tramchester.com/somethingelse";

    @Test
    void shouldFormNewURLLower() throws MalformedURLException, URISyntaxException {
        String result = filter.mapUrl(path);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldFormNewURLUpper() throws MalformedURLException, URISyntaxException {
        String original = path.replace("http","HTTP");

        String result = filter.mapUrl(original);

        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldFilterIfCorrectHeaderIsSetTramBusterCoUk() {
        Assertions.assertAll(() -> checkUrl("http://green.trambuster.co.uk/somethingelse"));
    }

    @Test
    void shouldFilterIfCorrectHeaderIsSetTramBusterCom() {
        Assertions.assertAll(() -> checkUrl("http://green.trambuster.com/somethingelse"));
    }

    @Test
    void shouldFilterIfCorrectHeaderIsSetTramBusterIndo() {
        Assertions.assertAll(() -> checkUrl("http://green.trambuster.info/somethingelse"));
    }

    @Test
    void shouldFilterIfCorrectHeaderIsSetTramChesterCoUK() {
        Assertions.assertAll(() -> checkUrl("http://green.tramchester.co.uk/somethingelse"));
    }

    @Test
    void shouldFilterIfCorrectHeaderIsSetTramChesterInfo() {
        Assertions.assertAll(() -> checkUrl("http://green.tramchester.info/somethingelse"));
    }

    @Test
    void shouldFilterIfCorrectHeaderIsSetTramChesterCom() {
        Assertions.assertAll(() -> checkUrl("http://green.tramchester.com/somethingelse"));
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
    void shouldNotFilterIfCHeaderIsMissing() throws IOException, ServletException {
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("X-Forwarded-Proto")).andReturn(null);
        chain.doFilter(request,response);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> filter.doFilter(request, response, chain));
        verifyAll();
    }
}
