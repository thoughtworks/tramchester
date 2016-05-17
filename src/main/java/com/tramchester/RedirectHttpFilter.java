package com.tramchester;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RedirectHttpFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String header = httpServletRequest.getHeader("X-Forwarded-Proto");
        if (header!=null) {
            if ("http".equals(header.toLowerCase())) {
                String location = mapUrl(httpServletRequest.getRequestURL().toString());
                ((HttpServletResponse) response).sendRedirect(location);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    public String mapUrl(String originalURL) {
        String norm = originalURL.toLowerCase();

        return norm.replaceFirst("http", "https");
    }

    @Override
    public void destroy() {

    }
}
