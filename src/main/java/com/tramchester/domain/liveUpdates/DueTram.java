package com.tramchester.domain.liveUpdates;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.DateTimeJsonDeserializer;
import com.tramchester.mappers.DateTimeJsonSerializer;
import org.joda.time.DateTime;

public class DueTram {
    private String status;
    private String destination;
    private int wait;
    private String carriages;
    private DateTime when;

    public DueTram() {
        // deserialisation
    }

    public DueTram(String destination, String status, int wait, String carriages, DateTime updateTime) {
        this.destination = destination;
        this.status = status;
        this.wait = wait;
        this.carriages = carriages;
        when = updateTime.plusMinutes(wait);
    }

    public String getDestination() {
        return destination;
    }

    public String getStatus() {
        return status;
    }

    public int getWait() {
        return wait;
    }

    public String getCarriages() {
        return carriages;
    }

    @Override
    public String toString() {
        return "DueTram{" +
                "status='" + status + '\'' +
                ", destination='" + destination + '\'' +
                ", wait=" + wait +
                ", carriages='" + carriages + '\'' +
                '}';
    }

    @JsonSerialize(using = DateTimeJsonSerializer.class)
    @JsonDeserialize(using = DateTimeJsonDeserializer.class)
    public DateTime getWhen() {
        return when;
    }
}
