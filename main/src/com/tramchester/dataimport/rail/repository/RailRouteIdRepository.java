package com.tramchester.dataimport.rail.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.ExtractAgencyCallingPointsFromLocationRecords;
import com.tramchester.dataimport.rail.ProvidesRailTimetableRecords;
import com.tramchester.dataimport.rail.RailRouteIDBuilder;
import com.tramchester.domain.Agency;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.Station;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/***
 * Used to create initial view of rail routes as part of rail data import, not expected to be used apart from this
 * NOTE:
 * is used as part of TransportData load so cannot depend (via dep injection) on that data, hence ExtractAgencyCallingPointsFromLocationRecords
 */
@LazySingleton
public class RailRouteIdRepository implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(RailRouteIdRepository.class);
    public static final int CACHED_ROUTES_SIZE = 8000; // approx 6000 rail routes currently Jan 2023

    private final ProvidesRailTimetableRecords providesRailTimetableRecords;
    private final RailRouteIDBuilder railRouteIDBuilder;
    private final boolean enabled;

    private final Map<IdFor<Agency>, List<AgencyCallingPointsWithRouteId>> routeIdsForAgency;

    // many repeated calls during rail data load, caching helps significantly with performance
    private final Cache<AgencyCallingPoints, RailRouteId> cachedIds;

    @Inject
    public RailRouteIdRepository(ProvidesRailTimetableRecords providesRailTimetableRecords,
                                 RailRouteIDBuilder railRouteIDBuilder, TramchesterConfig config,
                                 CacheMetrics cacheMetrics) {
        this.providesRailTimetableRecords = providesRailTimetableRecords;
        this.railRouteIDBuilder = railRouteIDBuilder;
        enabled = config.hasRailConfig();
        routeIdsForAgency = new HashMap<>();

        // only used during load, hence very short duration
        cachedIds = Caffeine.newBuilder().maximumSize(CACHED_ROUTES_SIZE).expireAfterAccess(2, TimeUnit.MINUTES).
                recordStats().build();

        cacheMetrics.register(this);

    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("Rail is not enabled");
            return;
        }

        logger.info("Starting");
        createRouteIdsFor(providesRailTimetableRecords);
        logger.info("Started");
    }

    @PreDestroy
    public void dispose() {
        logger.info("Stopping");
        routeIdsForAgency.clear();
        cachedIds.invalidateAll();
        logger.info("stopped");
    }

    private void createRouteIdsFor(ProvidesRailTimetableRecords providesRailTimetableRecords) {
        Set<AgencyCallingPoints> loadedCallingPoints = ExtractAgencyCallingPointsFromLocationRecords.loadCallingPoints(providesRailTimetableRecords);
        createRouteIdsFor(loadedCallingPoints);
        loadedCallingPoints.clear();
    }

    private void createRouteIdsFor(Set<AgencyCallingPoints> agencyCallingPoints) {
        Map<IdFor<Agency>, Set<AgencyCallingPoints>> callingPointsByAgency = new HashMap<>();

        logger.info("Create possible route ids for " + agencyCallingPoints.size() + " calling points combinations");

        // efficiency: group calling points by agency id
        agencyCallingPoints.forEach(points -> {
            IdFor<Agency> agencyId = points.getAgencyId();
            if (!callingPointsByAgency.containsKey(agencyId)) {
                callingPointsByAgency.put(agencyId, new HashSet<>());
            }
            callingPointsByAgency.get(agencyId).add(points);
        });

        List<Integer> totals = new ArrayList<>();
        callingPointsByAgency.forEach((agencyId, callingPoints) -> {
            List<AgencyCallingPointsWithRouteId> results = createSortedRoutesFor(agencyId, callingPoints);
            routeIdsForAgency.put(agencyId, results);
            totals.add(results.size());
        });

        int total = totals.stream().reduce(Integer::sum).orElse(0);

        logger.info("Created " + total + " ids from " + agencyCallingPoints.size() + " sets of calling points");

        callingPointsByAgency.clear();
    }

    private List<AgencyCallingPointsWithRouteId> createSortedRoutesFor(IdFor<Agency> agencyId, Set<AgencyCallingPoints> callingPoints) {

        // (begin,end) -> CallingPoints
        Map<StationIdPair, Set<AgencyCallingPoints>> callingPointsByBeginEnd = new HashMap<>();

        // group by (begin, end) of the route as a whole
        callingPoints.forEach(points -> {
            StationIdPair beginEnd = points.getBeginEnd();
            if (!callingPointsByBeginEnd.containsKey(beginEnd)) {
                callingPointsByBeginEnd.put(beginEnd, new HashSet<>());
            }
            callingPointsByBeginEnd.get(beginEnd).add(points);
        });

        // sorted by length
        List<AgencyCallingPointsWithRouteId> results = callingPointsByBeginEnd.entrySet().stream().
                flatMap(entry -> createRouteIdsFor(agencyId, entry.getKey(), entry.getValue()).stream()).
                sorted(Comparator.comparingInt(AgencyCallingPoints::numberCallingPoints)).
                collect(Collectors.toList());

        logger.info("Added " + results.size() + " entries for " + agencyId);

        return results;
    }

    private List<AgencyCallingPointsWithRouteId> createRouteIdsFor(IdFor<Agency> agencyId, StationIdPair beginEnd,
                                                                   Set<AgencyCallingPoints> callingPoints) {
        logger.debug("Create route ids for " + agencyId + " and " + beginEnd);

        if (callingPoints.size()==1) {
            return callingPoints.stream().map(filtered -> new AgencyCallingPointsWithRouteId(filtered, railRouteIDBuilder.getIdFor(filtered))).
                    collect(Collectors.toList());
        }

        // longest list first, so "sub-routes" are contains within larger routes
        List<AgencyCallingPoints> sortedBySize = callingPoints.stream().
                sorted(Comparator.comparingInt(AgencyCallingPoints::numberCallingPoints).reversed()).
                collect(Collectors.toList());

        List<AgencyCallingPoints> reduced = new ArrayList<>();
        for (AgencyCallingPoints agencyCallingPoints : sortedBySize) {
            if (reduced.stream().noneMatch(existing -> existing.contains(agencyCallingPoints))) {
                reduced.add(agencyCallingPoints);
            }
        }

        return reduced.stream().
                map(filtered -> new AgencyCallingPointsWithRouteId(filtered, railRouteIDBuilder.getIdFor(filtered))).
                collect(Collectors.toList());
    }

    /***
     * Only use during rail data import, use RouteRepository otherwise
     * @param agencyId agency id for this rail route
     * @param callingStations stations this route calls at
     * @return the MutableRoute to use
     */
    public RailRouteId getRouteIdFor(IdFor<Agency> agencyId, List<Station> callingStations) {
        // to a list, order matters
        List<IdFor<Station>> callingStationsIds = callingStations.stream().map(Station::getId).collect(Collectors.toList());
        AgencyCallingPoints agencyCallingPoints = new AgencyCallingPoints(agencyId, callingStationsIds);

        return cachedIds.get(agencyCallingPoints, unused -> getRouteId(agencyCallingPoints));
    }

    /** test support **/
    public RailRouteId getRouteIdUncached(IdFor<Agency> agencyId, List<Station> callingStations) {
        List<IdFor<Station>> callingStationsIds = callingStations.stream().map(Station::getId).collect(Collectors.toList());
        AgencyCallingPoints agencyCallingPoints = new AgencyCallingPoints(agencyId, callingStationsIds);
        return getRouteId(agencyCallingPoints);
    }

    public Set<RailRouteId> getForAgency(IdFor<Agency> agencyId) {
        return routeIdsForAgency.get(agencyId).stream().map(AgencyCallingPointsWithRouteId::getRouteId).collect(Collectors.toSet());
    }

    private RailRouteId getRouteId(AgencyCallingPoints agencyCallingPoints) {

        IdFor<Agency> agencyId = agencyCallingPoints.getAgencyId();

        if (!routeIdsForAgency.containsKey(agencyId)) {
            Set<IdFor<Agency>> ids = routeIdsForAgency.keySet();
            String msg = "Did not find agency "+ agencyId +" in routes, cache: " + ids;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        StationIdPair beginEnd = agencyCallingPoints.getBeginEnd();

        // existing routes and corresponding IDs
        Optional<AgencyCallingPointsWithRouteId> matching = routeIdsForAgency.get(agencyId).stream().
                filter(callingPoints -> callingPoints.getBeginEnd().equals(beginEnd)).
                filter(callingPoints -> callingPoints.contains(agencyCallingPoints)).
                max(Comparator.comparingInt(AgencyCallingPoints::numberCallingPoints));

        if (matching.isEmpty()) {
            throw new RuntimeException("Could not find a route id for " + agencyCallingPoints);
        }

        return matching.get().getRouteId();
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        List<Pair<String,CacheStats>> result = new ArrayList<>();
        result.add(Pair.of("routeIdCache", cachedIds.stats()));
        return result;
    }

    public List<AgencyCallingPointsWithRouteId> getCallingPointsFor(IdFor<Agency> agencyId) {
        return this.routeIdsForAgency.get(agencyId);
    }

    public static class AgencyCallingPoints {

        private final IdFor<Agency> agencyId;
        private final List<IdFor<Station>> callingPoints;
        private final StationIdPair beginEnd;

        private AgencyCallingPoints(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints, StationIdPair beginEnd) {
            this.agencyId = agencyId;
            this.callingPoints = callingPoints;
            this.beginEnd = beginEnd;
        }

        public AgencyCallingPoints(IdFor<Agency> agencyId, List<IdFor<Station>> callingPoints) {
            this(agencyId, callingPoints, findBeginAndEnd(callingPoints));
        }

        public AgencyCallingPoints(AgencyCallingPoints other) {
            this(other.agencyId, other.callingPoints, other.beginEnd);
        }

        public IdFor<Agency> getAgencyId() {
            return agencyId;
        }

        @NotNull
        private static StationIdPair findBeginAndEnd(final List<IdFor<Station>> callingPoints) {
            if (callingPoints.size()<2) {
                throw new RuntimeException("Not enough calling points for " + callingPoints);
            }
            final IdFor<Station> first = callingPoints.get(0);
            final IdFor<Station> last = callingPoints.get(callingPoints.size() - 1);
            return StationIdPair.of(first, last);
        }

        public List<IdFor<Station>> getCallingPoints() {
            return callingPoints;
        }

        public int numberCallingPoints() {
            return callingPoints.size();
        }

        public boolean contains(AgencyCallingPoints other) {
            if (!agencyId.equals(other.agencyId)) {
                throw new RuntimeException("AgencyId mismatch for " +this+ " and provided " + other);
            }

            // to be same route need same begin and end
            if (!beginEnd.equals(other.beginEnd)) {
                return false;
            }

            final int otherSize = other.numberCallingPoints();
            final int size = numberCallingPoints();

            // can't contain a bigger list
            if (otherSize>size) {
                return false;
            }

            // both lists are ordered by calling order
            int searchIndex = 0;

            for (int i = 0; i < size; i++) {
                if (callingPoints.get(i).equals(other.callingPoints.get(searchIndex))) {
                    searchIndex++;
                }
                if (searchIndex>=otherSize) {
                    break;
                }
            }
            return searchIndex == otherSize;

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AgencyCallingPoints that = (AgencyCallingPoints) o;
            return agencyId.equals(that.agencyId) && callingPoints.equals(that.callingPoints) && beginEnd.equals(that.beginEnd);
        }

        @Override
        public int hashCode() {
            return Objects.hash(agencyId, callingPoints, beginEnd);
        }

        @Override
        public String toString() {
            return "AgencyCallingPoints{" +
                    "agencyId=" + agencyId +
                    ", callingPoints=" + callingPoints +
                    ", beginEnd=" + beginEnd +
                    '}';
        }

        public StationIdPair getBeginEnd() {
            return beginEnd;
        }
    }

    public static class AgencyCallingPointsWithRouteId extends AgencyCallingPoints {

        private final RailRouteId routeId;

        public AgencyCallingPointsWithRouteId(AgencyCallingPoints other, RailRouteId routeId) {
            super(other);
            this.routeId = routeId;
        }

        public RailRouteId getRouteId() {
            return routeId;
        }

        @Override
        public String toString() {
            return "AgencyCallingPointsWithRouteId{" +
                    "routeId=" + routeId +
                    "} " + super.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            AgencyCallingPointsWithRouteId that = (AgencyCallingPointsWithRouteId) o;
            return routeId.equals(that.routeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), routeId);
        }
    }
}
