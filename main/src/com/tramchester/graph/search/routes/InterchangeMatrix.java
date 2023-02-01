package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.ImmutableBitSet;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.graph.search.routes.RouteCostMatrix.MAX_DEPTH;
import static java.lang.String.format;

@LazySingleton
public class InterchangeMatrix {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeMatrix.class);

    private final InterchangeRepository interchangeRepository;
    private final InterchangeIndex interchangeIndex;
    private final StationAvailabilityRepository availabilityRepository;

    private final List<IndexedBitSet> overlaps;
    private final int numberOfInterchanges;

    @Inject
    public InterchangeMatrix(InterchangeRepository interchangeRepository, StationAvailabilityRepository availabilityRepository) {
        this.interchangeRepository = interchangeRepository;
        this.interchangeIndex = new InterchangeIndex(interchangeRepository);
        this.numberOfInterchanges = interchangeRepository.size();
        this.availabilityRepository = availabilityRepository;
        this.overlaps = new ArrayList<>();
    }

    @PostConstruct
    private void start() {
        logger.info("starting");
        interchangeIndex.start();
        populateMatrix();
        logger.info("started");
    }

    private void populateMatrix() {
        for (int i = 0; i < MAX_DEPTH; i++) {
            overlaps.add(new IndexedBitSet(numberOfInterchanges, numberOfInterchanges));
        }

        // initial direct connections
        IndexedBitSet forDepth = overlaps.get(0);
        interchangeRepository.getAllInterchanges().forEach(interchangeStation -> {
            int index = interchangeIndex.get(interchangeStation);
            BitSet row = getPickupsFor(interchangeStation);
            forDepth.insert(index, row);
        });

        // indirect
        for (int i = 0; i < MAX_DEPTH-1; i++) {
            propogate(i);
        }
    }

    private void propogate(int currentDegree) {
        final int nextDegree = currentDegree + 1;

        final IndexedBitSet currentMatrix = overlaps.get(currentDegree);
        final IndexedBitSet newMatrix = overlaps.get(nextDegree);

        for (int interchange = 0; interchange < numberOfInterchanges; interchange++) {
            final BitSet resultForInterchange = new BitSet(numberOfInterchanges);
            ImmutableBitSet currentConnectionsForInterchange = currentMatrix.getBitSetForRow(interchange);

            currentConnectionsForInterchange.getBitIndexes().forEach(connectedInterchange -> {
                final ImmutableBitSet otherConnections = currentMatrix.getBitSetForRow(connectedInterchange);
                otherConnections.applyOrTo(resultForInterchange);
            });

            final ImmutableBitSet allExistingConnectionsForRoute = getExistingBitSetsFor(interchange, currentDegree);
            //
            allExistingConnectionsForRoute.applyAndNotTo(resultForInterchange);  // don't include any current connections for this route
            //
            newMatrix.insert(interchange, resultForInterchange);
        }
    }

    private ImmutableBitSet getExistingBitSetsFor(int interchange, int startingDegree) {
        final IndexedBitSet connectionsAtAllDepths = new IndexedBitSet(1, numberOfInterchanges);

        for (int degree = startingDegree; degree > 0; degree--) {
            IndexedBitSet allConnectionsAtDegree = overlaps.get(degree);
            ImmutableBitSet existingConnectionsAtDepth = allConnectionsAtDegree.getBitSetForRow(interchange);
            connectionsAtAllDepths.or(existingConnectionsAtDepth);
        }

        return connectionsAtAllDepths.createImmutable();
    }

    private BitSet getPickupsFor(InterchangeStation destination) {
        Set<Route> dropOffs = destination.getDropoffRoutes();
        BitSet result = new BitSet(numberOfInterchanges);

        interchangeRepository.getAllInterchanges().stream().
                filter(interchangeStation -> !SetUtils.intersection(dropOffs, interchangeStation.getPickupRoutes()).isEmpty()).
                map(interchangeIndex::get).
                forEach(result::set);

        return result;
    }

    public int getDegree(IdFor<Station> begin, IdFor<Station> end, TramDate date, TimeRange timeRange, Set<TransportMode> modes) {
        guardForInterchange(begin);
        guardForInterchange(end);

        // TODO Should be able to create a date/time overlap mask here
        IndexedBitSet mask = createOverlapMatrixFor(date, timeRange, modes);

        int indexBegin = interchangeIndex.get(begin);
        int indexEnd = interchangeIndex.get(end);

        for (int depth = 0; depth < overlaps.size(); depth++) {
            IndexedBitSet overlap = overlaps.get(depth);
            final IndexedBitSet withDateApplied = overlap.and(mask);
            if (withDateApplied.isSet(indexBegin, indexEnd)) {
                return depth;
            }
        }

        return  -1;
    }

    private void guardForInterchange(IdFor<Station> begin) {
        if (!interchangeRepository.isInterchange(begin)) {
            String message = begin + " is not an interchange";
            logger.error(message);
            throw new RuntimeException(message);
        }
    }

    public IndexedBitSet createOverlapMatrixFor(TramDate date, TimeRange timeRange, Set<TransportMode> requestedModes) {
        final Set<Integer> availableOnDate = new HashSet<>();
        for (int index = 0; index < numberOfInterchanges; index++) {
            final Station station = interchangeIndex.get(index).getStation();
            if (availabilityRepository.isAvailable(station, date, timeRange, requestedModes)) {
                availableOnDate.add(index);
            }
        }

        IndexedBitSet result = IndexedBitSet.Square(numberOfInterchanges);
        for (int firstRouteIndex = 0; firstRouteIndex < numberOfInterchanges; firstRouteIndex++) {
            BitSet row = new BitSet(numberOfInterchanges);
            if (availableOnDate.contains(firstRouteIndex)) {
                for (int secondInterchangeIndex = 0; secondInterchangeIndex < numberOfInterchanges; secondInterchangeIndex++) {
                    if (availableOnDate.contains(secondInterchangeIndex)) {
                        row.set(secondInterchangeIndex);
                    }
                }
            }
            result.insert(firstRouteIndex, row);
        }
        availableOnDate.clear();
        logger.info(format("created overlap matrix for %s and modes %s with %s entries", date, requestedModes, result.numberOfBitsSet()));
        return result;
    }

    private static class InterchangeIndex {
        private final InterchangeRepository interchangeRepository;
        private final Map<IdFor<Station>, Integer> map;
        private final Map<Integer, InterchangeStation> reverseMap;

        public InterchangeIndex(InterchangeRepository interchangeRepository) {
            this.interchangeRepository = interchangeRepository;
            map = new HashMap<>();
            reverseMap = new HashMap<>();
        }

        public void start() {
            List<InterchangeStation> interchanges = new ArrayList<>(interchangeRepository.getAllInterchanges());

            for (int i = 0; i < interchanges.size(); i++) {
                InterchangeStation interchangeStation = interchanges.get(i);
                map.put(interchangeStation.getStationId(), i);
                reverseMap.put(i, interchangeStation);
            }

        }

        public int get(InterchangeStation interchangeStation) {
            return map.get(interchangeStation.getStationId());
        }

        public int get(IdFor<Station> stationId) {
            return map.get(stationId);
        }

        public InterchangeStation get(int index) {
            return reverseMap.get(index);
        }
    }

}
