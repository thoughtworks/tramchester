package com.tramchester.domain.liveUpdates;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.LocalDateJsonDeserializer;
import com.tramchester.mappers.LocalDateJsonSerializer;
import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;

public class StationDepartureInfo {

    private String lineName;
    private String stationPlatform;
    private String message;
    private List<DueTram> dueTrams;
    private DateTime lastUpdate;

    public StationDepartureInfo(String lineName, String stationPlatform, String message, DateTime lastUpdate) {
        this.lineName = lineName;
        this.stationPlatform = stationPlatform;
        this.message = message;
        this.lastUpdate = lastUpdate;
        dueTrams = new LinkedList<>();
    }

    public StationDepartureInfo() {
        // deserialisation
    }

    public String getLineName() {
        return lineName;
    }

    public String getStationPlatform() {
        return stationPlatform;
    }

    public String getMessage() {
        return message;
    }

    public List<DueTram> getDueTrams() {
        return dueTrams;
    }

   // @JsonSerialize(using = LocalDateJsonSerializer.class)
   // @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public DateTime getLastUpdate() {
        return lastUpdate;
    }

    @JsonIgnore
    public void addDueTram(DueTram dueTram) {
        dueTrams.add(dueTram);
    }
}
