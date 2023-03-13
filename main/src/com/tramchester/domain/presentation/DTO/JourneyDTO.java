package com.tramchester.domain.presentation.DTO;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonTypeName("journey")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use= JsonTypeInfo.Id.NAME)
public class JourneyDTO {

    private LocationRefWithPosition begin;
    private List<SimpleStageDTO> stages;
    private LocalDateTime expectedArrivalTime; // needed to handle 'next day' results
    private LocalDateTime firstDepartureTime;  // needed to handle 'next day' results
    private List<LocationRefWithPosition> changeStations;
    private TramTime queryTime;
    private List<Note> notes;
    private List<LocationRefWithPosition> path;
    private LocalDate queryDate;

    public JourneyDTO() {
        // Deserialization
    }

    public JourneyDTO(LocationRefWithPosition begin, List<SimpleStageDTO> stages,
                      LocalDateTime expectedArrivalTime, LocalDateTime firstDepartureTime,
                      List<LocationRefWithPosition> changeStations, TramTime queryTime, List<Note> notes,
                      List<LocationRefWithPosition> path, TramDate queryDate) {
        this(begin, stages, expectedArrivalTime, firstDepartureTime, changeStations, queryTime, notes,
                path, queryDate.toLocalDate());
    }

    public JourneyDTO(LocationRefWithPosition begin, List<SimpleStageDTO> stages,
                      LocalDateTime expectedArrivalTime, LocalDateTime firstDepartureTime,
                      List<LocationRefWithPosition> changeStations, TramTime queryTime, List<Note> notes,
                      List<LocationRefWithPosition> path, LocalDate queryDate) {
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

    public List<SimpleStageDTO> getStages() {
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

    public LocationRefDTO getBegin() {
        return begin;
    }

    public List<LocationRefWithPosition> getChangeStations() {
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

    public List<LocationRefWithPosition> getPath() {
        return path;
    }

    @JsonSerialize(using = LocalDateJsonSerializer.class)
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public LocalDate getQueryDate() {
        return queryDate;
    }
}
