package com.tramchester.domain.presentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.places.Station;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RecentJourneys {
    private Set<Timestamped> timestamps;

    public RecentJourneys() {
        // deserialisation
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecentJourneys that = (RecentJourneys) o;
        return timestamps.equals(that.timestamps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamps);
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
    public boolean containsStationId(IdFor<Station> id) {
        for (Timestamped timestamp : timestamps) {
            if (timestamp.getId().equals(id.forDTO())) {
                return true;
            }
        }
        return false;
    }

    public static RecentJourneys empty() {
        return new RecentJourneys().setTimestamps(new HashSet<>());
    }

    public static RecentJourneys decodeCookie(ObjectMapper objectMapper, String cookieString) throws IOException {
        String decoded = URLDecoder.decode(cookieString, StandardCharsets.UTF_8);
        return objectMapper.readValue(decoded, RecentJourneys.class);
    }

    public static String encodeCookie(ObjectMapper objectMapper, RecentJourneys recentJourneys) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(recentJourneys);
        return URLEncoder.encode(json, StandardCharsets.UTF_8);
    }

}
