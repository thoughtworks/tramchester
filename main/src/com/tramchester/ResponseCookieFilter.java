package com.tramchester;

import com.tramchester.resources.UsesRecentCookie;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.NewCookie;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ResponseCookieFilter implements ContainerResponseFilter {
    private static final Logger logger = LoggerFactory.getLogger(ResponseCookieFilter.class);
    private static final String HEADER = HttpHeader.SET_COOKIE.asString();
    private static final String SAMESITE = " ;SameSite=" + HttpCookie.SameSite.STRICT.name();

    // WORKAROUND
    // TODO This is a workaround for the inability to set 'samesite' header via NewCookie class in jersey
    // Hopefully once new jersey out and Dropwizard updated then can be removed
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Map<String, NewCookie> cookies = responseContext.getCookies();
        if (cookies.containsKey(UsesRecentCookie.TRAMCHESTER_RECENT)) {
            logger.debug("Cookie present, need to add samesite value");
            String cookieHeader = responseContext.getHeaderString(HEADER);
            String replacement = cookieHeader + SAMESITE;
            responseContext.getHeaders().remove(HEADER);
            responseContext.getHeaders().add(HEADER, replacement);
            logger.info("Added "+SAMESITE+" to cookie");
        }
    }
}
