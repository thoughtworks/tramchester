package com.tramchester.domain.presentation.DTO;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.CallsAtPlatforms;
import com.tramchester.domain.HasId;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.TramTimeJsonSerializer;

import java.util.List;
import java.util.stream.Collectors;

@JsonTypeName("journey")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use= JsonTypeInfo.Id.NAME)
public class JourneyDTO implements CallsAtPlatforms {

    private StationRefWithPosition begin;
    private StationRefWithPosition end;
    private List<StageDTO> stages;
    private TramTime expectedArrivalTime;
    private TramTime firstDepartureTime;
    private boolean isDirect;
    private List<String> changeStations;
    private TramTime queryTime;
    private List<Note> notes;
    private List<StationRefWithPosition> path;

    public JourneyDTO() {
        // Deserialization
    }

    public JourneyDTO(StationRefWithPosition begin, StationRefWithPosition end, List<StageDTO> stages,
                      TramTime expectedArrivalTime, TramTime firstDepartureTime, boolean isDirect,
                      List<String> changeStations, TramTime queryTime, List<Note> notes,
                      List<StationRefWithPosition> path) {
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
    }

    public List<StageDTO> getStages() {
        return stages;
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getExpectedArrivalTime() {
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

    public List<String> getChangeStations() {
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
    public List<HasId> getCallingPlatformIds() {
        return stages.stream().filter(StageDTO::getHasPlatform).map(StageDTO::getPlatform).collect(Collectors.toList());
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
}
