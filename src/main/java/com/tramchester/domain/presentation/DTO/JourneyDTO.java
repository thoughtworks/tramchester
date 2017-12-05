package com.tramchester.domain.presentation.DTO;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.livedata.EnrichPlatform;
import com.tramchester.mappers.TimeJsonDeserializer;
import com.tramchester.mappers.TimeJsonSerializer;
import org.joda.time.LocalTime;

import java.util.List;
import java.util.stream.Collectors;

public class JourneyDTO implements Comparable<JourneyDTO> {

    private LocalTime expectedArrivalTime;
    private LocationDTO end;
    private LocationDTO begin;
    private LocalTime firstDepartureTime;
    private String summary;
    private String heading;
    private List<StageDTO> stages;

    public JourneyDTO() {
        // Deserialization
    }

    // TODO should this care about enrichment, use constructor that takes List<StageDTO> stages instead?
    public JourneyDTO(Journey original, EnrichPlatform liveDataEnricher) {
        this.stages = original.getStages().stream().map(stage -> stage.asDTO(liveDataEnricher)).collect(Collectors.toList());
        this.summary = original.getSummary();
        this.heading = original.getHeading();
        this.firstDepartureTime = original.getFirstDepartureTime();
        this.expectedArrivalTime = original.getExpectedArrivalTime();
        this.end = new LocationDTO(original.getEnd());
        this.begin = new LocationDTO(original.getBegin());
    }

    public List<StageDTO> getStages() {
        return stages;
    }

    public String getSummary() {
        return summary;
    }

    public String getHeading() {
        return heading;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    @JsonDeserialize(using = TimeJsonDeserializer.class)
    public LocalTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    @JsonDeserialize(using = TimeJsonDeserializer.class)
    public LocalTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public Location getBegin() {
        return begin;
    }

    public Location getEnd() {
        return end;
    }

    @Override
    public int compareTo(JourneyDTO other) {
        // arrival first
        int compare = checkArrival(other);
        // then departure time
        if (compare==0) {
            compare = checkDeparture(other);
        }
        // then number of stages
        if (compare==0) {
            // if arrival times match, put journeys with fewer stages first
            if (this.stages.size()<other.stages.size()) {
                compare = -1;
            } else if (other.stages.size()>stages.size()) {
                compare = 1;
            }
        }
        return compare;
    }

    private int checkDeparture(JourneyDTO other) {
        return TimeAsMinutes.compare(getFirstDepartureTime(),other.getFirstDepartureTime());
    }

    private int checkArrival(JourneyDTO other) {
        return TimeAsMinutes.compare(getExpectedArrivalTime(), other.getExpectedArrivalTime());
    }
}
