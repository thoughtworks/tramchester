package com.tramchester.domain.presentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.Timestamped;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

public class RecentJourneys {
    private Set<Timestamped> timestamps;

    public RecentJourneys() {
        // deserialisation
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==null) return false;

        RecentJourneys other = (RecentJourneys) obj;
        if (other==null) return false;

        return this.timestamps.equals(other.timestamps);
    }

    @JsonIgnore
    public RecentJourneys setTimestamps(Set<Timestamped> timestamps) {
        setRecentIds(timestamps);
        return this;
    }

    public void setRecentIds(Set<Timestamped> timestamps) {
        this.timestamps = timestamps;
    }

    public Set<Timestamped> getRecentIds() {
        return timestamps;
    }

    @JsonIgnore
    public boolean contains(String id) {
        return timestamps.contains(new Timestamped(id, DateTime.now()));
    }

    public static RecentJourneys empty() {
        return new RecentJourneys().setTimestamps(new HashSet<>());
    }

    public static RecentJourneys decodeCookie(ObjectMapper objectMapper, String cookieString) throws IOException {
        String decoded = URLDecoder.decode(cookieString,"UTF-8");
        return objectMapper.readValue(decoded, RecentJourneys.class);
    }

    public static String encodeCookie(ObjectMapper objectMapper, RecentJourneys recentJourneys) throws JsonProcessingException, UnsupportedEncodingException {
        String json = objectMapper.writeValueAsString(recentJourneys);
        return URLEncoder.encode(json, "UTF-8");
    }
}
