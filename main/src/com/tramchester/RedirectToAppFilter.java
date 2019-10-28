package com.tramchester;


import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static java.lang.String.format;

public class RedirectToAppFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RedirectToAppFilter.class);

    private TramchesterConfig config;

    public RedirectToAppFilter(TramchesterConfig config) {
        this.config = config;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String location = httpServletRequest.getRequestURL().toString().toLowerCase();
        URL url = new URL(location);
        if (url.getPath().equals("/")) {
            String redirection = url.toString() + "app";
            logger.info(format("Redirect from %s to %s", url , redirection));
            ((HttpServletResponse) response).sendRedirect(redirection);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
