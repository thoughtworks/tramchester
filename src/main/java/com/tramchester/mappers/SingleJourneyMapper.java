package com.tramchester.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.WalkingStage;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;

import java.util.List;
import java.util.Optional;

public abstract class SingleJourneyMapper {
    public abstract Optional<Journey> createJourney(RawJourney rawJourney, int withinMins);

    protected TravelAction decideAction(List<TransportStage> stagesSoFar) {
        if (stagesSoFar.isEmpty()) {
            return TravelAction.Board;
        }
        if ((stagesSoFar.get(stagesSoFar.size()-1) instanceof WalkingStage)) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }
}
