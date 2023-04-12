package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.repository.RailRouteCallingPoints;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;


@LazySingleton
public class RailRouteIDBuilder {
    private static final Logger logger = LoggerFactory.getLogger(RailRouteIDBuilder.class);

    private final Set<RailRouteId> alreadyAllocated;
    private final Map<AgencyCallingPoints, RailRouteId> callingPointsToId;

    @Inject
    public RailRouteIDBuilder() {
        alreadyAllocated = new HashSet<>();
        callingPointsToId = new HashMap<>();
    }

    /***
     * create RailRouteId for each set of callings points
     * @param agencyId the agency for the callings points
     * @param railRoutes the set of all collections calling points, comprising all the "routes" for the agency
     * @return callings points mapped to RailRouteId
     */
    public Set<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> getRouteIdsFor(IdFor<Agency> agencyId, Set<RailRouteCallingPoints> railRoutes) {

        // (begin,end) -> CallingPoints
        Map<StationIdPair, Set<RailRouteCallingPoints>> routesGroupedByBeginEnd = new HashMap<>();

        // group by (begin, end) of the route as a whole
        railRoutes.forEach(route -> {
            StationIdPair beginEnd = route.getBeginEnd();
            if (!routesGroupedByBeginEnd.containsKey(beginEnd)) {
                routesGroupedByBeginEnd.put(beginEnd, new HashSet<>());
            }
            routesGroupedByBeginEnd.get(beginEnd).add(route);
        });

        Set<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> results = routesGroupedByBeginEnd.entrySet().stream().
                flatMap(grouped -> createRouteIdsFor(agencyId, grouped.getKey(), grouped.getValue()).stream()).
                collect(Collectors.toSet());

        logger.info("Added " + results.size() + " entries for " + agencyId);

        return results;
    }

    /***
     * Takes all railRoutes for a given agency and same Being and End and creates RailRouteId for those
     * @param agencyId the agency for the routes
     * @param beginEnd the shared begin and end for every route
     * @param railRoutes the rail 'routes' operating between being and end
     * @return routes decorated with the RailRouteId
     */
    private List<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> createRouteIdsFor(IdFor<Agency> agencyId, StationIdPair beginEnd,
                                                                                            Set<RailRouteCallingPoints> railRoutes) {

        logger.debug("Create route ids for " + agencyId + " and " + beginEnd);

        if (railRoutes.size()==1) {
            return railRoutes.stream().
                    map(railRoute -> new RailRouteIdRepository.RailRouteCallingPointsWithRouteId(railRoute, getIdFor(railRoute))).
                    collect(Collectors.toList());
        }

        // longest list first, so "sub-routes" are contains within larger routes
        List<RailRouteCallingPoints> routesBySize = railRoutes.stream().
                sorted(RailRouteCallingPoints::compareTo).
                collect(Collectors.toList());

        // longestRoutes are created by 'consuming' shorter routes that are subsets of the calling points for the
        // longer routes
        List<RailRouteCallingPoints> longestRoutes = new ArrayList<>();
        for (RailRouteCallingPoints agencyCallingPoints : routesBySize) {
            if (longestRoutes.stream().noneMatch(existing -> existing.contains(agencyCallingPoints))) {
                longestRoutes.add(agencyCallingPoints);
            }
        }

        // now gen Id's (only) for those longest routes
        List<RailRouteIdRepository.RailRouteCallingPointsWithRouteId> railRoutesWithIds = longestRoutes.stream().
                map(railRoute -> new RailRouteIdRepository.RailRouteCallingPointsWithRouteId(railRoute, getIdFor(railRoute))).
                collect(Collectors.toList());

        logger.info(format("Created %s rail route ids for %s %s", railRoutesWithIds.size(), agencyId, beginEnd));

        return railRoutesWithIds;
    }

    public RailRouteId getIdFor(RailRouteCallingPoints agencyCallingPoints) {
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
