package com.tramchester.domain.presentation.DTO;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.mappers.LocalTimeJsonDeserializer;
import com.tramchester.mappers.LocalTimeJsonSerializer;
import org.joda.time.LocalTime;

import java.util.List;

public class JourneyDTO implements Comparable<JourneyDTO> {

    private LocationDTO begin;
    private LocationDTO end;
    private List<StageDTO> stages;
    private LocalTime expectedArrivalTime;
    private LocalTime firstDepartureTime;
    private String summary;
    private String heading;

    public JourneyDTO() {
        // Deserialization
    }

    public JourneyDTO(LocationDTO begin, LocationDTO end, List<StageDTO> stages,
                      LocalTime expectedArrivalTime, LocalTime firstDepartureTime,
                      String summary, String heading) {
        this.begin = begin;
        this.end = end;
        this.stages = stages;
        this.expectedArrivalTime = expectedArrivalTime;
        this.firstDepartureTime = firstDepartureTime;
        this.summary = summary;
        this.heading = heading;
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

    @JsonSerialize(using = LocalTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalTimeJsonDeserializer.class)
    public LocalTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonSerialize(using = LocalTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalTimeJsonDeserializer.class)
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
