package com.tramchester.dataimport.rail;

import com.tramchester.config.RailConfig;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.dataimport.rail.records.reference.TrainStatus;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.RailPlatformStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.WriteableTransportData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.None;
import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.domain.reference.TransportMode.*;
import static java.lang.String.format;
import static java.time.temporal.ChronoField.*;

public class RailTimetableMapper {
    private static final Logger logger = LoggerFactory.getLogger(RailTimetableMapper.class);

    public static final DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendValue(MONTH_OF_YEAR, 2)
                .appendValue(DAY_OF_MONTH, 2).toFormatter();

    private enum State {
        SeenSchedule,
        SeenScheduleExtra,
        SeenOrigin,
        Between
    }
    
    private State currentState;
    private boolean overlay;
    private RawService rawService;
    private final CreatesTransportDataForRail processor;
    private final Map<String,TIPLOCInsert> tiplocInsertRecords;
    private final MissingStations missingStations;
    private final Set<Pair<TrainStatus, TrainCategory>> travelCombinations;
    private final Set<RawService> skipped;

    public RailTimetableMapper(RailTransportDataFromFiles.StationsTemporary stations, WriteableTransportData container,
                               RailConfig config, GraphFilterActive filter) {
        currentState = State.Between;
        overlay = false;
        tiplocInsertRecords = new HashMap<>();
        missingStations = new MissingStations();
        travelCombinations = new HashSet<>();
        skipped = new HashSet<>();
        processor = new CreatesTransportDataForRail(stations, container, missingStations, travelCombinations, config, filter);
    }

    public void seen(RailTimetableRecord record) {
        switch (record.getRecordType()) {
            case TiplocInsert -> tipLocInsert(record);
            case BasicSchedule -> seenBegin(record);
            case BasicScheduleExtra -> seenExtraInfo(record);
            case TerminatingLocation -> seenEnd(record);
            case OriginLocation -> seenOrigin(record);
            case IntermediateLocation -> seenIntermediate(record);
        }
    }

    private void tipLocInsert(RailTimetableRecord record) {
        TIPLOCInsert insert = (TIPLOCInsert) record;
        String atocCode = insert.getTiplocCode();
        tiplocInsertRecords.put(atocCode, insert);
    }

    private void seenExtraInfo(RailTimetableRecord record) {
        guardState(State.SeenSchedule, record);
        rawService.addScheduleExtra(record);
        currentState = State.SeenScheduleExtra;
    }

    private void seenIntermediate(RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        rawService.addIntermediate(record);
    }

    private void seenOrigin(RailTimetableRecord record) {
        guardState(State.SeenScheduleExtra, record);
        rawService.addOrigin(record);
        currentState = State.SeenOrigin;
    }

    private void seenEnd(RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        rawService.finish(record);
        processor.consume(rawService, overlay, skipped);
        currentState = State.Between;
        overlay = false;
    }

    private void seenBegin(RailTimetableRecord record) {
        BasicSchedule basicSchedule = (BasicSchedule) record;
        guardState(State.Between, record);

        rawService = new RawService(basicSchedule);

        switch (basicSchedule.getSTPIndicator()) {
            case Cancellation -> {
                processor.recordCancellations(basicSchedule);
                currentState = State.Between;
            }
            case New, Permanent -> currentState = State.SeenSchedule;
            case Overlay -> {
                overlay = true;
                currentState = State.SeenSchedule;
            }
            default -> logger.warn("Not handling " + basicSchedule);
        }
    }

    public void reportDiagnostics() {
        Set<String> missingTiplocs = missingStations.report(logger);
        missingTiplocs.stream().
                filter(tiplocInsertRecords::containsKey).
                map(tiplocInsertRecords::get).
                forEach(record -> logger.info("Had record for missing tiploc: "+record));
        travelCombinations.forEach(pair -> logger.info(String.format("Rail mode Status: %s Category: %s",
                pair.getLeft(), pair.getRight())));
        reportSkipped(skipped);
    }

    private void reportSkipped(Set<RawService> skipped) {
        if (skipped.isEmpty()) {
            return;
        }
        logger.warn("Skipped " + skipped.size() + " records");
//        StringBuilder builder = new StringBuilder();
//        skipped.forEach(skip -> {
//            builder.append(" ").append(skip.basicScheduleRecord.getUniqueTrainId());
//        });
//        logger.warn("Skipped following services with unique train ids " + builder);
    }

    private void guardState(State expectedState, RailTimetableRecord record) {
        if (currentState != expectedState) {
            throw new RuntimeException(format("Expected state %s not %s at %s", expectedState, currentState, record));
        }
    }

    private static class RawService {

        private final BasicSchedule basicScheduleRecord;
        private BasicScheduleExtraDetails extraDetails;
        private OriginLocation originLocation;
        private final List<IntermediateLocation> intermediateLocations;
        private TerminatingLocation terminatingLocation;

        public RawService(RailTimetableRecord basicScheduleRecord) {
            this.basicScheduleRecord = (BasicSchedule) basicScheduleRecord;
            intermediateLocations = new ArrayList<>();
        }

        public void addIntermediate(RailTimetableRecord record) {
            intermediateLocations.add((IntermediateLocation) record);
        }

        public void addOrigin(RailTimetableRecord record) {
            this.originLocation = (OriginLocation) record;
        }

        public void finish(RailTimetableRecord record) {
            this.terminatingLocation = (TerminatingLocation) record;
        }

        public void addScheduleExtra(RailTimetableRecord record) {
            this.extraDetails = (BasicScheduleExtraDetails) record;
        }
    }

    private static class CreatesTransportDataForRail {
        private static final Logger logger = LoggerFactory.getLogger(CreatesTransportDataForRail.class);

        private final RailTransportDataFromFiles.StationsTemporary stations;
        private final WriteableTransportData container;
        private final MissingStations missingStations; // stations missing unexpectedly
        private final RailServiceGroups railServiceGroups;
        private final Set<Pair<TrainStatus, TrainCategory>> travelCombinations;
        private final RailConfig config;
        private final RailRouteIDBuilder railRouteIDBuilder;
        private final GraphFilterActive filter;

        private CreatesTransportDataForRail(RailTransportDataFromFiles.StationsTemporary stations, WriteableTransportData container,
                                            MissingStations missingStations, Set<Pair<TrainStatus, TrainCategory>> travelCombinations,
                                            RailConfig config, GraphFilterActive filter) {
            this.stations = stations;
            this.container = container;
            this.missingStations = missingStations;

            this.railServiceGroups = new RailServiceGroups(container);
            this.travelCombinations = travelCombinations;
            this.config = config;
            this.filter = filter;
            this.railRouteIDBuilder = new RailRouteIDBuilder();
        }

        public void consume(RawService rawService, boolean isOverlay, Set<RawService> skipped) {
            BasicSchedule basicSchedule = rawService.basicScheduleRecord;

            switch (basicSchedule.getTransactionType()) {
                case New -> {
                    if (!createNew(rawService, isOverlay)) {
                        skipped.add(rawService);
                    }
                }
                case Delete -> delete(rawService.basicScheduleRecord);
                case Revise -> revise(rawService);
                case Unknown -> logger.warn("Unknown transaction type for " + rawService.basicScheduleRecord);
            }
        }

        private void delete(BasicSchedule basicScheduleRecord) {
            logger.error("Delete schedule " + basicScheduleRecord);
        }

        private void revise(RawService rawService) {
            logger.error("Revise schedule " + rawService);
        }

        private void recordCancellations(BasicSchedule basicSchedule) {
            railServiceGroups.applyCancellation(basicSchedule);
        }

        private boolean createNew(RawService rawService, boolean isOverlay) {
            final BasicSchedule basicSchedule = rawService.basicScheduleRecord;

            // assists with diagnosing data issues
            travelCombinations.add(Pair.of(basicSchedule.getTrainStatus(), basicSchedule.getTrainCategory()));

            TransportMode mode = RailTransportModeMapper.getModeFor(rawService.basicScheduleRecord);

            final String uniqueTrainId = basicSchedule.getUniqueTrainId();
            if (!shouldInclude(mode)) {
                logger.debug(format("Skipping %s of category %s and status %s", uniqueTrainId, basicSchedule.getTrainCategory(),
                        basicSchedule.getTrainStatus()));
                railServiceGroups.recordSkip(basicSchedule);
                return false;
            }

            final OriginLocation originLocation = rawService.originLocation;
            final List<IntermediateLocation> intermediateLocations = rawService.intermediateLocations;
            final TerminatingLocation terminatingLocation = rawService.terminatingLocation;

            logger.debug("Create schedule for " + uniqueTrainId);
            final String atocCode = rawService.extraDetails.getAtocCode();

            // Agency
            MutableAgency mutableAgency = getOrCreateAgency(atocCode);

            final IdFor<Agency> agencyId = mutableAgency.getId();

            // Calling points
            List<Station> calledAtStations = getRouteStationCallingPoints(rawService);

            if (calledAtStations.isEmpty() || calledAtStations.size()==1) {
                // likely due to all stations being filtered out as beyond geo bounds
                logger.debug(format("Skip, Not enough calling points (%s) for (%s)", calledAtStations.stream(), rawService));
                return false;
            }

            // Service
            MutableService service = railServiceGroups.getOrCreateService(basicSchedule, isOverlay);

            IdFor<Route> routeId = railRouteIDBuilder.getIdFor(agencyId, calledAtStations);

            MutableRoute route = getOrCreateRoute(routeId, rawService, mutableAgency, mode, calledAtStations);
            route.addService(service);
            mutableAgency.addRoute(route);

            // Trip
            MutableTrip trip = getOrCreateTrip(basicSchedule, service, route, mode);
            route.addTrip(trip);

            // Stations, Platforms, StopCalls
            int stopSequence = 1;
            //boolean crossMidnight = crossMidnight(originLocation, terminatingLocation);
            TramTime originTime = originLocation.getDeparture();
            populateForLocation(originLocation, route, trip, stopSequence, false, agencyId, basicSchedule, originTime);
            stopSequence = stopSequence + 1;
            for (IntermediateLocation intermediateLocation : intermediateLocations) {
                if (populateForLocation(intermediateLocation, route, trip, stopSequence, false, agencyId, basicSchedule, originTime)) {
                    stopSequence = stopSequence + 1;
                }
            }
            populateForLocation(terminatingLocation, route, trip, stopSequence, true, agencyId, basicSchedule, originTime);

            return true;
        }

        private boolean shouldInclude(TransportMode mode) {
            return config.getModes().contains(mode);
        }

        private boolean populateForLocation(RailLocationRecord railLocation, MutableRoute route, MutableTrip trip, int stopSequence,
                                            boolean lastStop, IdFor<Agency> agencyId, BasicSchedule schedule, TramTime originTime) {

            if (!isStation(railLocation)) {
                if (!stations.wasOutOfBounds(railLocation.getTiplocCode())) {
                    // only log an issue if tiploc was not filtered out for being outside area of interest
                    missingStationDiagnosticsFor(railLocation, agencyId, schedule);
                }
                return false;
            }

            MutableStation station = findStationFor(railLocation);

            if (railLocation.isPassingRecord()) {
                station.addPassingRoute(route);
                return false;
            }

            GTFSPickupDropoffType pickup;
            GTFSPickupDropoffType dropoff;

            pickup = lastStop ? None : Regular;
            dropoff = stopSequence==1 ? None : Regular;

            if (pickup != None) {
                station.addRoutePickUp(route);
            }
            if (dropoff != None) {
                station.addRouteDropOff(route);
            }

            // Route Station
            RouteStation routeStation = new RouteStation(station, route);
            container.addRouteStation(routeStation);

            // Platform
            MutablePlatform platform = getOrCreatePlatform(station, railLocation);
            platform.addRoute(route);
            station.addPlatform(platform);

            // Stop Call
            // TODO this doesn't cope with journeys that cross 2 days....
            TramTime arrivalTime = railLocation.getArrival();
            if (arrivalTime.isBefore(originTime)) {
                arrivalTime = TramTime.nextDay(arrivalTime);
            }

            TramTime departureTime = railLocation.getDeparture();
            if (departureTime.isBefore(originTime)) {
                departureTime = TramTime.nextDay(departureTime);
            }

            StopCall stopCall = createStopCall(trip, station, platform, stopSequence,
                    arrivalTime, departureTime, pickup, dropoff);
            if (TramTime.diffenceAsMinutes(arrivalTime, departureTime)>60) {
                // this definitely happens, so an info not a warning
                logger.info("Delay of more than one hour for " + stopCall);
            }
            trip.addStop(stopCall);

            return true;
        }

        private void missingStationDiagnosticsFor(RailLocationRecord railLocation, IdFor<Agency> agencyId, BasicSchedule schedule) {
            final RailRecordType recordType = railLocation.getRecordType();
            boolean intermediate = (recordType==RailRecordType.IntermediateLocation);
            if (intermediate) {
                return;
            }

            String tiplocCode = railLocation.getTiplocCode();
            missingStations.record(tiplocCode, railLocation, agencyId, schedule);
        }

        private boolean isStation(RailLocationRecord record) {
            final String tiplocCode = record.getTiplocCode();
            IdFor<Station> stationId = StringIdFor.createId(tiplocCode);
            return stations.hasStationId(stationId);
        }

        private MutableStation findStationFor(RailLocationRecord record) {
            final String tiplocCode = record.getTiplocCode();
            IdFor<Station> stationId = StringIdFor.createId(tiplocCode);
            MutableStation station;
            if (!stations.hasStationId(stationId)) {
                throw new RuntimeException(format("Missing stationid %s encountered for %s", stationId, record));
            } else {
                station = stations.getMutableStation(stationId);
            }
            stations.markAsNeeded(station);
            return station;
        }

        @NotNull
        private RailPlatformStopCall createStopCall(MutableTrip trip, MutableStation station,
                                                MutablePlatform platform, int stopSequence, TramTime arrivalTime,
                                                TramTime departureTime, GTFSPickupDropoffType pickup, GTFSPickupDropoffType dropoff) {
            return new RailPlatformStopCall(station, arrivalTime, departureTime, stopSequence, pickup, dropoff, trip, platform);
        }

        private MutableAgency getOrCreateAgency(String atocCode) {
            MutableAgency mutableAgency;
            final IdFor<Agency> agencyId = Agency.createId(atocCode);
            if (container.hasAgencyId(agencyId)) {
                mutableAgency = container.getMutableAgency(agencyId);
            } else {
                // todo get list of atoc names
                logger.info("Creating agency for atco code " + atocCode);

                // todo - need to look up the actual agency name here, just using the id at the moment
                mutableAgency = new MutableAgency(DataSourceID.rail, agencyId, atocCode);
                container.addAgency(mutableAgency);
            }
            return mutableAgency;
        }

        private MutablePlatform getOrCreatePlatform(MutableStation originStation, RailLocationRecord originLocation) {
            String platformNumber = originLocation.getPlatform();
            if (platformNumber.isBlank()) {
                platformNumber = "UNK";
            }
            IdFor<Platform> platformId = StringIdFor.createId(originLocation.getTiplocCode() + ":" + platformNumber);
            MutablePlatform platform;
            if (container.hasPlatformId(platformId)) {
                platform = container.getMutablePlatform(platformId);
            } else {
                platform = new MutablePlatform(platformId, originStation.getName(), platformNumber,
                        originStation.getLatLong());
                container.addPlatform(platform);
            }

            return platform;
        }

        private MutableTrip getOrCreateTrip(BasicSchedule schedule, MutableService service, MutableRoute route, TransportMode mode) {
            MutableTrip trip;
            IdFor<Trip> tripId = createTripIdFor(service);
            if (container.hasTripId(tripId)) {
                logger.info("Had existing tripId: " + tripId + " for " + schedule);
                trip = container.getMutableTrip(tripId);
            } else {
                trip = new MutableTrip(tripId, schedule.getTrainIdentity(), service, route, mode);
                container.addTrip(trip);
                service.addTrip(trip);
            }
            return trip;
        }

        private IdFor<Trip> createTripIdFor(MutableService service) {
            return StringIdFor.withPrefix("trip:", service.getId());
        }

        private MutableRoute getOrCreateRoute(IdFor<Route> routeId, RawService rawService, MutableAgency mutableAgency, final TransportMode mode,
                                              List<Station> callingPoints) {
            IdFor<Agency> agencyId = mutableAgency.getId();
            MutableRoute route;

            if (container.hasRouteId(routeId)) {
                route = container.getMutableRoute(routeId);
                IdFor<Agency> routeAgencyCode = route.getAgency().getId();
                if (!routeAgencyCode.equals(agencyId)) {
                    String msg = String.format("Got route %s wrong agency id (%s) expected: %s\nSchedule: %s\nExtraDetails: %s",
                            routeId, routeAgencyCode, agencyId, rawService.basicScheduleRecord, rawService.extraDetails);
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }
                if (!allowedModes(route, mode)) {
                    String msg = String.format("Got route %s wrong TransportMode (%s) route had: %s\nSchedule: %s\nExtraDetails: %s",
                            routeId, mode, route.getTransportMode(), rawService.basicScheduleRecord, rawService.extraDetails);
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }
            } else {
                // record rail replacement bus as a train route
                TransportMode actualMode = (mode==RailReplacementBus) ? Train : mode;
                route = new MutableRailRoute(routeId, callingPoints, mutableAgency, actualMode);
                container.addRoute(route);
            }
            return route;
        }

        private boolean allowedModes(Route route, TransportMode mode) {
            if (route.getTransportMode()==Train) {
                return mode==RailReplacementBus || mode==Train;
            }
            if (route.getTransportMode()==Subway) {
                return mode==RailReplacementBus || mode==Subway;
            }
            return route.getTransportMode()==mode;
        }

        private List<Station> getRouteStationCallingPoints(RawService rawService) {
            List<Station> result = new ArrayList<>();
            if (isStation(rawService.originLocation)) {
                result.add(findStationFor(rawService.originLocation));
            }
            List<Station> inters = rawService.intermediateLocations.stream().
                    filter(intermediateLocation -> !intermediateLocation.isPassingRecord()).
                    filter(this::isStation).
                    map(this::findStationFor).
                    collect(Collectors.toList());
            result.addAll(inters);
            if (isStation(rawService.terminatingLocation)) {
                result.add(findStationFor(rawService.terminatingLocation));
            }

            if (result.isEmpty()) {
                // likely filtered out by geo bounds
                logger.debug("Did not get any calling points for " + rawService.basicScheduleRecord);
            } else if (result.size()==1 && !filter.isFiltered()) {
                // less likely to see this
                logger.warn("Only one calling points for " + rawService.basicScheduleRecord);
            }
            return result;
        }

    }

    private static class MissingStations {
        private final Map<String, MissingStation> missing;

        private MissingStations() {
            missing = new HashMap<>();
        }

        public void record(String tiplocCode, RailLocationRecord railLocation, IdFor<Agency> agencyId, BasicSchedule schedule) {
            if (!missing.containsKey(tiplocCode)) {
                missing.put(tiplocCode,new MissingStation(tiplocCode, agencyId, schedule));
            }
            missing.get(tiplocCode).addRecord(railLocation);
        }

        public Set<String> report(Logger logger) {
            if (missing.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> tiplocCodes = new HashSet<>();
            missing.forEach((tiplocCode, missingStation) -> {
                missingStation.report(logger);
                tiplocCodes.add(tiplocCode);
            });
            return tiplocCodes;
        }
    }

    private static class MissingStation {
        private final String tiplocCode;
        private final Set<RailLocationRecord> records;
        private final IdFor<Agency> agencyId;
        private final BasicSchedule schedule;

        private MissingStation(String tiplocCode, IdFor<Agency> agencyId, BasicSchedule schedule) {
            this.tiplocCode = tiplocCode;
            this.agencyId = agencyId;
            this.schedule = schedule;
            records = new HashSet<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MissingStation that = (MissingStation) o;

            if (!tiplocCode.equals(that.tiplocCode)) return false;
            return agencyId.equals(that.agencyId);
        }

        @Override
        public int hashCode() {
            int result = tiplocCode.hashCode();
            result = 31 * result + agencyId.hashCode();
            return result;
        }

        public void addRecord(RailLocationRecord railLocation) {
            records.add(railLocation);
        }

        public void report(Logger logger) {
            logger.info(format("Missing tiplocCode %s for %s %s in records %s",
                    tiplocCode, agencyId, schedule, records));
        }
    }
}
