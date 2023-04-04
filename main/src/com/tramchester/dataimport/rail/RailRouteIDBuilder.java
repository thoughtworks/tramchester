package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
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

    public List<RailRouteIdRepository.AgencyCallingPointsWithRouteId> createSortedRoutesFor(IdFor<Agency> agencyId, Set<RailRouteIdRepository.AgencyCallingPoints> callingPoints) {

        // (begin,end) -> CallingPoints
        Map<StationIdPair, Set<RailRouteIdRepository.AgencyCallingPoints>> callingPointsByBeginEnd = new HashMap<>();

        // group by (begin, end) of the route as a whole
        callingPoints.forEach(points -> {
            StationIdPair beginEnd = points.getBeginEnd();
            if (!callingPointsByBeginEnd.containsKey(beginEnd)) {
                callingPointsByBeginEnd.put(beginEnd, new HashSet<>());
            }
            callingPointsByBeginEnd.get(beginEnd).add(points);
        });

        // sorted by length
        List<RailRouteIdRepository.AgencyCallingPointsWithRouteId> results = callingPointsByBeginEnd.entrySet().stream().
                flatMap(entry -> createRouteIdsFor(agencyId, entry.getKey(), entry.getValue()).stream()).
                sorted(Comparator.comparingInt(RailRouteIdRepository.AgencyCallingPoints::numberCallingPoints)).
                collect(Collectors.toList());

        logger.info("Added " + results.size() + " entries for " + agencyId);

        return results;
    }

    private List<RailRouteIdRepository.AgencyCallingPointsWithRouteId> createRouteIdsFor(IdFor<Agency> agencyId, StationIdPair beginEnd,
                                                                                         Set<RailRouteIdRepository.AgencyCallingPoints> callingPoints) {

        logger.debug("Create route ids for " + agencyId + " and " + beginEnd);

        if (callingPoints.size()==1) {
            return callingPoints.stream().
                    map(filtered -> new RailRouteIdRepository.AgencyCallingPointsWithRouteId(filtered, getIdFor(filtered))).
                    collect(Collectors.toList());
        }

        // longest list first, so "sub-routes" are contains within larger routes
        List<RailRouteIdRepository.AgencyCallingPoints> sortedBySize = callingPoints.stream().
                sorted(Comparator.comparingInt(RailRouteIdRepository.AgencyCallingPoints::numberCallingPoints).reversed()).
                collect(Collectors.toList());

        List<RailRouteIdRepository.AgencyCallingPoints> reduced = new ArrayList<>();
        for (RailRouteIdRepository.AgencyCallingPoints agencyCallingPoints : sortedBySize) {
            if (reduced.stream().noneMatch(existing -> existing.contains(agencyCallingPoints))) {
                reduced.add(agencyCallingPoints);
            }
        }

        List<RailRouteIdRepository.AgencyCallingPointsWithRouteId> callingPointsWithRouteIds = reduced.stream().
                map(agencyCallingPoints -> new RailRouteIdRepository.AgencyCallingPointsWithRouteId(agencyCallingPoints,
                        getIdFor(agencyCallingPoints))).
                collect(Collectors.toList());

        logger.info(format("Created %s rail route ids for %s %s", callingPointsWithRouteIds.size(), agencyId, beginEnd));

        return callingPointsWithRouteIds;
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
