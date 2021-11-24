package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.*;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
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
import com.tramchester.repository.TransportDataContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.None;
import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static java.lang.String.format;
import static java.time.temporal.ChronoField.*;

public class RailTimetableMapper {
    private static final Logger logger = LoggerFactory.getLogger(RailTimetableMapper.class);

    private static final DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
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

    public RailTimetableMapper(TransportDataContainer container) {
        currentState = State.Between;
        overlay = false;
        tiplocInsertRecords = new HashMap<>();
        missingStations = new MissingStations();
        processor = new CreatesTransportDataForRail(container, tiplocInsertRecords, missingStations);
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
        if (overlay) {
            processor.overlay(rawService);
        } else {
            processor.consume(rawService);
        }
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
    }

    private void guardState(State expectedState, RailTimetableRecord record) {
        if (currentState != expectedState) {
            throw new RuntimeException(format("Expected state %s not %s at %s", expectedState, currentState, record));
        }
    }

    private static class RawService {

        private final BasicSchedule basicScheduleRecord;
        private final List<IntermediateLocation> intermediateLocations;
        private OriginLocation originLocation;
        private TerminatingLocation terminatingLocation;
        private BasicScheduleExtraDetails extraDetails;

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

        private final TransportDataContainer container;
        private final Map<String, TIPLOCInsert> tiplocInsertRecords;
        private final MissingStations missingStations; // stations missing unexpectedly
        private final IdSet<Service> skippedServices; // services skipped due to train category etc.

        private CreatesTransportDataForRail(TransportDataContainer container, Map<String, TIPLOCInsert> tiplocInsertRecords,
                                            MissingStations missingStations) {
            this.container = container;
            this.tiplocInsertRecords = tiplocInsertRecords;
            this.missingStations = missingStations;
            skippedServices = new IdSet<>();
        }

        public void consume(RawService rawService) {
            BasicSchedule basicSchedule = rawService.basicScheduleRecord;

            switch (basicSchedule.getTransactionType()) {
                case N -> createNew(rawService);
                case D -> delete(rawService.basicScheduleRecord);
                case R -> revise(rawService);
                case Unknown -> logger.warn("Unknown transaction type for " + rawService.basicScheduleRecord);
            }

        }


        public void overlay(RawService rawService) {
            BasicSchedule basicSchedule = rawService.basicScheduleRecord;
            final IdFor<Service> serviceId = getServiceIdFor(basicSchedule);
            final Service service = container.getServiceById(serviceId);
            if (service ==null) {
                logger.warn("Overlay found for service, but existing service not found " + serviceId);
            }
            consume(rawService); // service Id will match so will overwrite existing service record
        }

        private void delete(BasicSchedule basicScheduleRecord) {
            logger.info("Delete schedule " + basicScheduleRecord);
        }

        private void revise(RawService rawService) {
            logger.info("Revise schedule " + rawService);
        }

        private void recordCancellations(BasicSchedule basicSchedule) {
            final IdFor<Service> serviceId = getServiceIdFor(basicSchedule);
            if (container.hasServiceId(serviceId)) {
                logger.debug("Making cancellations for schedule " + basicSchedule.getUniqueTrainId());
                Service service = container.getMutableService(serviceId);
                ServiceCalendar calendar = service.getCalendar();
                addServiceExceptions(calendar, basicSchedule.getStartDate(), basicSchedule.getEndDate(), basicSchedule.getDaysOfWeek());
            } else {
                if (!skippedServices.contains(serviceId)) {
                    logger.warn("Failed to find service to amend for " + basicSchedule);
                }
            }
        }

        private void addServiceExceptions(ServiceCalendar calendar, LocalDate startDate, LocalDate endDate, Set<DayOfWeek> excludedDays) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (excludedDays.contains(current.getDayOfWeek())) {
                    calendar.excludeDate(current);
                }
                current = current.plusDays(1);
            }
        }

        private void createNew(RawService rawService) {
            final BasicSchedule basicSchedule = rawService.basicScheduleRecord;
            TrainCategory cat = basicSchedule.getTrainCategory();
            final String uniqueTrainId = basicSchedule.getUniqueTrainId();
            if (cat==TrainCategory.LondonUndergroundOrMetroService || cat==TrainCategory.BusService) {
                logger.debug(format("Skipping %s of category %s and status %s", uniqueTrainId, basicSchedule.getTrainCategory(),
                        basicSchedule.getTrainStatus()));
                skippedServices.add(getServiceIdFor(basicSchedule));
                return;
            }

            final OriginLocation originLocation = rawService.originLocation;
            final List<IntermediateLocation> intermediateLocations = rawService.intermediateLocations;
            final TerminatingLocation terminatingLocation = rawService.terminatingLocation;

            logger.debug("Create schedule for " + uniqueTrainId);
            final String atocCode = rawService.extraDetails.getAtocCode();

            // Agency
            MutableAgency mutableAgency = getOrCreateAgency(atocCode);
            final IdFor<Agency> agencyId = mutableAgency.getId();

            // Service
            MutableService service = getOrCreateService(basicSchedule);

            // Route
            MutableRoute route = getOrCreateRoute(originLocation, terminatingLocation, mutableAgency, atocCode);
            route.addService(service);
            mutableAgency.addRoute(route);

            // Trip
            MutableTrip trip = getOrCreateTrip(basicSchedule, service, route);
            route.addTrip(trip);

            // Stations, Platforms, StopCalls
            int stopSequence = 1;
            populateForLocation(originLocation, route, trip, stopSequence, false, agencyId, basicSchedule);
            stopSequence = stopSequence + 1;
            for (IntermediateLocation intermediateLocation : intermediateLocations) {
                if (populateForLocation(intermediateLocation, route, trip, stopSequence, false, agencyId, basicSchedule)) {
                    stopSequence = stopSequence + 1;
                }
            }
            populateForLocation(terminatingLocation, route, trip, stopSequence, true, agencyId, basicSchedule);
        }

        private boolean populateForLocation(RailLocationRecord railLocation, MutableRoute route, MutableTrip trip, int stopSequence,
                                         boolean lastStop, IdFor<Agency> agencyId, BasicSchedule schedule) {

            if (!isStation(railLocation)) {
                missingStationDiagnosticsFor(railLocation, agencyId, schedule);
                return false;
            }

            // Station
            MutableStation station = getStationFor(railLocation);
            station.addRoute(route);

            // Route Station
            RouteStation routeStation = new RouteStation(station, route);
            container.addRouteStation(routeStation);

            // Platform
            MutablePlatform platform = getOrCreatePlatform(station, railLocation);
            platform.addRoute(route);
            station.addPlatform(platform);

            final GTFSPickupDropoffType pickup = lastStop ? None : Regular;
            final GTFSPickupDropoffType dropoff = stopSequence==1 ? None : Regular;

            // Stop Call
            TramTime arrivalTime = railLocation.getPublicArrival();
            TramTime departureTime = railLocation.getPublicDeparture();
            StopCall stopCall = createStopCall(trip, station, platform, stopSequence,
                    arrivalTime, departureTime, pickup, dropoff);
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
            return container.hasStationId(stationId);
        }

        private MutableStation getStationFor(RailLocationRecord record) {
            final String tiplocCode = record.getTiplocCode();
            IdFor<Station> stationId = StringIdFor.createId(tiplocCode);
            MutableStation station;
            if (!container.hasStationId(stationId)) {
                    throw new RuntimeException(format("Missing stationid %s encountered for %s", stationId, record));
            } else {
                station = container.getMutableStation(stationId);
            }
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
            final IdFor<Agency> agencyId = StringIdFor.createId(atocCode);
            if (container.hasAgencyId(agencyId)) {
                mutableAgency = container.getMutableAgency(agencyId);
            } else {
                // todo get list of atoc names
                mutableAgency = new MutableAgency(DataSourceID.rail, agencyId, atocCode);
                container.addAgency(mutableAgency);
            }
            return mutableAgency;
        }

        private MutablePlatform getOrCreatePlatform(MutableStation originStation, RailLocationRecord originLocation) {
            String platformNumber = originLocation.getPlatform();
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

        private MutableTrip getOrCreateTrip(BasicSchedule schedule, MutableService service, MutableRoute route) {
            MutableTrip trip;
            IdFor<Trip> tripId = createTripIdFor(schedule);
            if (container.hasTripId(tripId)) {
                logger.info("Had existing tripId: " + tripId + " for " + schedule);
                trip = container.getMutableTrip(tripId);
            } else {
                trip = new MutableTrip(tripId, schedule.getTrainIdentity(), service, route);
                container.addTrip(trip);
            }
            return trip;
        }

        private IdFor<Trip> createTripIdFor(BasicSchedule schedule) {
            String startDate = schedule.getStartDate().format(dateFormatter);
            String endDate = schedule.getEndDate().format(dateFormatter);
            return StringIdFor.createId(format("%s:%s:%s", schedule.getUniqueTrainId(), startDate, endDate));
        }

        private MutableRoute getOrCreateRoute(OriginLocation originLocation, TerminatingLocation terminatingLocation,
                                              MutableAgency mutableAgency, String atocCode) {
            MutableRoute route;
            String shortName = createRouteShortName(originLocation, terminatingLocation, atocCode);
            IdFor<Route> routeId = StringIdFor.createId(shortName);
            if (container.hasRouteId(routeId)) {
                route = container.getMutableRoute(routeId);
            } else {
                String routeName = createRouteName(originLocation, terminatingLocation, atocCode);
                route = new MutableRoute(routeId, shortName, routeName, mutableAgency, TransportMode.Train);
                container.addRoute(route);
            }
            return route;
        }

        private MutableService getOrCreateService(BasicSchedule schedule) {
            MutableService service;
            final IdFor<Service> serviceId = getServiceIdFor(schedule);
            if (container.hasServiceId(serviceId)) {
                service = container.getMutableService(serviceId);
            } else {
                service = new MutableService(serviceId);
                ServiceCalendar calendar = new ServiceCalendar(schedule.getStartDate(), schedule.getEndDate(), schedule.getDaysOfWeek());
                service.setCalendar(calendar);
                container.addService(service);
            }
            return service;
        }

        private String createRouteName(OriginLocation originLocation, TerminatingLocation terminatingLocation, String atocCode) {
            final String startName = getNameForRoute(originLocation);
            final String endName = getNameForRoute(terminatingLocation);
            return format("%s service from %s to %s", atocCode, startName, endName);
        }

        private String getNameForRoute(RailLocationRecord originLocation) {
            String name;
            if (isStation(originLocation)) {
                Station start = getStationFor(originLocation);
                 name = start.getName();
            } else {
                final String tiplocCode = originLocation.getTiplocCode();
                if (tiplocInsertRecords.containsKey(tiplocCode)) {
                    name = tiplocInsertRecords.get(tiplocCode).getName();
                } else {
                    logger.warn("Cannot find name for " + tiplocCode);
                    name = tiplocCode;
                }
            }
            return name;
        }

        private String createRouteShortName(OriginLocation originLocation, TerminatingLocation terminatingLocation, String atocCode) {
            return format("%s:%s=>%s", atocCode, originLocation.getTiplocCode(), terminatingLocation.getTiplocCode());
        }

    }

    @NotNull
    private static IdFor<Service> getServiceIdFor(BasicSchedule schedule) {
        // need to use unique id otherwise no way to process cancellations since dates will not match
        return StringIdFor.createId(schedule.getUniqueTrainId());
//        String startDate = schedule.getStartDate().format(dateFormatter);
//        String endDate = schedule.getEndDate().format(dateFormatter);
//        return StringIdFor.createId(format("%s:%s:%s", schedule.getUniqueTrainId(), startDate, endDate));
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
