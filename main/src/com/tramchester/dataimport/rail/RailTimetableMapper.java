package com.tramchester.dataimport.rail;

import com.tramchester.config.RailConfig;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.dataimport.rail.records.reference.TrainStatus;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.dataimport.rail.repository.RailStationRecordsRepository;
import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.RailPlatformStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.WriteableTransportData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
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
    private static DataSourceID dataSourceID;
    private final RailServiceGroups railServiceGroups;

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
    private final Set<Pair<TrainStatus, TrainCategory>> travelCombinations;
    private final Set<RawService> skipped;

    public RailTimetableMapper(RailStationRecordsRepository stations, WriteableTransportData container,
                               RailConfig config, GraphFilterActive filter, BoundingBox bounds, RailRouteIdRepository railRouteRepository) {

        currentState = State.Between;
        overlay = false;
        travelCombinations = new HashSet<>();
        skipped = new HashSet<>();

        railServiceGroups = new RailServiceGroups(container);
        processor = new CreatesTransportDataForRail(stations, container, travelCombinations,
                config, filter, bounds, railServiceGroups, railRouteRepository);
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
        // TODO Deal with stations present in TiplocInsert but not in RailStationRecordsRepository
//        TIPLOCInsert insert = (TIPLOCInsert) record;
//        String atocCode = insert.getTiplocCode();
//        tiplocInsertRecords.put(atocCode, insert);
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
        travelCombinations.forEach(pair -> logger.info(String.format("Rail loaded: Status: %s Category: %s",
                pair.getLeft(), pair.getRight())));
        railServiceGroups.reportUnmatchedCancellations();
        reportSkipped(skipped);
    }

    private void reportSkipped(Set<RawService> skipped) {
        if (skipped.isEmpty()) {
            return;
        }
        logger.warn("Skipped " + skipped.size() + " records");
    }

    private void guardState(State expectedState, RailTimetableRecord record) {
        if (currentState != expectedState) {
            throw new RuntimeException(format("Expected state %s not %s at %s", expectedState, currentState, record));
        }
    }

    static class RawService {

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

        public RailLocationRecord getTerminatingLocation() {
            return terminatingLocation;
        }
    }

    private static class CreatesTransportDataForRail {
        private static final Logger logger = LoggerFactory.getLogger(CreatesTransportDataForRail.class);

        private final RailStationRecordsRepository stationRecords;
        private final WriteableTransportData container;
        private final RailServiceGroups railServiceGroups;
        private final RailRouteIdRepository railRouteIdRepository;
        private final Set<Pair<TrainStatus, TrainCategory>> travelCombinations;
        private final RailConfig config;
        private final GraphFilterActive filter;
        private final BoundingBox bounds;

        private final Map<PlatformId, MutablePlatform> platformLookup;

        private CreatesTransportDataForRail(RailStationRecordsRepository stationRecords, WriteableTransportData container,
                                            Set<Pair<TrainStatus, TrainCategory>> travelCombinations,
                                            RailConfig config, GraphFilterActive filter, BoundingBox bounds, RailServiceGroups railServiceGroups,
                                            RailRouteIdRepository railRouteIdRepository) {
            this.stationRecords = stationRecords;
            this.container = container;

            this.travelCombinations = travelCombinations;
            this.config = config;
            this.filter = filter;
            this.bounds = bounds;
            this.railServiceGroups = railServiceGroups;
            this.railRouteIdRepository = railRouteIdRepository;

            platformLookup = new HashMap<>();
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

        private boolean createNew(final RawService rawService, final boolean isOverlay) {
            final BasicSchedule basicSchedule = rawService.basicScheduleRecord;

            // assists with diagnosing data issues
            travelCombinations.add(Pair.of(basicSchedule.getTrainStatus(), basicSchedule.getTrainCategory()));

            final TransportMode mode = RailTransportModeMapper.getModeFor(rawService.basicScheduleRecord);

            final String uniqueTrainId = basicSchedule.getUniqueTrainId();
            if (!shouldInclude(mode)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Skipping %s of category %s and status %s", uniqueTrainId, basicSchedule.getTrainCategory(),
                            basicSchedule.getTrainStatus()));
                }
                railServiceGroups.recordSkip(basicSchedule);
                return false;
            }

            final OriginLocation originLocation = rawService.originLocation;
            final List<IntermediateLocation> intermediateLocations = rawService.intermediateLocations;
            final TerminatingLocation terminatingLocation = rawService.terminatingLocation;

            //logger.debug("Create schedule for " + uniqueTrainId);
            final String atocCode = rawService.extraDetails.getAtocCode();

            // Calling points
            final List<Station> allCalledAtStations = getRouteStationCallingPoints(rawService);

            if (allCalledAtStations.isEmpty() || allCalledAtStations.size()==1) {
                logger.warn(format("Skip, Not enough calling points (%s) for (%s) without bounds checking",
                        HasId.asIds(allCalledAtStations), rawService));
                railServiceGroups.recordSkip(basicSchedule);
                return false;
            }

            final List<Station> withinBoundsCallingStations = allCalledAtStations.stream().
                    filter(bounds::contained).
                    collect(Collectors.toList());

            if (withinBoundsCallingStations.isEmpty() || withinBoundsCallingStations.size()==1) {
                // likely due to all stations being filtered out as beyond geo bounds
                //logger.debug(format("Skip, Not enough calling points (%s) for (%s)", inBoundsCalledAtStations.stream(), rawService));
                railServiceGroups.recordSkip(basicSchedule);
                return false;
            }

            // Agency
            final MutableAgency mutableAgency = getOrCreateAgency(atocCode);
            final IdFor<Agency> agencyId = mutableAgency.getId();

            // route ID uses "national" ids, so without calling points filtered to be within bounds
            // this is so routes that start and/or finish out-of-bounds are named correctly
            final IdFor<Route> routeId = railRouteIdRepository.getRouteIdFor(agencyId, allCalledAtStations);

            // Service
            final MutableService service = railServiceGroups.getOrCreateService(basicSchedule, isOverlay);

            final MutableRoute route = getOrCreateRoute(routeId, rawService, mutableAgency, mode, withinBoundsCallingStations);

            route.addService(service);
            mutableAgency.addRoute(route);

            // Trip
            final MutableTrip trip = getOrCreateTrip(basicSchedule, service, route, mode);
            route.addTrip(trip);

            final TramTime originTime = originLocation.getDeparture();

            // Stations, Platforms, StopCalls
            int stopSequence = 1;
            populateForLocationIfWithinBounds(originLocation, route, trip, stopSequence, originTime);
            stopSequence = stopSequence + 1;
            for (IntermediateLocation intermediateLocation : intermediateLocations) {
                populateForLocationIfWithinBounds(intermediateLocation, route, trip, stopSequence, originTime);
                // if ....  stopSequence = stopSequence + 1; - keep sequence same as source even if skipping
                stopSequence = stopSequence + 1;
            }
            populateForLocationIfWithinBounds(terminatingLocation, route, trip, stopSequence, originTime);

            return true;
        }

        private boolean shouldInclude(final TransportMode mode) {
            return config.getModes().contains(mode);
        }

        private boolean populateForLocationIfWithinBounds(final RailLocationRecord railLocation,
                                                          final MutableRoute route, final MutableTrip trip,
                                                          final int stopSequence, final TramTime originTime) {
            if (!railLocation.getArrival().isValid()) {
                logger.warn("Invalid arrival time for " + railLocation);
            }
            if (!railLocation.getDeparture().isValid()) {
                logger.warn("Invalid departure time for " + railLocation);
            }

            if (!stationRecords.hasStationRecord(railLocation)) {
                return false;
            }

            final MutableStation station = findStationFor(railLocation);

            if (!bounds.contained(station)) {
                return false;
            }

            stationRecords.markAsInUse(station);

            final EnumSet<LocationActivityCode> activity = railLocation.getActivity();

            // Platform
            final IdFor<NaptanArea> areaId = station.getAreaId(); // naptan seems only to have rail stations, not platforms
            final MutablePlatform platform = getOrCreatePlatform(station, railLocation, areaId);

            station.addPlatform(platform);

            final boolean doesPickup = LocationActivityCode.doesPickup(activity);
            final boolean doesDropOff = LocationActivityCode.doesDropOff(activity);

            if (doesDropOff) {
                station.addRouteDropOff(route);
                platform.addRouteDropOff(route);
            }
            if (doesPickup) {
                station.addRoutePickUp(route);
                platform.addRoutePickUp(route);
            }

            // Route Station
            final RouteStation routeStation = new RouteStation(station, route);
            container.addRouteStation(routeStation);

            final StopCall stopCall;
            if (railLocation.doesStop()) {
                // TODO this doesn't cope with journeys that cross 2 days....
                final TramTime arrivalTime = getDayAdjusted(railLocation.getArrival(), originTime);
                final TramTime departureTime = getDayAdjusted(railLocation.getDeparture(), originTime);

                // TODO Request stops?
                final GTFSPickupDropoffType pickup = doesPickup ? Regular : None;
                final GTFSPickupDropoffType dropoff = doesDropOff ? Regular : None;
                stopCall = createStopCall(trip, station, platform, stopSequence, arrivalTime, departureTime, pickup, dropoff);

                if (Durations.greaterThan(TramTime.difference(arrivalTime, departureTime), Duration.ofMinutes(60))) {
                    // this definitely happens, so an info not a warning
                    logger.info("Delay of more than one hour for " + stopCall + " on trip " + trip.getId());
                }
            } else {
                stopCall = createStopcallForNoneStopping(railLocation, trip, stopSequence, station, platform, originTime);
            }

            trip.addStop(stopCall);

            return true;
        }

        private TramTime getDayAdjusted(TramTime arrivalTime, TramTime originTime) {
            if (arrivalTime.isBefore(originTime)) {
                arrivalTime = TramTime.nextDay(arrivalTime);
            }
            return arrivalTime;
        }

        @NotNull
        private StopCall createStopcallForNoneStopping(RailLocationRecord railLocation, MutableTrip trip, int stopSequence,
                                                       MutableStation station, MutablePlatform platform, TramTime originTime) {
            StopCall stopCall;
            TramTime passingTime;
            if (railLocation.isOrigin()) {
                passingTime = railLocation.getDeparture();
            } else if (railLocation.isTerminating()) {
                passingTime = railLocation.getArrival();
            } else {
                passingTime = railLocation.getPassingTime();
            }

            if (!passingTime.isValid()) {
                throw new RuntimeException("Invalid passing time for " + railLocation);
            }

            passingTime = getDayAdjusted(passingTime, originTime);

            stopCall = createStopCall(trip, station, platform, stopSequence, passingTime, passingTime, None, None);
            return stopCall;
        }

        private MutableStation findStationFor(RailLocationRecord record) {
            MutableStation station;
            if (!stationRecords.hasStationRecord(record)) {
                throw new RuntimeException(format("Missing stationid %s encountered for %s", record.getTiplocCode(), record));
            } else {
                station = stationRecords.getMutableStationFor(record);
            }
            // can't do this here, not always filtered by geo bounds at this stage
            //stationRecords.markAsInUse(station);
            return station;
        }

        @NotNull
        private RailPlatformStopCall createStopCall(MutableTrip trip, MutableStation station,
                                                MutablePlatform platform, int stopSequence, TramTime arrivalTime,
                                                TramTime departureTime, GTFSPickupDropoffType pickup, GTFSPickupDropoffType dropoff) {
            if (!arrivalTime.isValid()) {
                throw new RuntimeException(format("Invalid arrival time %s for %s on trip %s ",
                        arrivalTime, station.getId(), trip));
            }
            if (!departureTime.isValid()) {
                throw new RuntimeException(format("Invalid departure time %s for %s on trip %s ",
                        departureTime, station.getId(), trip));
            }

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

                dataSourceID = DataSourceID.rail;
                String agencyName = TrainOperatingCompanies.companyNameFor(agencyId);
                if (agencyName.equals(TrainOperatingCompanies.UNKNOWN.getCompanyName())) {
                    logger.warn("Unable to find name for agency " + atocCode);
                }
                mutableAgency = new MutableAgency(dataSourceID, agencyId, agencyName);
                container.addAgency(mutableAgency);
            }
            return mutableAgency;
        }

        private MutablePlatform getOrCreatePlatform(Station originStation, RailLocationRecord originLocation, IdFor<NaptanArea> areaId) {

            final String originLocationPlatform = originLocation.getPlatform();
            final String platformNumber = originLocationPlatform.isEmpty() ? "UNK" : originLocationPlatform;

            PlatformId platformId = PlatformId.createId(originStation.getId(), platformNumber);

            final MutablePlatform platform;
            if (platformLookup.containsKey(platformId)) {
                platform = platformLookup.get(platformId);
            } else {
                platform = new MutablePlatform(platformId, originStation, originStation.getName(), dataSourceID, platformNumber,
                        areaId, originStation.getLatLong(), originStation.getGridPosition(), originStation.isMarkedInterchange());
                container.addPlatform(platform);
                platformLookup.put(platformId, platform);
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
            return StringIdFor.withPrefix("trip:", service.getId(), Trip.class);
        }

        private MutableRoute getOrCreateRoute(IdFor<Route> routeId, RawService rawService, MutableAgency mutableAgency,
                                              final TransportMode mode, List<Station> withinBoundsCallingPoints) {
            IdFor<Agency> agencyId = mutableAgency.getId();
            MutableRoute route;

            if (container.hasRouteId(routeId)) {
                // route id already present
                route = container.getMutableRoute(routeId);
                IdFor<Agency> routeAgencyCode = route.getAgency().getId();
                if (!routeAgencyCode.equals(agencyId)) {
                    String msg = String.format("Got route %s wrong agency id (%s) expected: %s\nSchedule: %s\nExtraDetails: %s",
                            routeId, routeAgencyCode, agencyId, rawService.basicScheduleRecord, rawService.extraDetails);
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }
                if (!matchingTransportModes(route, mode)) {
                    String msg = String.format("Got route %s wrong TransportMode (%s) route had: %s\nSchedule: %s\nExtraDetails: %s",
                            routeId, mode, route.getTransportMode(), rawService.basicScheduleRecord, rawService.extraDetails);
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }
            } else {
                // note:create RailReplacementBus routes as Train
                TransportMode actualMode = (mode==RailReplacementBus) ? Train : mode;
                route = new MutableRailRoute(routeId, withinBoundsCallingPoints, mutableAgency, actualMode);
                container.addRoute(route);
            }
            return route;
        }

        private boolean matchingTransportModes(Route route, TransportMode mode) {
            if (route.getTransportMode()==Train) {
                return mode==RailReplacementBus || mode==Train;
            }
            return route.getTransportMode()==mode;
        }

        private List<Station> getRouteStationCallingPoints(RawService rawService) {

            List<Station> result = new ArrayList<>();

            // add the starting point
            if (stationRecords.hasStationRecord(rawService.originLocation)) {
                final MutableStation station = findStationFor(rawService.originLocation);
                result.add(station);
            }

            // add the intermediates
            // TODO for now don't record passed stations (not stopping) but might want to do so in future
            // to assist with live data processing
            final List<IntermediateLocation> callingRecords = rawService.intermediateLocations.stream().
                    filter(IntermediateLocation::doesStop).
                    collect(Collectors.toList());

            final List<Station> intermediates = callingRecords.stream().
                    filter(stationRecords::hasStationRecord).
                    map(this::findStationFor).
                    collect(Collectors.toList());

            result.addAll(intermediates);

            // add the final station
            if (stationRecords.hasStationRecord(rawService.terminatingLocation)) {
                result.add(findStationFor(rawService.terminatingLocation));
            }

            if (!filter.isActive()) {
                if (callingRecords.size() != intermediates.size()) {
                    Set<String> missing = callingRecords.stream().
                            filter(record -> !stationRecords.hasStationRecord(record)).
                            map(IntermediateLocation::getTiplocCode).
                            collect(Collectors.toSet());
                    logger.warn(format("Did not match all calling points (got %s of %s) for %s Missing: %s",
                            intermediates.size(), callingRecords.size(), rawService.basicScheduleRecord,
                            missing));
                }
            }

            return result;
        }

    }

}
