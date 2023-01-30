package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.BasicScheduleExtraDetails;
import com.tramchester.dataimport.rail.records.RailLocationRecord;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.BoundingBox;
import com.tramchester.repository.naptan.NaptanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * Supports route id creation by preloading calling points for each service in the timetable into a
 * set of calling point records
 */
public class ExtractAgencyCallingPointsFromLocationRecords {
    private static final Logger logger = LoggerFactory.getLogger(ExtractAgencyCallingPointsFromLocationRecords.class);

    private String currentAtocCode;
    private final NaptanRepository naptanRepository;
    private final BoundingBox bounds;
    private final List<RailLocationRecord> locations;
    private final Set<RailRouteIdRepository.AgencyCallingPoints> agencyCallingPoints;

    public ExtractAgencyCallingPointsFromLocationRecords(NaptanRepository naptanRepository, BoundingBox bounds) {
        this.naptanRepository = naptanRepository;
        this.bounds = bounds;
        currentAtocCode = "";
        agencyCallingPoints = new HashSet<>();
        locations = new ArrayList<>();
    }

    public static Set<RailRouteIdRepository.AgencyCallingPoints> loadCallingPoints(ProvidesRailTimetableRecords providesRailTimetableRecords,
                                                                                   NaptanRepository naptanRepository, BoundingBox bounds) {

        logger.info("Begin extraction of calling points from " + providesRailTimetableRecords.toString());
        ExtractAgencyCallingPointsFromLocationRecords extractor = new ExtractAgencyCallingPointsFromLocationRecords(naptanRepository, bounds);

        Stream<RailTimetableRecord> records = providesRailTimetableRecords.load();
        records.forEach(extractor::processRecord);

        logger.info("Finished extraction, loaded " + extractor.agencyCallingPoints.size() + " unique agency calling points records");
        return extractor.agencyCallingPoints;
    }

    private void processRecord(RailTimetableRecord record) {
        switch (record.getRecordType()) {
            case BasicScheduleExtra -> seenBegin(record);
            case TerminatingLocation -> seenEnd(record);
            case OriginLocation, IntermediateLocation -> seenLocation(record);
        }
    }

    private void seenBegin(RailTimetableRecord record) {
        if (!currentAtocCode.isEmpty()) {
            throw new RuntimeException("Unexpected state, was still processing for " + currentAtocCode + " at " + record);
        }

        BasicScheduleExtraDetails extraDetails = (BasicScheduleExtraDetails) record;
        currentAtocCode = extraDetails.getAtocCode();
    }

    private void seenLocation(RailTimetableRecord record) {
        RailLocationRecord locationRecord = (RailLocationRecord) record;
        if (LocationActivityCode.doesStop(((RailLocationRecord) record).getActivity())) {
            locations.add(locationRecord);
        }
    }

    private void seenEnd(RailTimetableRecord record) {
        RailLocationRecord locationRecord = (RailLocationRecord) record;
        locations.add(locationRecord);
        createAgencyCallingPoints();
        currentAtocCode = "";
        locations.clear();
    }

    private void createAgencyCallingPoints() {
        String atocCode = currentAtocCode;
        List<IdFor<Station>> callingPoints = locations.stream().
                filter(RailLocationRecord::doesStop).
                map(RailLocationRecord::getTiplocCode).
                map(Station::createId).
                filter(this::withinBounds).
                collect(Collectors.toList());

        IdFor<Agency> agencyId = Agency.createId(atocCode);

        // calling points filtered by bounds, so only add the valid ones
        if (callingPoints.size()>1) {
            agencyCallingPoints.add(new RailRouteIdRepository.AgencyCallingPoints(agencyId, callingPoints));
        }

    }

    private boolean withinBounds(IdFor<Station> stationId) {
        if (!naptanRepository.containsTiploc(stationId)) {
            return false;
        }
        NaptanRecord record = naptanRepository.getForTiploc(stationId);
        if (record.getGridPosition().isValid()) {
            return bounds.contained(record.getGridPosition());
        }
        return false;
    }


}
