package com.tramchester;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StaticAssetFilter implements Filter {

    private int seconds;

    public StaticAssetFilter(int seconds) {
        this.seconds = seconds;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        seconds = 48 * 60 * 60;
        httpServletResponse.setHeader("Cache-Control", "public, max-age=" + seconds);

        chain.doFilter(request,response);
    }

    @Override
    public void destroy() {

    }
}
