package com.tramchester.domain.presentation;

import com.tramchester.domain.Location;
import com.tramchester.domain.RawStage;
import com.tramchester.domain.presentation.DTO.StageDTO;
import org.joda.time.LocalTime;

public interface TransportStage extends RawStage {
    String getSummary();
    String getPrompt();
    String getHeadSign();

    Location getActionStation(); // place where action happens, i.e. Board At X or Walk To X
    Location getLastStation();
    Location getFirstStation();

    LocalTime getFirstDepartureTime();
    LocalTime getExpectedArrivalTime();

    int getDuration();

    StageDTO asDTO();
}
