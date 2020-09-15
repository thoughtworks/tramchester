package com.tramchester.domain.presentation.DTO;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.CallsAtPlatforms;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonTypeName("journey")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use= JsonTypeInfo.Id.NAME)
public class JourneyDTO implements CallsAtPlatforms {

    private StationRefWithPosition begin;
    private StationRefWithPosition end;
    private List<StageDTO> stages;
    private LocalDateTime expectedArrivalTime; // need to handle 'next day' results
    private LocalDateTime firstDepartureTime;  // need to handle 'next day' results
    private boolean isDirect;
    private List<StationRefWithPosition> changeStations;
    private TramTime queryTime;
    private List<Note> notes;
    private List<StationRefWithPosition> path;
    private LocalDate queryDate;

    public JourneyDTO() {
        // Deserialization
    }

    public JourneyDTO(StationRefWithPosition begin, StationRefWithPosition end, List<StageDTO> stages,
                      LocalDateTime expectedArrivalTime, LocalDateTime firstDepartureTime, boolean isDirect,
                      List<StationRefWithPosition> changeStations, TramTime queryTime, List<Note> notes,
                      List<StationRefWithPosition> path, LocalDate queryDate) {
        this.begin = begin;
        this.end = end;
        this.stages = stages;
        this.expectedArrivalTime = expectedArrivalTime;
        this.firstDepartureTime = firstDepartureTime;
        this.isDirect = isDirect;
        this.changeStations = changeStations;
        this.queryTime = queryTime;
        this.notes = notes;
        this.path = path;
        this.queryDate = queryDate;
    }

    public List<StageDTO> getStages() {
        return stages;
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    public LocalDateTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    public LocalDateTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public StationRefDTO getBegin() {
        return begin;
    }

    public StationRefDTO getEnd() {
        return end;
    }

    public boolean getIsDirect() {
        return isDirect;
    }

    public List<StationRefWithPosition> getChangeStations() {
        return changeStations;
    }

    @Override
    public String toString() {
        return "JourneyDTO{" +
                "begin=" + begin +
                ", end=" + end +
                ", stages=" + stages +
                ", expectedArrivalTime=" + expectedArrivalTime +
                ", firstDepartureTime=" + firstDepartureTime +
                ", isDirect=" + isDirect +
                ", changeStations=" + changeStations +
                ", queryTime=" + queryTime +
                '}';
    }

    @JsonIgnore
    @Override
    public IdSet<Platform> getCallingPlatformIds() {
        return stages.stream().
                filter(StageDTO::getHasPlatform).
                map(StageDTO::getPlatform).
                map(PlatformDTO::getPlatformId).
                collect(IdSet.idCollector());
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getQueryTime() {
        return queryTime;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public List<StationRefWithPosition> getPath() {
        return path;
    }

    @JsonSerialize(using = LocalDateJsonSerializer.class)
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public LocalDate getQueryDate() {
        return queryDate;
    }
}
