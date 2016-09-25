package com.tramchester.resources;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.RecentJourneys;
import com.tramchester.domain.UpdateRecentJourneys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class UsesRecentCookie {
    private static final Logger logger = LoggerFactory.getLogger(UsesRecentCookie.class);

    public static final String TRAMCHESTER_RECENT = "tramchesterRecent";

    private UpdateRecentJourneys updateRecentJourneys;
    protected final ObjectMapper mapper;

    public UsesRecentCookie(UpdateRecentJourneys updateRecentJourneys, ObjectMapper mapper) {
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


    protected NewCookie createRecentCookie(String fromId, Cookie cookie) throws UnsupportedEncodingException, JsonProcessingException {
        RecentJourneys recentJourneys = recentFromCookie(cookie);
        recentJourneys = updateRecentJourneys.processFrom(recentJourneys,fromId);
        return new NewCookie(TRAMCHESTER_RECENT, RecentJourneys.encodeCookie(mapper,recentJourneys));
    }
}
