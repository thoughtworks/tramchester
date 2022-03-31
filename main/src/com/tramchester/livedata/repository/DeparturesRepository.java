package com.tramchester.livedata.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.repository.StationGroupsRepository;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@LazySingleton
public class DeparturesRepository {

    private final StationLocationsRepository stationLocationsRepository;
    private final DueTramsSource dueTramsSource;
    private final TramchesterConfig config;

    @Inject
    public DeparturesRepository(StationLocationsRepository stationLocationsRepository, DueTramsSource dueTramsSource, TramchesterConfig config, StationGroupsRepository stationGroupsRepository) {
        this.stationLocationsRepository = stationLocationsRepository;
        this.dueTramsSource = dueTramsSource;
        this.config = config;
    }

    public List<DueTram> dueTramsForLocation(Location<?> location, LocalDate date, TramTime time) {
        return switch (location.getLocationType()) {
            case Station -> getStationDepartures((Station) location, date, time);
            case StationGroup -> getStationGroupDepartures((StationGroup) location, date, time);
            case MyLocation, Postcode -> getDeparturesNearTo(location, date, time);
            case Platform -> getPlatformDepatrues((Platform) location, date, time);
        };
    }

    private List<DueTram> getPlatformDepatrues(Platform platform, LocalDate date, TramTime time) {
        Optional<PlatformDueTrams> platformDue = dueTramsSource.dueTramsForPlatform(platform.getId(), date, time);
        if (platformDue.isEmpty()) {
            return Collections.emptyList();
        }
        return platformDue.get().getDueTrams().stream().distinct().collect(Collectors.toList());
    }

    private List<DueTram> getDeparturesNearTo(Location<?> location, LocalDate date, TramTime time) {
        final MarginInMeters margin = MarginInMeters.of(config.getNearestStopRangeKM());
        final int numOfNearestStopsToOffer = config.getNumOfNearestStopsToOffer();

        List<Station> nearbyStations = stationLocationsRepository.nearestStationsSorted(location,
                numOfNearestStopsToOffer, margin);

        return nearbyStations.stream().
                flatMap(station -> getStationDepartures(station, date, time).stream()).
                distinct().
                collect(Collectors.toList());
    }

    private List<DueTram> getStationGroupDepartures(StationGroup stationGroup, LocalDate date, TramTime time) {
        return stationGroup.getContained().stream().
                flatMap(station -> getStationDepartures(station, date, time).stream()).
                distinct().collect(Collectors.toList());

    }

    private List<DueTram> getStationDepartures(Station station, LocalDate date, TramTime time) {
        return dueTramsSource.dueTramsForStation(station, date, time);
    }
}
