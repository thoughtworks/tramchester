package com.tramchester.unit.graph;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.*;
import com.tramchester.graph.search.diagnostics.ReasonCode;
import com.tramchester.graph.search.diagnostics.ServiceReason;
import com.tramchester.graph.search.diagnostics.ServiceReasons;
import com.tramchester.graph.search.diagnostics.HowIGotHere;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.repository.TripRepository;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;
import static com.tramchester.graph.graphbuild.GraphLabel.*;
import static com.tramchester.graph.search.diagnostics.ReasonCode.*;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TramRouteEvaluatorTest extends EasyMockSupport {

    private LocationSet destinationStations;
    private ServiceHeuristics serviceHeuristics;
    private NodeContentsRepository contentsRepository;
    private Path path;
    private HowIGotHere howIGotHere;
    private Node node;
    private ServiceReasons reasons;
    private TramchesterConfig config;
    private SortsPositions sortsPositions;
    private PreviousVisits previousSuccessfulVisit;
    private LatLong latLongHint;
    private Long destinationNodeId;
    private Relationship lastRelationship;
    private TripRepository tripRepository;
    private long startNodeId;
    private LowestCostSeen lowestCostSeen;
    private ProvidesNow providesNow;
    private LowestCostsForDestRoutes lowestCostsForRoutes;
    private Duration maxInitialWait;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        MutableStation forTest = new MutableStation(StringIdFor.createId("destinationStationId"),
                NaptanArea.createId("area"), "name",
                new LatLong(1, 1), new GridPosition(1000,1000), DataSourceID.tfgm);

        destinationStations = LocationSet.singleton(forTest);

        forTest.addRouteDropOff(TestEnv.getTramTestRoute());
        forTest.addRouteDropOff(TestEnv.getTramTestRoute());
        forTest.addRoutePickUp(TestEnv.getTramTestRoute());
        forTest.addRoutePickUp(TestEnv.getTramTestRoute());

        lowestCostSeen = createMock(LowestCostSeen.class);
        previousSuccessfulVisit = createMock(PreviousVisits.class);
        tripRepository = createMock(TripRepository.class);

        contentsRepository = createMock(NodeContentsRepository.class);

        providesNow = createMock(ProvidesNow.class);

        config = new TestConfig() {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return Collections.singletonList(new TFGMGTFSSourceTestConfig("data/tram",
                       GTFSTransportationType.tram, TransportMode.Tram, IdSet.emptySet(),
                        Collections.emptySet(), Collections.emptyList(), Duration.ofMinutes(13)));
            }
        };

        maxInitialWait = config.getInitialMaxWaitFor(DataSourceID.tfgm);

        latLongHint = TramStations.ManAirport.getLatLong();
        destinationNodeId = 88L;
        startNodeId = 128L;

        long maxNumberOfJourneys = 2;
        JourneyRequest journeyRequest = new JourneyRequest(
                TestEnv.nextSaturday(), TramTime.of(8,15), false,
                3, Duration.ofMinutes(config.getMaxJourneyDuration()), maxNumberOfJourneys, TramsOnly);
        reasons = new ServiceReasons(journeyRequest, TramTime.of(8,15), providesNow);

        serviceHeuristics = createMock(ServiceHeuristics.class);
        sortsPositions = createMock(SortsPositions.class);
        path = createMock(Path.class);
        node = createMock(Node.class);
        lastRelationship = createMock(Relationship.class);
        //routeToRouteCosts = createMock(RouteToRouteCosts.class);
        lowestCostsForRoutes = createMock(LowestCostsForDestRoutes.class);

        howIGotHere = HowIGotHere.forTest(42L, 24L);

        EasyMock.expect(node.getId()).andStubReturn(42L);
        EasyMock.expect(node.getAllProperties()).andStubReturn(new HashMap<>());

        EasyMock.expect(lastRelationship.getId()).andStubReturn(24L);

        EasyMock.expect(path.endNode()).andStubReturn(node);
        EasyMock.expect(path.lastRelationship()).andStubReturn(lastRelationship);

    }

    @NotNull
    private NotStartedState getNotStartedState() {

        RegistersStates registersStates = new RegistersStates();
        TraversalStateFactory traversalStateFactory = new TraversalStateFactory(registersStates, contentsRepository, config);

        final TraversalOps traversalOps = new TraversalOps(contentsRepository, tripRepository, sortsPositions, destinationStations,
                latLongHint, lowestCostsForRoutes, TestEnv.testDay());
        return new NotStartedState(traversalOps, traversalStateFactory, TramsOnly);
    }

    @NotNull
    private TramRouteEvaluator getEvaluatorForTest(long destinationNodeId) {
        Set<Long> destinationNodeIds = new HashSet<>();
        destinationNodeIds.add(destinationNodeId);
        Instant begin = Instant.now();
        // empty means all

        return new TramRouteEvaluator(serviceHeuristics, destinationNodeIds, contentsRepository,
                reasons, previousSuccessfulVisit, lowestCostSeen, config, startNodeId, begin, providesNow, TramsOnly, maxInitialWait);
    }

    @Test
    void shouldHaveReasonsThatInclude() {
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, Arrived.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, ServiceDateOk.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, ServiceTimeOk.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, NumChangesOK.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, TimeOk.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, HourOk.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, Reachable.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, ReachableNoCheck.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, DurationOk.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, WalkOk.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, Continue.getEvaluation());
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, StationOpen.getEvaluation());

    }

    @Test
    void shouldHaveReasonsThatExclude() {
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, HigherCost.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, ReturnedToStart.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, PathTooLong.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TooManyChanges.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, NotReachable.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, NotOnQueryDate.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, TookTooLong.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, ServiceNotRunningAtTime.getEvaluation());

        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, NotAtHour.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, AlreadyDeparted.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, DoesNotOperateOnTime.getEvaluation());
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, StationClosed.getEvaluation());

    }

    @Test
    void shouldMatchDestinationLowerCost() {
        long destinationNodeId = 42;
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);

        BranchState<JourneyState> branchState = new TestBranchState();

        JourneyState journeyState = createMock(JourneyState.class);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(Duration.ofMinutes(42));
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(7);

        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);

        EasyMock.expect(lowestCostSeen.isLower(journeyState)).andReturn(true);
        EasyMock.expect(journeyState.getTraversalStateName()).andStubReturn("aName");

        lowestCostSeen.setLowestCost(journeyState);
        EasyMock.expectLastCall();


        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.Arrived, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldMatchDestinationButHigherCost() {
        long destinationNodeId = 42;
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);

        BranchState<JourneyState> branchState = new TestBranchState();

        JourneyState journeyState = createMock(JourneyState.class);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(Duration.ofMinutes(100));
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(10);
        EasyMock.expect(journeyState.getTraversalStateName()).andStubReturn("aName");

        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);

        EasyMock.expect(lowestCostSeen.isLower(journeyState)).andReturn(false);
        EasyMock.expect(lowestCostSeen.getLowestNumChanges()).andReturn(5);

        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.HigherCost, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldMatchDestinationButHigherCostButLessHops() {
        long destinationNodeId = 42;
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);

        BranchState<JourneyState> branchState = new TestBranchState();

        JourneyState journeyState = createMock(JourneyState.class);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(Duration.ofMinutes(100));
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(2);
        EasyMock.expect(journeyState.getTraversalStateName()).andStubReturn("aName");

        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        EasyMock.expect(lowestCostSeen.getLowestNumChanges()).andReturn(5);
        EasyMock.expect(lowestCostSeen.isLower(journeyState)).andReturn(false);

        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.Arrived, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldUseCachedResultForMultipleJourneyExclude() {
        long destinationNodeId = 42;
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);

        BranchState<JourneyState> state = new TestBranchState();

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        final JourneyState journeyState = new JourneyState(time, traversalState);
        state.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.NotAtHour);

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfPreviousVisit() {
        long destinationNodeId = 42;
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);

        BranchState<JourneyState> state = new TestBranchState();

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        final JourneyState journeyState = new JourneyState(time, traversalState);
        state.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.DoesNotOperateOnTime);

        replayAll();
        Evaluation result = evaluator.evaluate(path, state);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldPruneIfTooLong() {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(200);

        BranchState<JourneyState> branchState = new TestBranchState();
        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);
        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);

        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(path.length()).andReturn(201);

        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.PathTooLong, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotRunningToday() {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(DurationOk, howIGotHere));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);
        BranchState<JourneyState> branchState = new TestBranchState();

        final EnumSet<GraphLabel> labels = EnumSet.of(SERVICE);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        TramTime time = TramTime.of(8, 15);

        EasyMock.expect(serviceHeuristics.checkServiceDateAndTime(node, howIGotHere, reasons, time, 13)).
                andReturn(ServiceReason.DoesNotRunOnQueryDate(howIGotHere, StringIdFor.createId("nodeServiceId")));

        NotStartedState traversalState = getNotStartedState();
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);

        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.NotOnQueryDate, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();

        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfUnreachableNode() throws TramchesterException {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION, TRAM);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(DurationOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkModes(labels, EnumSet.of(TRAM), howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.TransportModeOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Tram, node, true);
        branchState.setState(journeyState);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, 0, howIGotHere, reasons, time)).
                andReturn(ServiceReason.StationNotReachable(howIGotHere, ReasonCode.NotReachable));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);

        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.NotReachable, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfStationIsClosed() throws TramchesterException {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION, TRAM);
        Set<GraphLabel> requestedLabels = EnumSet.of(TRAM);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(DurationOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkModes(labels, requestedLabels, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.TransportModeOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Tram, node, true);
        branchState.setState(journeyState);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, 0, howIGotHere, reasons, time)).
                andReturn(ServiceReason.IsValid(ReasonCode.Reachable, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(ServiceReason.StationClosed(howIGotHere, Shudehill.fake()));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.StationClosed, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfNotOnTramNode() throws TramchesterException {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION, TRAM);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(DurationOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkModes(labels, EnumSet.of(TRAM), howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.TransportModeOk, howIGotHere));

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        JourneyState journeyState = new JourneyState(time, traversalState);
        journeyState.board(TransportMode.Bus, node, true);
        branchState.setState(journeyState);

        EasyMock.expect(lowestCostSeen.everArrived()).andStubReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(lastRelationship.isType(WALKS_TO_STATION)).andReturn(true);

        EasyMock.expect(serviceHeuristics.canReachDestination(node, 0, howIGotHere, reasons, time)).
                andReturn(ServiceReason.IsValid(ReasonCode.Reachable, howIGotHere));
        EasyMock.expect(serviceHeuristics.lowerCostIncludingInterchange(node, Duration.ZERO, lowestCostSeen, howIGotHere, reasons)).andReturn(
                ServiceReason.IsValid(ReasonCode.Reachable, howIGotHere));

        EasyMock.expect(serviceHeuristics.checkStationOpen(node, howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(StationOpen, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(WalkOk, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfWalking() {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(DurationOk, howIGotHere));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        final EnumSet<GraphLabel> labels = EnumSet.of(QUERY_NODE);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(lastRelationship.isType(WALKS_TO_STATION)).andReturn(true);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(WalkOk, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfTakingTooLong() {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        TramTime time = TramTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO,howIGotHere, reasons)).
                andReturn(ServiceReason.TookTooLong(time, howIGotHere));

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        NotStartedState traversalState = getNotStartedState();
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.TookTooLong, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfTimedOut() {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        final JourneyState journeyState = createMock(JourneyState.class);
        final Duration costSoFar = Duration.ofMinutes(100);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(costSoFar);
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(10);
        EasyMock.expect(journeyState.getTraversalStateName()).andStubReturn("aName");
        EasyMock.expect(path.length()).andReturn(42);

        branchState.setState(journeyState);
        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).
                andReturn(ReasonCode.PreviousCacheMiss);

        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(true);
        EasyMock.expect(lowestCostSeen.getLowestDuration()).andReturn(costSoFar.plusMinutes(10));

        Instant instant = Instant.now().plusMillis(config.getCalcTimeoutMillis()+1);
        EasyMock.expect(providesNow.getInstant()).andReturn(instant);

        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.TimedOut, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfAlreadyTooLong() {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        final JourneyState journeyState = createMock(JourneyState.class);
        EasyMock.expect(journeyState.getTotalDurationSoFar()).andReturn(Duration.ofMinutes(100));
        EasyMock.expect(journeyState.getNumberChanges()).andReturn(10);
        EasyMock.expect(journeyState.getTraversalStateName()).andStubReturn("aName");

        branchState.setState(journeyState);
        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).
                andReturn(ReasonCode.PreviousCacheMiss);

        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(true);
        EasyMock.expect(lowestCostSeen.getLowestDuration()).andReturn(Duration.ofMinutes(10));

        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.HigherCost, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfOverLimitOnChanges() {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(path.length()).andReturn(50);
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);

        TramTime time = TramTime.of(8, 15);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.TooManyChanges(howIGotHere, 5));

        NotStartedState traversalState = getNotStartedState();
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        final EnumSet<GraphLabel> labels = EnumSet.of(ROUTE_STATION);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.TooManyChanges, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotCorrectHour() {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        final EnumSet<GraphLabel> labels = EnumSet.of(HOUR);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        NotStartedState traversalState = getNotStartedState();
        TramTime time = TramTime.of(8, 15);

        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(DurationOk, howIGotHere));
        int maxInitialWaitMins = (int) maxInitialWait.toMinutes();
        EasyMock.expect(serviceHeuristics.interestedInHour(howIGotHere, time, reasons, maxInitialWaitMins, EnumSet.of(HOUR))).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.NotAtHour, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfServiceNotCorrectMinute() throws TramchesterException {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        NotStartedState traversalState = getNotStartedState();
        TramTime time = TramTime.of(8, 15);
        JourneyState journeyState = new JourneyState(time, traversalState);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNotBeenOnTripBefore(howIGotHere, node, journeyState, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.Continue, howIGotHere));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        final EnumSet<GraphLabel> labels = EnumSet.of(MINUTE);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        journeyState.board(TransportMode.Tram, node, true); // So uses non-initial wait time
        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO ,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(DurationOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkTime(howIGotHere, node, time, reasons, config.getMaxWait())).
                andReturn(ServiceReason.DoesNotOperateOnTime(time, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.DoesNotOperateOnTime, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldExcludeIfSeenSameTripBefore() throws TramchesterException {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();

        IdFor<Trip> tripId = MutableTrip.createId("tripId1");

        NotStartedState traversalState = getNotStartedState();
        TramTime time = TramTime.of(8, 15);
        JourneyState journeyState = new JourneyState(time, traversalState);

        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNotBeenOnTripBefore(howIGotHere, node, journeyState, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.Continue, howIGotHere));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);

        final EnumSet<GraphLabel> labels = EnumSet.of(MINUTE);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        journeyState.board(TransportMode.Tram, node, true); // So uses non-initial wait time
        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO ,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(DurationOk, howIGotHere));

        EasyMock.expect(serviceHeuristics.checkNotBeenOnTripBefore(howIGotHere, node, journeyState, reasons)).
                andReturn(ServiceReason.SameTrip(tripId, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.SameTrip, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.EXCLUDE_AND_PRUNE, result);
        verifyAll();
    }

    @Test
    void shouldIncludeIfMatchesNoRules() {
        TramRouteEvaluator evaluator = getEvaluatorForTest(destinationNodeId);
        BranchState<JourneyState> branchState = new TestBranchState();
        EasyMock.expect(serviceHeuristics.getMaxPathLength()).andStubReturn(400);
        EasyMock.expect(serviceHeuristics.checkNumberChanges(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumChangesOK, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberWalkingConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NumConnectionsOk, howIGotHere));
        EasyMock.expect(serviceHeuristics.checkNumberNeighbourConnections(0, howIGotHere, reasons)).
                andStubReturn(ServiceReason.IsValid(ReasonCode.NeighbourConnectionsOk, howIGotHere));

        EasyMock.expect(lowestCostSeen.everArrived()).andReturn(false);

        EasyMock.expect(path.length()).andStubReturn(50);
        EasyMock.expect(lastRelationship.isType(WALKS_TO_STATION)).andReturn(false);

        final EnumSet<GraphLabel> labels = EnumSet.of(GROUPED);
        EasyMock.expect(contentsRepository.getLabels(node)).andReturn(labels);

        TramTime time = TramTime.of(8, 15);
        NotStartedState traversalState = getNotStartedState();
        final JourneyState journeyState = new JourneyState(time, traversalState);
        branchState.setState(journeyState);

        EasyMock.expect(serviceHeuristics.journeyDurationUnderLimit(Duration.ZERO,howIGotHere, reasons)).
                andReturn(ServiceReason.IsValid(DurationOk, howIGotHere));

        EasyMock.expect(previousSuccessfulVisit.getPreviousResult(node, journeyState, labels)).andReturn(ReasonCode.PreviousCacheMiss);
        previousSuccessfulVisit.recordVisitIfUseful(ReasonCode.Continue, node, journeyState, labels);
        EasyMock.expectLastCall();

        replayAll();
        Evaluation result = evaluator.evaluate(path, branchState);
        assertEquals(Evaluation.INCLUDE_AND_CONTINUE, result);
        verifyAll();
    }

    private static class TestBranchState implements BranchState<JourneyState> {
        private JourneyState journeyState;

        @Override
        public JourneyState getState() {
            return journeyState;
        }

        @Override
        public void setState(JourneyState journeyState) {
            this.journeyState = journeyState;
        }
    }

}
