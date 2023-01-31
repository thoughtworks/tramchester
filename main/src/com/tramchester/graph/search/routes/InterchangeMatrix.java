package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.collections.ImmutableBitSet;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.InterchangeRepository;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static com.tramchester.graph.search.routes.RouteCostMatrix.MAX_DEPTH;

@LazySingleton
public class InterchangeMatrix {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeMatrix.class);

    private final InterchangeRepository interchangeRepository;
    private final InterchangeIndex interchangeIndex;
    private final List<IndexedBitSet> overlaps;
    private final int numberOfInterchanges;

    @Inject
    public InterchangeMatrix(InterchangeRepository interchangeRepository) {
        this.interchangeRepository = interchangeRepository;
        this.interchangeIndex = new InterchangeIndex(interchangeRepository);
        this.numberOfInterchanges = interchangeRepository.size();
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

    public int getDegree(IdFor<Station> begin, IdFor<Station> end) {
        guardForInterchange(begin);
        guardForInterchange(end);

        // TODO Should be able to create a date/time overlap mask here

        int indexBegin = interchangeIndex.get(begin);
        int indexEnd = interchangeIndex.get(end);

        for (int depth = 0; depth < overlaps.size(); depth++) {
            if (overlaps.get(depth).isSet(indexBegin, indexEnd)) {
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

    private static class InterchangeIndex {
        private final InterchangeRepository interchangeRepository;
        private final Map<IdFor<Station>, Integer> map;

        public InterchangeIndex(InterchangeRepository interchangeRepository) {
            this.interchangeRepository = interchangeRepository;
            map = new HashMap<>();
        }

        public void start() {
            List<IdFor<Station>> ids = interchangeRepository.getAllInterchanges().stream().
                    map(InterchangeStation::getStationId).
                    collect(Collectors.toList());

            for (int i = 0; i < ids.size(); i++) {
                map.put(ids.get(i), i);
            }

        }

        public int get(InterchangeStation interchangeStation) {
            return map.get(interchangeStation.getStationId());
        }

        public int get(IdFor<Station> stationId) {
            return map.get(stationId);
        }
    }

}
