package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.BasicScheduleExtraDetails;
import com.tramchester.dataimport.rail.records.RailLocationRecord;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
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
    private final List<RailLocationRecord> locations;
    private final Set<RailRouteIdRepository.AgencyCallingPoints> agencyCallingPoints;

    public ExtractAgencyCallingPointsFromLocationRecords() {
        currentAtocCode = "";
        agencyCallingPoints = new HashSet<>();
        locations = new ArrayList<>();
    }

    public static Set<RailRouteIdRepository.AgencyCallingPoints> loadCallingPoints(ProvidesRailTimetableRecords providesRailTimetableRecords) {

        logger.info("Begin extraction of calling points from " + providesRailTimetableRecords.toString());
        ExtractAgencyCallingPointsFromLocationRecords extractor = new ExtractAgencyCallingPointsFromLocationRecords();

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
                collect(Collectors.toList());

        IdFor<Agency> agencyId = Agency.createId(atocCode);

        // calling points filtered by bounds, so only add the valid ones
        if (callingPoints.size()>1) {
            agencyCallingPoints.add(new RailRouteIdRepository.AgencyCallingPoints(agencyId, callingPoints));
        }

    }



}
