package com.tramchester;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StaticAssetFilter implements Filter {

    private final int seconds;

    public StaticAssetFilter(int seconds) {
        this.seconds = seconds;
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        httpServletResponse.setHeader("Cache-Control", "public, max-age=" + seconds);

        chain.doFilter(request,response);
    }

    @Override
    public void destroy() {

    }
}
