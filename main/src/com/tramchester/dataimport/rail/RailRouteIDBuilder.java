package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class RailRouteIDBuilder {
    private final Map<IdFor<Route>, Integer> baseIdToIndex;
    private final Map<AgencyCallingPoints, IdFor<Route>> callingPointsToId;

    @Inject
    public RailRouteIDBuilder() {
        baseIdToIndex = new HashMap<>();
        callingPointsToId = new HashMap<>();
    }

    public IdFor<Route> getIdFor(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints) {
        // form unique route id based on the atoc code and list of calling points
        // have to include agency id since different agencies might serve same calling points

        AgencyCallingPoints agencyCallingPoints = new AgencyCallingPoints(agencyId, callingPoints);

        if (callingPointsToId.containsKey(agencyCallingPoints)) {
            return callingPointsToId.get(agencyCallingPoints);
        }

        // else new combination of calling stations
        String baseIdText = createBaseIdForRoute(agencyId.getGraphId(), callingPoints);
        IdFor<Route> baseId = StringIdFor.createId(baseIdText);

        // TODO baseId = Route.createId(agencyId, callingPoints);

        int newIndex = 1;
        if (baseIdToIndex.containsKey(baseId)) {
            newIndex = baseIdToIndex.get(baseId) + 1;
        }

        baseIdToIndex.put(baseId, newIndex);
        IdFor<Route> finalId = StringIdFor.withSuffix(baseId, ":"+ newIndex);
        callingPointsToId.put(agencyCallingPoints, finalId);
        return finalId;

    }

    private String createBaseIdForRoute(String atocCode, List<IdFor<Station>> callingPoints) {
        String firstName = callingPoints.get(0).forDTO();
        String lastName = callingPoints.get(callingPoints.size()-1).forDTO();
        return format("%s:%s=>%s", atocCode, firstName, lastName);
    }

    private static class AgencyCallingPoints {
        private final IdFor<Agency> agencyId;
        private final List<IdFor<Station>> callingPoints;

        private AgencyCallingPoints(IdFor<Agency> agencyId, List<IdFor<Station>> stationCallingPoints) {
            //this.callingPoints = stationCallingPoints.stream().map(Station::getId).collect(Collectors.toList());
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
