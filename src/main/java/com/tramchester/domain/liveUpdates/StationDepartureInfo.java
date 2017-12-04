package com.tramchester.domain.liveUpdates;

import com.tramchester.domain.liveUpdates.DueTram;
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

    public DateTime getLastUpdate() {
        return lastUpdate;
    }

    public void addDueTram(DueTram dueTram) {
        dueTrams.add(dueTram);
    }
}
