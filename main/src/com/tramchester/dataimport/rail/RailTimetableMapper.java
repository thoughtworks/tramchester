package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.PlatformStopCall;
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
import java.util.*;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.None;
import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static java.lang.String.format;

public class RailTimetableMapper {
    private static final Logger logger = LoggerFactory.getLogger(RailTimetableMapper.class);

    private State currentState;
    private CurrentSchedule currentSchedule;
    private final CreatesTransportDataForRail processor;
    private final Map<String,TIPLOCInsert> tiplocInsertRecords;
    private final Map<String, Set<RailLocationRecord>> missingStations;

    public RailTimetableMapper(TransportDataContainer container) {
        currentState = State.Between;
        tiplocInsertRecords = new HashMap<>();
        missingStations = new HashMap<>();
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
        currentSchedule.addScheduleExtra(record);
        currentState = State.SeenScheduleExtra;
    }

    private void seenIntermediate(RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        currentSchedule.addIntermediate(record);
    }

    private void seenOrigin(RailTimetableRecord record) {
        guardState(State.SeenScheduleExtra, record);
        currentSchedule.addOrigin(record);
        currentState = State.SeenOrigin;
    }

    private void seenEnd(RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        currentSchedule.finish(record);
        processor.consume(currentSchedule);
        currentState = State.Between;
    }

    private void seenBegin(RailTimetableRecord record) {
        BasicSchedule basicSchedule = (BasicSchedule) record;
        guardState(State.Between, record);

        currentSchedule = new CurrentSchedule(basicSchedule);
        if (basicSchedule.getSTPIndicator()== BasicSchedule.ShortTermPlanIndicator.Cancellation) {
            processor.recordCancellations(basicSchedule);
            currentState = State.Between;
        } else {
            currentState = State.SeenSchedule;
        }
    }

    public void reportDiagnostics() {
        if (missingStations.isEmpty()) {
            return;
        }
        missingStations.forEach((tiploc, records) -> {
            logger.warn("Missing tiploc " + tiploc + " for records " + records);
            if (tiplocInsertRecords.containsKey(tiploc)) {
                logger.info("Found TI record: " + tiplocInsertRecords.get(tiploc));
            }
        });
    }

    private enum State {
        SeenSchedule,
        SeenScheduleExtra,
        SeenOrigin,
        Between
    }

    private void guardState(State expectedState, RailTimetableRecord record) {
        if (currentState != expectedState) {
            throw new RuntimeException(format("Expected state %s not %s at %s", expectedState, currentState, record));
        }
    }

    private static class CurrentSchedule {

        private final BasicSchedule basicScheduleRecord;
        private final List<IntermediateLocation> intermediateLocations;
        private OriginLocation originLocation;
        private TerminatingLocation terminatingLocation;
        private BasicScheduleExtraDetails extraDetails;

        public CurrentSchedule(RailTimetableRecord basicScheduleRecord) {
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
        private final Map<String, Set<RailLocationRecord>> missingStations;

        private CreatesTransportDataForRail(TransportDataContainer container, Map<String, TIPLOCInsert> tiplocInsertRecords,
                                            Map<String, Set<RailLocationRecord>> missingStations) {
            this.container = container;
            this.tiplocInsertRecords = tiplocInsertRecords;
            this.missingStations = missingStations;
        }

        public void consume(CurrentSchedule currentSchedule) {
            BasicSchedule basicSchedule = currentSchedule.basicScheduleRecord;

            switch (basicSchedule.getTransactionType()) {
                case N -> createNew(currentSchedule);
                case D -> delete(currentSchedule.basicScheduleRecord);
                case R -> revise(currentSchedule);
                case Unknown -> logger.warn("Unknown transaction type for " + currentSchedule.basicScheduleRecord);
            }

        }

        private void delete(BasicSchedule basicScheduleRecord) {
            logger.info("Delete schedule " + basicScheduleRecord);
        }

        private void revise(CurrentSchedule currentSchedule) {
            logger.info("Revise schedule " + currentSchedule);
        }

        private void recordCancellations(BasicSchedule basicSchedule) {
            final IdFor<Service> serviceId = StringIdFor.createId(basicSchedule.getUniqueTrainId());
            if (container.hasServiceId(serviceId)) {
                logger.debug("Making cancellations for schedule " + basicSchedule.getUniqueTrainId());
                Service service = container.getMutableService(serviceId);
                ServiceCalendar calendar = service.getCalendar();
                addServiceExceptions(calendar, basicSchedule.getStartDate(), basicSchedule.getEndDate(), basicSchedule.getDaysOfWeek());
            } else {
                logger.error("Failed to find service to amend for " + basicSchedule);
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

        private void createNew(CurrentSchedule currentSchedule) {
            final BasicSchedule schedule = currentSchedule.basicScheduleRecord;
            final OriginLocation originLocation = currentSchedule.originLocation;
            final List<IntermediateLocation> intermediateLocations = currentSchedule.intermediateLocations;
            final TerminatingLocation terminatingLocation = currentSchedule.terminatingLocation;

            logger.debug("Create schedule for " + schedule.getUniqueTrainId());
            final String atocCode = currentSchedule.extraDetails.getAtocCode();

            // Agency
            MutableAgency mutableAgency = getOrCreateAgency(atocCode);

            // Service
            MutableService service = getOrCreateService(currentSchedule, schedule);

            // Route
            MutableRoute route = getOrCreateRoute(currentSchedule, originLocation, mutableAgency, atocCode);
            route.addService(service);
            mutableAgency.addRoute(route);

            // Trip
            MutableTrip trip = getOrCreateTrip(schedule, service, route);
            route.addTrip(trip);

            // Stations, Platforms, StopCalls
            int stopSequence = 1;
            populateForLocation(originLocation, route, trip, stopSequence, false);
            stopSequence = stopSequence + 1;
            for (int i = 0; i < intermediateLocations.size(); i++) {
                populateForLocation(intermediateLocations.get(i), route, trip, stopSequence+i, false);
            }
            stopSequence = stopSequence + intermediateLocations.size();
            populateForLocation(terminatingLocation, route, trip, stopSequence, true);

        }

        private void populateForLocation(RailLocationRecord railLocation, MutableRoute route, MutableTrip trip, int stopSequence,
                                         boolean lastStop) {

            if (!isStation(railLocation)) {
                missingStationDiagnosticsFor(railLocation);
                return;
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
            TramTime departureTime = railLocation.getPublicDeparture(); // same arrive & depart for first stop
            PlatformStopCall stopCall = createStopCall(trip, station, platform, stopSequence,
                    arrivalTime, departureTime, pickup, dropoff);
            trip.addStop(stopCall);
        }

        private void missingStationDiagnosticsFor(RailLocationRecord railLocation) {
            final RailRecordType recordType = railLocation.getRecordType();
            boolean intermediate = (recordType==RailRecordType.IntermediateLocation);
            if (intermediate) {
                return;
            }

            String tiplocCode = railLocation.getTiplocCode();
            if (!missingStations.containsKey(tiplocCode)) {
                missingStations.put(tiplocCode, new HashSet<>());
            }
            missingStations.get(tiplocCode).add(railLocation);
//            boolean warn = (recordType==RailRecordType.TerminatingLocation) || (recordType==RailRecordType.OriginLocation);
//            String message = "Tiploc '" + railLocation.getTiplocCode() + "' was not a station at " + railLocation;
//            if (warn) {
//                logger.warn(message);
//            } else {
//                // passing points etc
//                logger.debug(message);
//            }
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
        private PlatformStopCall createStopCall(MutableTrip trip, MutableStation station,
                                                MutablePlatform platform, int stopSequence, TramTime arrivalTime,
                                                TramTime departureTime, GTFSPickupDropoffType pickup, GTFSPickupDropoffType dropoff) {
            StopTimeData stopTimeData = new StopTimeData(trip.getId(), arrivalTime, departureTime, station.getId(), stopSequence, pickup, dropoff);
            return new PlatformStopCall(trip, platform, station, stopTimeData);
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
            String platformText = originLocation.getPlatform();
            final String platformIdText = originLocation.getTiplocCode() + "_" + platformText;
            IdFor<Platform> platformId = StringIdFor.createId(platformIdText);
            MutablePlatform platform;
            if (container.hasPlatformId(platformId)) {
                platform = container.getMutablePlatform(platformId);
            } else {
                platform = new MutablePlatform(platformIdText, originStation.getName() + " Platform " + platformText,
                        originStation.getLatLong());
                container.addPlatform(platform);
            }

            return platform;
        }

        private MutableTrip getOrCreateTrip(BasicSchedule schedule, MutableService service, MutableRoute route) {
            MutableTrip trip;
            IdFor<Trip> tripId = StringIdFor.createId(schedule.getUniqueTrainId());
            if (container.hasTripId(tripId)) {
                trip = container.getMutableTrip(tripId);
            } else {
                trip = new MutableTrip(tripId, schedule.getTrainIdentity(), service, route);
                container.addTrip(trip);
            }
            return trip;
        }

        private MutableRoute getOrCreateRoute(CurrentSchedule currentSchedule, OriginLocation originLocation, MutableAgency mutableAgency, String atocCode) {
            MutableRoute route;
            String shortName = createRouteShortName(originLocation, currentSchedule.terminatingLocation,
                    atocCode);
            IdFor<Route> routeId = StringIdFor.createId(shortName);
            if (container.hasRouteId(routeId)) {
                route = container.getMutableRoute(routeId);
            } else {
                String routeName = createRouteName(originLocation, currentSchedule.terminatingLocation,
                        atocCode);
                route = new MutableRoute(routeId, shortName, routeName, mutableAgency, TransportMode.Train);
                container.addRoute(route);
            }
            return route;
        }

        private MutableService getOrCreateService(CurrentSchedule currentSchedule, BasicSchedule schedule) {
            MutableService service;
            //final IdFor<Service> serviceId = StringIdFor.createId(currentSchedule.extraDetails.getRetailServiceID());
            final IdFor<Service> serviceId = StringIdFor.createId(schedule.getUniqueTrainId());
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
            return format("%s_%s_%s", atocCode, originLocation.getTiplocCode(), terminatingLocation.getTiplocCode());
        }

    }
}
