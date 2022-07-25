package com.tramchester.dataimport.rail.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.ProvidesRailTimetableRecords;
import com.tramchester.dataimport.rail.RailRouteIDBuilder;
import com.tramchester.dataimport.rail.records.BasicScheduleExtraDetails;
import com.tramchester.dataimport.rail.records.RailLocationRecord;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * Used to create initial view of rail routes as part of rail data import, not expected to be used apart from this
 */
@LazySingleton
public class RailRouteIdRepository {
    private static final Logger logger = LoggerFactory.getLogger(RailRouteIdRepository.class);

    private final ProvidesRailTimetableRecords providesRailTimetableRecords;
    private final RailRouteIDBuilder railRouteIDBuilder;
    private final Map<IdFor<Agency>, List<AgencyCallingPointsWithRouteId>> idMap;
    private final boolean enabled;

    @Inject
    public RailRouteIdRepository(ProvidesRailTimetableRecords providesRailTimetableRecords,
                                 RailRouteIDBuilder railRouteIDBuilder, TramchesterConfig config) {
        this.providesRailTimetableRecords = providesRailTimetableRecords;
        this.railRouteIDBuilder = railRouteIDBuilder;
        enabled = config.hasRailConfig();
        idMap = new HashMap<>();

    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("Rail is not enabled");
            return;
        }

        logger.info("Starting");
        createRailRoutesFor(providesRailTimetableRecords);
        logger.info("Started");
    }

    @PreDestroy
    public void stop() {
        idMap.clear();
    }

    private void createRailRoutesFor(ProvidesRailTimetableRecords providesRailTimetableRecords) {
        RouteExtractor routeExtractor = new RouteExtractor();
        Stream<RailTimetableRecord> records = providesRailTimetableRecords.load();
        records.forEach(routeExtractor::processRecord);

        createRoutesFor(routeExtractor.getPossibleRoutes());

        routeExtractor.clear();

    }

    private void createRoutesFor(Set<AgencyCallingPoints> possibleRoutes) {
        Map<IdFor<Agency>, Set<AgencyCallingPoints>> routesByAgency = new HashMap<>();

        // group by agency id
        possibleRoutes.forEach(possibleRoute -> {
            IdFor<Agency> agencyId = possibleRoute.getAgencyId();
            if (!routesByAgency.containsKey(agencyId)) {
                routesByAgency.put(agencyId, new HashSet<>());
            }
            routesByAgency.get(agencyId).add(possibleRoute);
        });

        routesByAgency.forEach((key, value) -> {
            List<AgencyCallingPointsWithRouteId> results = createRoutesFor(key, value);
            idMap.put(key, results);
        });

        routesByAgency.clear();
    }

    private List<AgencyCallingPointsWithRouteId> createRoutesFor(IdFor<Agency> agencyId, Set<AgencyCallingPoints> possibleRailRoutes) {
        logger.debug("Create route Ids for " + agencyId);

        Map<StationIdPair, Set<AgencyCallingPoints>> routesByBeginEnd = new HashMap<>();

        // group by (begin, end) of the route as a whole
        possibleRailRoutes.forEach(possibleRoute -> {
            StationIdPair beginEnd = possibleRoute.getBeginEnd();
            if (!routesByBeginEnd.containsKey(beginEnd)) {
                routesByBeginEnd.put(beginEnd, new HashSet<>());
            }
            routesByBeginEnd.get(beginEnd).add(possibleRoute);
        });

        List<AgencyCallingPointsWithRouteId> results = routesByBeginEnd.entrySet().stream().
                flatMap(entry -> createRoutesFor(agencyId, entry.getKey(), entry.getValue()).stream()).
                collect(Collectors.toList());

        logger.info("Added " + results.size() + " entries for " + agencyId);

        return results;
    }

    private List<AgencyCallingPointsWithRouteId> createRoutesFor(IdFor<Agency> agencyId, StationIdPair beginEnd,
                                                                   Set<AgencyCallingPoints> possibleRoutes) {
        logger.debug("Create route ids for " + agencyId + " and " + beginEnd);

        List<AgencyCallingPointsWithRouteId> results = new ArrayList<>();

        // longest list first, so "sub-routes" are contains within larger routes
        List<AgencyCallingPoints> sortedBySize = possibleRoutes.stream().
                sorted(Comparator.comparingInt(AgencyCallingPoints::numberCallingPoints).reversed()).
                collect(Collectors.toList());

        Map<List<IdFor<Station>>, IdFor<Route>> found = new HashMap<>();

        sortedBySize.forEach(agencyCallingPoints -> {
            List<IdFor<Station>> callingPoints = agencyCallingPoints.getCallingPoints();

            IdFor<Route> id;
            Optional<IdFor<Route>> alreadyMatched = matchingIdFor(callingPoints, found);
            if (alreadyMatched.isEmpty()) {
                id = railRouteIDBuilder.getIdFor(agencyId, callingPoints);
                found.put(callingPoints, id);

                // store the mapping from calling points and agency => Route Id to use
            } else {
                 id = alreadyMatched.get();
            }

            AgencyCallingPointsWithRouteId agencyCallingPointsWithId = new AgencyCallingPointsWithRouteId(agencyId, callingPoints, id);
            results.add(agencyCallingPointsWithId);


        });

        logger.debug("Added " + results.size() + " entries for " + beginEnd + " agency " + agencyId);

        return results;
    }

    private Optional<IdFor<Route>> matchingIdFor(List<IdFor<Station>> callingPoints, Map<List<IdFor<Station>>, IdFor<Route>> alreadyFound) {
        return alreadyFound.entrySet().stream().
                filter(found -> found.getKey().containsAll(callingPoints)).
                map(Map.Entry::getValue).
                findAny();
    }

    /***
     * Only use during rail data import, use RouteRepository otherwise
     * @param agencyId agency id for this rail route
     * @param callingStations stations this route calls at
     * @return the MutableRoute to use
     */
    public IdFor<Route> getRouteFor(IdFor<Agency> agencyId, List<Station> callingStations) {
        List<IdFor<Station>> callingStationsIds = callingStations.stream().map(Station::getId).collect(Collectors.toList());
        return getRouteIdForCallingPointsAndAgency(agencyId, callingStationsIds);
    }

    public IdFor<Route> getRouteIdForCallingPointsAndAgency(IdFor<Agency> agencyId, List<IdFor<Station>> callingStationsIds) {
        List<AgencyCallingPointsWithRouteId> forAgency = idMap.get(agencyId);

        List<AgencyCallingPointsWithRouteId> results = forAgency.stream().
                filter(withId -> withId.contains(callingStationsIds)).
                sorted(Comparator.comparingInt(AgencyCallingPoints::numberCallingPoints)).
                collect(Collectors.toList());

        if (results.isEmpty()) {
            // can happen where replacement services for one agency are under another agencies ID i.e. LT
            IdFor<Route> id = railRouteIDBuilder.getIdFor(agencyId, callingStationsIds);
            String msg = "No results for " + agencyId + " and " + callingStationsIds + " so create id " + id;
            logger.error(msg);
            return id;
            //throw new RuntimeException(msg);
        }

        AgencyCallingPointsWithRouteId lowestSizeMatch = results.get(0);
        if (lowestSizeMatch.numberCallingPoints() != callingStationsIds.size()) {
            logger.warn("Mismatch on number of calling points for " + callingStationsIds + " and results " + lowestSizeMatch);
        }

        return lowestSizeMatch.getRouteId();

    }

    private static class RouteExtractor {

        private String currentAtocCode;
        private final List<RailLocationRecord> locations;
        private final Set<AgencyCallingPoints> possibleRailRoutes;

        private RouteExtractor() {
            currentAtocCode = "";
            possibleRailRoutes = new HashSet<>();
            locations = new ArrayList<>();
        }

        private void processRecord(RailTimetableRecord record) {
            switch (record.getRecordType()) {
                case BasicScheduleExtra -> seenBegin(record);
                case TerminatingLocation -> seenEnd(record);
                case OriginLocation, IntermediateLocation -> seenLocation(record);
            }
        }

        private void seenEnd(RailTimetableRecord record) {
            RailLocationRecord locationRecord = (RailLocationRecord) record;
            locations.add(locationRecord);
            createAgencyCallingPoints();
            currentAtocCode = "";
            locations.clear();
        }

        private void createAgencyCallingPoints() {
            String atocCode = currentAtocCode;
            List<IdFor<Station>> callingPoints = locations.stream().
                    filter(RailLocationRecord::doesStop).
                    map(RailLocationRecord::getTiplocCode).
                    map(Station::createId).
                    collect(Collectors.toList());

            IdFor<Agency> agencyId = Agency.createId(atocCode);
            possibleRailRoutes.add(new AgencyCallingPoints(agencyId, callingPoints));

        }

        private void seenLocation(RailTimetableRecord record) {
            RailLocationRecord locationRecord = (RailLocationRecord) record;
            locations.add(locationRecord);
        }

        private void seenBegin(RailTimetableRecord record) {
            if (!currentAtocCode.isEmpty()) {
                throw new RuntimeException("Unexpected state, was still processing for " + currentAtocCode + " at " + record);
            }

            BasicScheduleExtraDetails extraDetails = (BasicScheduleExtraDetails) record;
            currentAtocCode = extraDetails.getAtocCode();
        }

        public Set<AgencyCallingPoints> getPossibleRoutes() {
            return possibleRailRoutes;
        }

        public void clear() {
            possibleRailRoutes.clear();
        }
    }

    private static class AgencyCallingPoints {

        private final IdFor<Agency> agencyId;
        private final List<IdFor<Station>> callingPoints;

        public AgencyCallingPoints(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints) {

            this.agencyId = agencyId;
            this.callingPoints = callingPoints;
        }

        public IdFor<Agency> getAgencyId() {
            return agencyId;
        }

        public StationIdPair getBeginEnd() {
            return getStationIdPair(callingPoints);
        }

        @NotNull
        private static StationIdPair getStationIdPair(List<IdFor<Station>> callingPoints) {
            if (callingPoints.size()<2) {
                throw new RuntimeException("Not enough calling points for " + callingPoints);
            }
            IdFor<Station> first = callingPoints.get(0);
            IdFor<Station> second = callingPoints.get(callingPoints.size() - 1);
            return StationIdPair.of(first, second);
        }

        public List<IdFor<Station>> getCallingPoints() {
            return callingPoints;
        }

        public int numberCallingPoints() {
            return callingPoints.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AgencyCallingPoints that = (AgencyCallingPoints) o;
            return agencyId.equals(that.agencyId) && callingPoints.equals(that.callingPoints);
        }

        @Override
        public int hashCode() {
            return Objects.hash(agencyId, callingPoints);
        }

        @Override
        public String toString() {
            return "AgencyCallingPoints{" +
                    "agencyId=" + agencyId +
                    ", callingPoints=" + callingPoints +
                    '}';
        }

        public boolean contains(List<IdFor<Station>> callingStationsIds) {
            if (!callingPoints.containsAll(callingStationsIds)) {
                return false;
            }

            StationIdPair otherPair = getStationIdPair(callingStationsIds);

            if (!otherPair.equals(getBeginEnd())) {
                return false;
            }

            return true;
        }
    }

    private static class AgencyCallingPointsWithRouteId extends AgencyCallingPoints {

        private final IdFor<Route> routeId;

        public AgencyCallingPointsWithRouteId(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints, IdFor<Route> routeId) {
            super(agencyId, callingPoints);
            this.routeId = routeId;
        }

        public IdFor<Route> getRouteId() {
            return routeId;
        }
    }
}
