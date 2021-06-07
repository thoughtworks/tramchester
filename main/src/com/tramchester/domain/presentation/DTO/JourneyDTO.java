package com.tramchester.domain.presentation.DTO;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonTypeName("journey")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use= JsonTypeInfo.Id.NAME)
public class JourneyDTO {

    private StationRefWithPosition begin;
    private List<StageDTO> stages;
    private LocalDateTime expectedArrivalTime; // need to handle 'next day' results
    private LocalDateTime firstDepartureTime;  // need to handle 'next day' results
    private List<StationRefWithPosition> changeStations;
    private TramTime queryTime;
    private List<Note> notes;
    private List<StationRefWithPosition> path;
    private LocalDate queryDate;

    public JourneyDTO() {
        // Deserialization
    }

    public JourneyDTO(StationRefWithPosition begin, List<StageDTO> stages,
                      LocalDateTime expectedArrivalTime, LocalDateTime firstDepartureTime,
                      List<StationRefWithPosition> changeStations, TramTime queryTime, List<Note> notes,
                      List<StationRefWithPosition> path, LocalDate queryDate) {
        this.begin = begin;
        this.stages = stages;
        this.expectedArrivalTime = expectedArrivalTime;
        this.firstDepartureTime = firstDepartureTime;
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

    public List<StationRefWithPosition> getChangeStations() {
        return changeStations;
    }

    @Override
    public String toString() {
        return "JourneyDTO{" +
                "begin=" + begin +
                ", stages=" + stages +
                ", expectedArrivalTime=" + expectedArrivalTime +
                ", firstDepartureTime=" + firstDepartureTime +
                ", changeStations=" + changeStations +
                ", queryTime=" + queryTime +
                '}';
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
