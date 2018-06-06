package com.tramchester.domain.liveUpdates;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.TramTime;
import com.tramchester.mappers.DateTimeJsonDeserializer;
import com.tramchester.mappers.DateTimeJsonSerializer;
import com.tramchester.mappers.TramTimeJsonDeserializer;
import com.tramchester.mappers.TramTimeJsonSerializer;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;

public class DueTram {

    private int wait;

    private String carriages;
    private String status;
    private String destination;
    private TramTime when;

    public DueTram() {
        // deserialisation
    }

    public DueTram(String destination, String status, int wait, String carriages, LocalTime updateTime) {
        this.destination = destination;
        this.status = status;
        this.wait = wait;
        this.carriages = carriages;
        this.when  = TramTime.create(updateTime).plusMinutes(wait);
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

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getWhen() {
        return when;
    }
}
