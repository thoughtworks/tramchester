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
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class RedirectToHttpsUsingELBProtoHeader implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RedirectToHttpsUsingELBProtoHeader.class);
    public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    private final TramchesterConfig config;

    // no cert for these hosts, redirect to secure domain
    private final List<String> unsecureHosts = Arrays.asList("trambuster.com",
            "trambuster.info",
            "trambuster.co.uk",
            "tramchester.co.uk",
            "tramchester.info");

    public RedirectToHttpsUsingELBProtoHeader(TramchesterConfig config) {
        this.config = config;
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String header = httpServletRequest.getHeader(X_FORWARDED_PROTO); // https is terminated by the ELB
        try {
            if (header != null) {
                if ("http".equalsIgnoreCase(header)) {
                    logger.info("found http in " +X_FORWARDED_PROTO+ " need to redirect to https");
                    String location = mapUrl(httpServletRequest.getRequestURL().toString());
                    ((HttpServletResponse) response).sendRedirect(location);
                    return;
                }
            }
        }
        catch(URISyntaxException unableToMap) {
            logger.error("Unable to parse the URL, redirecting to the secure host");
            ((HttpServletResponse) response).sendRedirect("https://"+config.getSecureHost());
        }
        chain.doFilter(request, response);
    }

    public String mapUrl(String originalURL) throws URISyntaxException, MalformedURLException {
        // Note: only called on initial redirection
        logger.debug("Mapping url "+originalURL);
        String result = "https"+ originalURL.substring(4);

        URL url = new URI(originalURL).toURL();
        String host = url.getHost();

        for (String unsecure: unsecureHosts) {
            if (host.toLowerCase().endsWith(unsecure)) {
                result = result.replaceFirst(unsecure, config.getSecureHost());
                break;
            }
        }

        logger.info(format("Mapped URL %s to %s",originalURL,result));
        return result;
    }

    @Override
    public void destroy() {

    }
}
