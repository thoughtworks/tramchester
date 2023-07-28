package com.tramchester.integration;


import com.tramchester.RedirectToHttpsUsingELBProtoHeader;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

class RedirectHttpFilterTest extends EasyMockSupport {

    private final RedirectToHttpsUsingELBProtoHeader filter = new RedirectToHttpsUsingELBProtoHeader(new IntegrationTramTestConfig());
    private final String path = "http://green.tramchester.co.uk/somethingelse";
    private final String expected = "https://green.tramchester.com/somethingelse";

    @Test
    void shouldFormNewURLLower() throws MalformedURLException {
        URL result = filter.mapUrl(URI.create(path).toURL());
        Assertions.assertEquals(expected, result.toExternalForm());
    }

    @Test
    void shouldFormNewURLUpper() throws MalformedURLException {
        String original = path.replace("http","HTTP");

        URL result = filter.mapUrl(URI.create(original).toURL());

        Assertions.assertEquals(expected, result.toExternalForm());
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

    @Test
    void shouldFilterIfCorrectHeaderIsSetTramChesterComWithQuery() throws IOException, ServletException {
        String original = "http://green.tramchester.com/somethingelse?query=xyz";

        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("X-Forwarded-Proto")).andReturn("http");
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(original));
        response.sendRedirect("https://green.tramchester.com/somethingelse?query=xyz");
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
    }

    @Test
    void shouldBadGatewayIfBadUrl() throws IOException, ServletException {
        String original = "xzy://somethingOdd_here";

        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("X-Forwarded-Proto")).andReturn("http");
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(original));
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
    }

    @Test
    void shouldBadGatewayIfUnrecognisedHost() throws IOException, ServletException {
        String original = "http://www.someInvalidSite.com/";

        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);
        FilterChain chain = createMock(FilterChain.class);

        EasyMock.expect(request.getHeader("X-Forwarded-Proto")).andReturn("http");
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer(original));
        response.sendError(HttpServletResponse.SC_BAD_GATEWAY);
        EasyMock.expectLastCall();

        replayAll();
        filter.doFilter(request, response, chain);
        verifyAll();
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
        EasyMock.expect(request.getRequestURL()).andReturn(new StringBuffer("http://localhost:8080"));
        chain.doFilter(request,response);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> filter.doFilter(request, response, chain));
        verifyAll();
    }
}
