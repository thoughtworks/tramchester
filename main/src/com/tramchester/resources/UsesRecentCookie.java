package com.tramchester.resources;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import java.io.IOException;
import java.net.URI;

import static java.lang.String.format;

public class UsesRecentCookie extends TransportResource {
    private static final Logger logger = LoggerFactory.getLogger(UsesRecentCookie.class);

    public static final String TRAMCHESTER_RECENT = "tramchesterRecent";
    private static final int VERSION = 1;

    private final UpdateRecentJourneys updateRecentJourneys;
    protected final ObjectMapper mapper;

    public UsesRecentCookie(UpdateRecentJourneys updateRecentJourneys, ProvidesNow providesNow, ObjectMapper mapper) {
        super(providesNow);
        this.updateRecentJourneys = updateRecentJourneys;
        this.mapper = mapper;
    }

    protected RecentJourneys recentFromCookie(Cookie cookie) {
        if (cookie==null) {
            return RecentJourneys.empty();
        }
        String cookieString = cookie.getValue();
        try {
            RecentJourneys recent = RecentJourneys.decodeCookie(mapper,cookieString);
            return recent==null ? RecentJourneys.empty() : recent;
        } catch (IOException e) {
            logger.warn("Unable to decode cookie for recent journeys: "+cookieString,e);
            return RecentJourneys.empty();
        }
    }

    protected NewCookie createRecentCookie(Cookie cookie, String fromId, String endId, boolean secure, URI baseURI) throws JsonProcessingException {
        logger.info(format("Updating recent stations cookie with %s and %s ",fromId, endId));
        RecentJourneys recentJourneys = recentFromCookie(cookie);
        if (!isFromMyLocation(fromId)) {
            // don't add MyLocation to recents list
            recentJourneys = updateRecentJourneys.createNewJourneys(recentJourneys, providesNow, fromId);
        }
        recentJourneys = updateRecentJourneys.createNewJourneys(recentJourneys, providesNow, endId);

        int maxAgeSecs = 60 * 60 * 24 * 100;

        // NOTE: SameSite is set via ResponseCookieFilter as NewCookie can't set SameSite (yet, TODO)
        return new NewCookie(TRAMCHESTER_RECENT, RecentJourneys.encodeCookie(mapper, recentJourneys)
            , "/api", baseURI.getHost(), VERSION,
                "tramchester recent journeys", maxAgeSecs, secure);
    }

    private boolean isFromMyLocation(String startId) {
        return MyLocation.MY_LOCATION_PLACEHOLDER_ID.equals(startId);
    }
}
