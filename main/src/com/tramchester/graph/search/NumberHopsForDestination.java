package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class NumberHopsForDestination {
    private static final Logger logger = LoggerFactory.getLogger(NumberHopsForDestination.class);

    private final RouteCostCalculator routeCostCalculator;
    private final StationRepository stationRepository;
    private final CompositeStationRepository compositeStationRepository;
    private final GraphDatabase database;

    @Inject
    public NumberHopsForDestination(RouteCostCalculator routeCostCalculator, StationRepository stationRepository, CompositeStationRepository compositeStationRepository, GraphDatabase database) {
        this.routeCostCalculator = routeCostCalculator;
        this.stationRepository = stationRepository;
        this.compositeStationRepository = compositeStationRepository;
        this.database = database;
    }

    public Map<IdFor<Station>, Long> calculateFor(Station dest) {
        Map<IdFor<Station>, Long> result = new HashMap<>();

        Set<TransportMode> modes = dest.getTransportModes();

        try (TimedTransaction timedTransaction = new TimedTransaction(database, logger, "find hops to " + dest.getId())) {
            modes.forEach(mode -> {
                Stream<Station> others = stationRepository.getStationsForModeStream(mode);

                Map<IdFor<Station>, Long> forMode = others.
                        filter(station -> !station.getId().equals(dest.getId())).
                        map(station -> getHopsBetween(timedTransaction.transaction(), station, dest)).
                        collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

                result.putAll(forMode);
            });
        }

        result.put(dest.getId(), 0L);

        return result;
    }

    private Pair<IdFor<Station>, Long> getHopsBetween(Transaction txn, Station start, Station end) {
        long hops = routeCostCalculator.getNumberHops(txn, start, end);
        return Pair.of(start.getId(), hops);
    }

    public Stream<Relationship> sortedByNearTo(Transaction txn, IdSet<Station> destinationStationIds,
                                               Set<SortsPositions.HasStationId<Relationship>> candidates) {
        Set<Station> destinations = destinationStationIds.stream().map(compositeStationRepository::getStationById).collect(Collectors.toSet());

        IdSet<Station> seen = new IdSet<>();
        Stream<SortsPositions.HasStationId<Relationship>> dedupped = candidates.stream().
                filter(hasId -> checkOrInsert(hasId, seen));
        seen.clear();

        Map<IdFor<Station>, Long> distances = dedupped.
                distinct().
                map(hasId -> Pair.of(hasId.getStationId(), findHopsBetween(txn, destinations, hasId.getStationId()))).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        return candidates.stream().
                sorted(Comparator.comparingLong(a -> distances.get(a.getStationId()))).
                map(SortsPositions.HasStationId::getContained);

    }

    private boolean checkOrInsert(SortsPositions.HasStationId<Relationship> hasId, IdSet<Station> seen) {
        IdFor<Station> id = hasId.getStationId();
        if (seen.contains(id)) {
            return false;
        } else {
            seen.add(hasId.getStationId());
            return true;
        }
    }

    private long findHopsBetween(Transaction txn, Set<Station> destinationStations, IdFor<Station> candidateId) {
        Station candidate = stationRepository.getStationById(candidateId);
        return destinationStations.stream().
            map(destination -> this.routeCostCalculator.getNumberHops(txn, destination, candidate)).
            min(Comparator.comparingLong(value -> value)).orElse(Long.MAX_VALUE);
    }


}
