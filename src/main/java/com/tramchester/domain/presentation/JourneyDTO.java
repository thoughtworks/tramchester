package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.mappers.TimeJsonDeserializer;
import com.tramchester.mappers.TimeJsonSerializer;
import org.joda.time.LocalTime;

import java.util.List;

public class JourneyDTO implements PresentableJourney {

    private LocalTime expectedArrivalTime;
    private Location end;
    private Location begin;
    private LocalTime firstDepartureTime;
    private String summary;
    private String heading;
    private List<PresentableStage> stages;

    public JourneyDTO() {
        // Deserialization
    }

    public JourneyDTO(Journey original) {
        this.stages = original.getStages();
        this.summary = original.getSummary();
        this.heading = original.getHeading();
        this.firstDepartureTime = original.getFirstDepartureTime();
        this.expectedArrivalTime = original.getExpectedArrivalTime();
        this.end = original.getEnd();
        this.begin = original.getBegin();
    }

    @Override
    public List<PresentableStage> getStages() {
        return stages;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public String getHeading() {
        return heading;
    }

    @Override
    @JsonSerialize(using = TimeJsonSerializer.class)
    @JsonDeserialize(using = TimeJsonDeserializer.class)
    public LocalTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @Override
    @JsonSerialize(using = TimeJsonSerializer.class)
    @JsonDeserialize(using = TimeJsonDeserializer.class)
    public LocalTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    @Override
    public Location getBegin() {
        return begin;
    }

    @Override
    public Location getEnd() {
        return end;
    }
}
