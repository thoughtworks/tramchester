package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LazySingleton
public class RailRouteIDBuilder {
    private final Map<RailRouteId, Integer> baseIdToIndex;
    private final Map<AgencyCallingPoints, RailRouteId> callingPointsToId;

    @Inject
    public RailRouteIDBuilder() {
        baseIdToIndex = new HashMap<>();
        callingPointsToId = new HashMap<>();
    }

    public RailRouteId getIdFor(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints) {
        // find unique route id based on the atoc code (aka agency) and list of calling points
        // have to include agency id since different agencies might serve same calling points

        AgencyCallingPoints agencyCallingPoints = new AgencyCallingPoints(agencyId, callingPoints);

        if (callingPointsToId.containsKey(agencyCallingPoints)) {
            return callingPointsToId.get(agencyCallingPoints);
        }

        int index = 1;
        RailRouteId railRouteId = RailRouteId.createId(agencyId, callingPoints, index);
        while (baseIdToIndex.containsKey(railRouteId)) {
            index = index + 1;
            railRouteId = RailRouteId.createId(agencyId, callingPoints, index);
        }

        baseIdToIndex.put(railRouteId, index);
        callingPointsToId.put(agencyCallingPoints, railRouteId);
        return railRouteId;

    }

    public RailRouteId getIdFor(RailRouteIdRepository.AgencyCallingPoints agencyCallingPoints) {
        return getIdFor(agencyCallingPoints.getAgencyId(), agencyCallingPoints.getCallingPoints());
    }

    private static class AgencyCallingPoints {
        private final IdFor<Agency> agencyId;
        private final List<IdFor<Station>> callingPoints;

        private AgencyCallingPoints(IdFor<Agency> agencyId, List<IdFor<Station>> stationCallingPoints) {
            this.callingPoints = stationCallingPoints;
            this.agencyId = agencyId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AgencyCallingPoints that = (AgencyCallingPoints) o;

            if (!agencyId.equals(that.agencyId)) return false;
            return callingPoints.equals(that.callingPoints);
        }

        @Override
        public int hashCode() {
            int result = agencyId.hashCode();
            result = 31 * result + callingPoints.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "AgencyCallingPoints{" +
                    "agencyId=" + agencyId +
                    ", callingPoints=" + callingPoints +
                    '}';
        }
    }

}
