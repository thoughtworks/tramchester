package com.tramchester;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StaticAssetFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(StaticAssetFilter.class);

    private final int seconds;

    public StaticAssetFilter(int seconds) {
        this.seconds = seconds;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("Init for " + filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        httpServletResponse.setHeader("Cache-Control", "public, max-age=" + seconds);

        chain.doFilter(request,response);
    }

    @Override
    public void destroy() {
        logger.info("destroy");
    }
}
