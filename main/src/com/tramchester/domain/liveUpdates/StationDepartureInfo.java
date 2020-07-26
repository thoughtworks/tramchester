package com.tramchester.domain.liveUpdates;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StationDepartureInfo implements HasPlatformMessage {

    private static final String NO_MESSAGE = "<no message>";

    public enum Direction {
        Incoming, Outgoing, Both, Unknown
    }

    private final String lineName;
    private final IdFor<Platform> stationPlatform;
    private final String message;
    private final List<DueTram> dueTrams;
    private final LocalDateTime lastUpdate;
    private final String displayId;
    private final Station station;
    private final Direction direction;

    // station code here is the actocode
    public StationDepartureInfo(String displayId, String lineName, Direction direction, String stationPlatform,
                                Station station, String message, LocalDateTime lastUpdate) {
        this.displayId = displayId;
        this.lineName = lineName;
        this.direction = direction;
        this.stationPlatform = IdFor.createId(stationPlatform);
        this.station = station;
        if (invalidMessage(message)) {
            this.message= "";
        } else {
            this.message = message;
        }
        this.lastUpdate = lastUpdate;
        dueTrams = new LinkedList<>();
    }

    private boolean invalidMessage(String message) {
        return NO_MESSAGE.equals(message) || scrollingDisplay(message);
    }

    private boolean scrollingDisplay(String message) {
        return message.startsWith("^F0Next");
    }

    public String getLineName() {
        return lineName;
    }

    public Direction getDirection() {
        return direction;
    }

    public IdFor<Platform> getStationPlatform() {
        return stationPlatform;
    }

    public String getMessage() {
        return message;
    }

    public List<DueTram> getDueTrams() {
        return dueTrams;
    }

    public List<DueTram> getDueTramsWithinWindow(int minutes) {
        return dueTrams.stream().filter(dueTram -> dueTram.getWait()<=minutes).collect(Collectors.toList());
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void addDueTram(DueTram dueTram) {
        dueTrams.add(dueTram);
    }

    public boolean hasDueTram(DueTram dueTram) {
        return dueTrams.contains(dueTram);
    }

    public String getDisplayId() {
        return displayId;
    }

    @Override
    public Station getStation() {
        return station;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationDepartureInfo that = (StationDepartureInfo) o;
        return lineName.equals(that.lineName) &&
                stationPlatform.equals(that.stationPlatform) &&
                message.equals(that.message) &&
                dueTrams.equals(that.dueTrams) &&
                lastUpdate.equals(that.lastUpdate) &&
                displayId.equals(that.displayId) &&
                station.equals(that.station) &&
                direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineName, stationPlatform, message, dueTrams, lastUpdate, displayId, station, direction);
    }

    @Override
    public String toString() {
        return "StationDepartureInfo{" +
                "lineName='" + lineName + '\'' +
                ", stationPlatform='" + stationPlatform + '\'' +
                ", message='" + message + '\'' +
                ", dueTrams=" + dueTrams +
                ", lastUpdate=" + lastUpdate +
                ", displayId='" + displayId + '\'' +
                ", location='" + station + '\'' +
                ", direction=" + direction +
                '}';
    }
}
