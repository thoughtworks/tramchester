package com.tramchester;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;

public class RedirectToAppFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RedirectToAppFilter.class);

    public RedirectToAppFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String userAgent = httpServletRequest.getHeader("User-Agent");
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        if (userAgent.startsWith("ELB-HealthChecker")) {
            servletResponse.setStatus(SC_OK);
            return;
        }

        String location = httpServletRequest.getRequestURL().toString().toLowerCase();
        URL url = new URL(location);
        if (url.getPath().equals("/")) {
            String redirection = url.toString() + "app";
            logger.info(format("Redirect from %s to %s", url, redirection));
            servletResponse.sendRedirect(redirection);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
