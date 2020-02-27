package com.tramchester.repository;

import com.tramchester.domain.Station;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InterchangeRepository {
    private final TransportDataSource dataSource;

    public InterchangeRepository(TransportDataSource dataSource) {

        this.dataSource = dataSource;
    }

    // sorted by agency numbers
    public List<Station> findAgencyInterchanges(int numberAgencies) {

        Set<Station> allStations = dataSource.getStations();
        return allStations.stream().
                filter(station -> !station.isTram()).
                filter(station -> station.getAgencies().size()>=numberAgencies).
                sorted(Comparator.comparingInt(a -> a.getAgencies().size())).
                collect(Collectors.toList());
    }
}
