package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.HasPlatformMessage;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonSerializer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StationDepartureInfoDTO implements HasPlatformMessage {
    private String lineName;
    private String stationPlatform;
    private String message;
    private List<DepartureDTO> dueTrams;
    private LocalDateTime lastUpdate;
    private String displayId;
    private String location;

    public StationDepartureInfoDTO(String lineName, String stationPlatform, String message, List<DepartureDTO> dueTrams,
                                   LocalDateTime lastUpdate, String displayId, String location) {
        this.lineName = lineName;
        this.stationPlatform = stationPlatform;
        this.message = message;
        this.dueTrams = dueTrams;
        this.lastUpdate = lastUpdate;
        this.displayId = displayId;
        this.location = location;
    }

    public StationDepartureInfoDTO(StationDepartureInfo info) {
        this(info.getLineName(),
                info.getStationPlatform(),
                info.getMessage(),
                mapDueTrams(info.getLocation(), info.getDueTrams()),
                info.getLastUpdate(),
                info.getDisplayId(),
                info.getLocation());
    }

    public StationDepartureInfoDTO() {
        // deserialisation
    }

    private static List<DepartureDTO> mapDueTrams(String location, List<DueTram> dueTrams) {
        return dueTrams.stream().map(dueTram -> new DepartureDTO(location, dueTram)).collect(Collectors.toList());
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

    public List<DepartureDTO> getDueTrams() {
        return dueTrams;
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public String getDisplayId() {
        return displayId;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationDepartureInfoDTO that = (StationDepartureInfoDTO) o;
        return lineName.equals(that.lineName) &&
                stationPlatform.equals(that.stationPlatform) &&
                message.equals(that.message) &&
                dueTrams.equals(that.dueTrams) &&
                lastUpdate.equals(that.lastUpdate) &&
                displayId.equals(that.displayId) &&
                location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineName, stationPlatform, message, dueTrams, lastUpdate, displayId, location);
    }

    @Override
    public String toString() {
        return "StationDepartureInfoDTO{" +
                "lineName='" + lineName + '\'' +
                ", stationPlatform='" + stationPlatform + '\'' +
                ", message='" + message + '\'' +
                ", dueTrams=" + dueTrams +
                ", lastUpdate=" + lastUpdate +
                ", displayId='" + displayId + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
