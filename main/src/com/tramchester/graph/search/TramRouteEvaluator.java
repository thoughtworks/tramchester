package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.PreviousSuccessfulVisits;
import com.tramchester.graph.search.states.HowIGotHere;
import com.tramchester.graph.search.states.TraversalState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;

public class TramRouteEvaluator implements PathEvaluator<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramRouteEvaluator.class);

    private final Set<Long> destinationNodeIds;
    private final ServiceHeuristics serviceHeuristics;
    private final NodeTypeRepository nodeTypeRepository;
    private final ServiceReasons reasons;
    private final PreviousSuccessfulVisits previousSuccessfulVisit;
    private int success;
    private int currentLowestCost;
    private final Set<Long> stationNodes;
    private final boolean loopDetection;

    public TramRouteEvaluator(ServiceHeuristics serviceHeuristics, Set<Long> destinationNodeIds,
                              NodeTypeRepository nodeTypeRepository, ServiceReasons reasons, PreviousSuccessfulVisits previousSuccessfulVisit,
                              TramchesterConfig config) {
        this.serviceHeuristics = serviceHeuristics;
        this.destinationNodeIds = destinationNodeIds;
        this.nodeTypeRepository = nodeTypeRepository;
        this.reasons = reasons;
        this.previousSuccessfulVisit = previousSuccessfulVisit;
        Set<GTFSTransportationType> transportModes = config.getTransportModes();
        loopDetection = transportModes.contains(GTFSTransportationType.bus) || transportModes.contains(GTFSTransportationType.train);
        success = 0;
        currentLowestCost = Integer.MAX_VALUE;
        stationNodes = new HashSet<>();
    }

    public void dispose() {
        previousSuccessfulVisit.clear();
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }

    @Override
    public Evaluation evaluate(Path path, BranchState<JourneyState> state) {
        ImmutableJourneyState journeyState = state.getState();
        TramTime journeyClock = journeyState.getJourneyClock();

        Node nextNode = path.endNode();

        if (previousSuccessfulVisit.hasUsableResult(nextNode, journeyClock)) {
            HowIGotHere howIGotHere = new HowIGotHere(path);
            reasons.recordReason(ServiceReason.Cached(journeyClock, howIGotHere));
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        ServiceReason.ReasonCode reasonCode = doEvaluate(path, journeyState, nextNode);
        Evaluation result = decideEvaluationAction(reasonCode);

        previousSuccessfulVisit.recordVisitIfUseful(reasonCode, nextNode, journeyClock);

        return result;
    }

    public static Evaluation decideEvaluationAction(ServiceReason.ReasonCode code) {
        switch (code) {
            case ServiceDateOk:
            case ServiceTimeOk:
            case NumChangesOK:
            case TimeOk:
            case HourOk:
            case Reachable:
            case ReachableNoCheck:
            case DurationOk:
            case WalkOk:
            case Continue:
                return Evaluation.INCLUDE_AND_CONTINUE;
            case Arrived:
                return Evaluation.INCLUDE_AND_PRUNE;
            case LongerPath:
            case SeenBusStationBefore:
            case PathTooLong:
            case TooManyChanges:
            case NotReachable:
            case TookTooLong:
            case ServiceNotRunningAtTime:
            case NotAtHour:
            case NotAtQueryTime:
            case NotOnQueryDate:
            case AlreadyDeparted:
                return Evaluation.EXCLUDE_AND_PRUNE;
            default:
                throw new RuntimeException("Unexpected reasoncode during evaluation: " + code.name());

        }
    }

    private ServiceReason.ReasonCode doEvaluate(Path thePath, ImmutableJourneyState journeyState, Node nextNode) {

        long nextNodeId = nextNode.getId();

        TraversalState previousTraversalState = journeyState.getTraversalState();
        HowIGotHere howIGotHere = new HowIGotHere(thePath);
        if (destinationNodeIds.contains(nextNodeId)) {
            // we've arrived
            int totalCost = previousTraversalState.getTotalCost();
            if (totalCost <= currentLowestCost) {
                // a better route than seen so far
                // <= equals so we include multiple options and routes in the results
                // An alternative to this would be to search over a finer grained list of times and catch alternatives
                // that way
                success = success + 1;
                currentLowestCost = totalCost;
                reasons.recordSuccess();
                return ServiceReason.ReasonCode.Arrived;
            } else {
                // found a route, but longer than current shortest
                reasons.recordReason(ServiceReason.Longer(howIGotHere));
                return ServiceReason.ReasonCode.LongerPath;
            }
        } else if (success>0) {
            // Not arrived, but we do have at least one successful route to our destination
            int totalCost = previousTraversalState.getTotalCost();
            if (totalCost>currentLowestCost) {
                // already longer that current shortest, no need to continue
                reasons.recordReason(ServiceReason.Longer(howIGotHere));
                return ServiceReason.ReasonCode.LongerPath;
            }
        }

        reasons.recordStat(journeyState);

        // no journey longer than N nodes
        if (thePath.length()>serviceHeuristics.getMaxPathLength()) {
            logger.warn("Hit max path length");
            reasons.recordReason(ServiceReason.PathToLong(howIGotHere));
            return ServiceReason.ReasonCode.PathTooLong;
        }

        // number of changes?
        if (!serviceHeuristics.checkNumberChanges(journeyState.getNumberChanges(), howIGotHere, reasons).isValid()) {
            return ServiceReason.ReasonCode.TooManyChanges;
        }

        // journey too long?
        if (!serviceHeuristics.journeyDurationUnderLimit(previousTraversalState.getTotalCost(), howIGotHere, reasons).isValid()) {
            return ServiceReason.ReasonCode.TookTooLong;
        }

        // is even reachable from here?
        if (nodeTypeRepository.isRouteStation(nextNode)) {
            if (!serviceHeuristics.canReachDestination(nextNode, howIGotHere, reasons).isValid()) {
                return ServiceReason.ReasonCode.NotReachable;
            }
        }

        // is the service running today?
        boolean isService = nodeTypeRepository.isService(nextNode);
        if (isService) {
            if (!serviceHeuristics.checkServiceDate(nextNode, howIGotHere, reasons).isValid()) {
                return ServiceReason.ReasonCode.NotOnQueryDate;
            }
        }

        // seeing loops?
        if (loopDetection) {
            if (nodeTypeRepository.isBusStation(nextNode) || nodeTypeRepository.isTrainStation(nextNode)) {
                if (stationNodes.contains(nextNodeId)) {
                    reasons.recordReason(ServiceReason.SeenBusStationBefore(howIGotHere));
                    return ServiceReason.ReasonCode.SeenBusStationBefore;
                }
                stationNodes.add(nextNodeId);
            }
        }

        Relationship inboundRelationship = thePath.lastRelationship();

        if (inboundRelationship != null) {
            // for walking routes we do want to include them all even if at same time
            if (inboundRelationship.isType(WALKS_TO)) {
                // TODO Record with different reason?
                return ServiceReason.ReasonCode.WalkOk;
            }
        }

        TramTime visitingTime = journeyState.getJourneyClock();

        // service available to catch?
        if (isService) {
            if (!serviceHeuristics.checkServiceTime(howIGotHere, nextNode, visitingTime, reasons).isValid()) {
                return ServiceReason.ReasonCode.ServiceNotRunningAtTime;
            }
        }

        // check time, just hour first
        if (nodeTypeRepository.isHour(nextNode)) {
            if (!serviceHeuristics.interestedInHour(howIGotHere, nextNode, visitingTime, reasons).isValid()) {
                return ServiceReason.ReasonCode.NotAtHour;
            }
        }

        // check time
        if (nodeTypeRepository.isTime(nextNode)) {
            ServiceReason serviceReason = serviceHeuristics.checkTime(howIGotHere, nextNode, visitingTime, reasons);
            return serviceReason.getReasonCode(); // valid, or not at time
        }

        return ServiceReason.ReasonCode.Continue;
    }

}
