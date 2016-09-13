package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.mappers.TimeJsonSerializer;
import org.joda.time.LocalTime;

import java.util.List;

@JsonDeserialize(as=JourneyDTO.class)
public interface PresentableJourney {
    List<PresentableStage> getStages();

    String getSummary();

    String getHeading();

    @JsonSerialize(using = TimeJsonSerializer.class)
    LocalTime getFirstDepartureTime();

    @JsonSerialize(using = TimeJsonSerializer.class)
    LocalTime getExpectedArrivalTime();

    Location getBegin();

    Location getEnd();
}
