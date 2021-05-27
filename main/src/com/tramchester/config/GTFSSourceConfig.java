package com.tramchester.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@JsonDeserialize(as= GTFSSourceAppConfig.class)
public interface GTFSSourceConfig extends HasDataPath {

    // name for diag, logging and entity factory selection
    String getName();

    // expect to see feedinfo.txt for this data set
    boolean getHasFeedInfo();

    // transport modes to include from this dataset
    Set<GTFSTransportationType> getTransportGTFSModes();

    // transport modes for this datasource that have platform data included
    Set<TransportMode> getTransportModesWithPlatforms();

    // no service dates
    // basically a workaround for tfgm timetable including services for dates their website says there are no services..
    Set<LocalDate> getNoServices();

    // additional interchanges
    // interchange to add to those auto discovered by the interchange repository
    Set<String> getAdditionalInterchanges();

    default Set<TransportMode> getTransportModes() {
        return getTransportGTFSModes().stream().
                map(TransportMode::fromGTFS).
                collect(Collectors.toSet());
    }

}
