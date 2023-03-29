package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;


@LazySingleton
public class RailRouteIDBuilder {

    private final Set<RailRouteId> alreadyAllocated;
    private final Map<AgencyCallingPoints, RailRouteId> callingPointsToId;

    @Inject
    public RailRouteIDBuilder() {
        alreadyAllocated = new HashSet<>();
        callingPointsToId = new HashMap<>();
    }

    public RailRouteId getIdFor(RailRouteIdRepository.AgencyCallingPoints agencyCallingPoints) {
        return getIdFor(agencyCallingPoints.getAgencyId(), agencyCallingPoints.getCallingPoints());
    }

    private RailRouteId getIdFor(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints) {
        // find unique route id based on the atoc code (aka agency) and list of calling points
        // have to include agency id since different agencies might serve same calling points

        AgencyCallingPoints agencyCallingPoints = new AgencyCallingPoints(agencyId, callingPoints);

        if (callingPointsToId.containsKey(agencyCallingPoints)) {
            return callingPointsToId.get(agencyCallingPoints);
        }

        int index = 1;
        RailRouteId railRouteId = RailRouteId.createId(agencyId, callingPoints, index);
        while (alreadyAllocated.contains(railRouteId)) {
            index = index + 1;
            railRouteId = RailRouteId.createId(agencyId, callingPoints, index);
        }

        alreadyAllocated.add(railRouteId);
        callingPointsToId.put(agencyCallingPoints, railRouteId);
        return railRouteId;

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
