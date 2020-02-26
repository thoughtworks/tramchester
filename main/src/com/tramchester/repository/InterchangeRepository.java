package com.tramchester.repository;

import com.tramchester.domain.Station;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InterchangeRepository {
    private final TransportDataSource dataSource;

    public InterchangeRepository(TransportDataSource dataSource) {

        this.dataSource = dataSource;
    }

    public List<Station> findAgencyInterchanges() {

        Set<Station> allStations = dataSource.getStations();
        return allStations.stream().filter(station -> station.getAgencies().size()>1).collect(Collectors.toList());
    }
}
