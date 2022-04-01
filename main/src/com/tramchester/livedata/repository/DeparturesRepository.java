package com.tramchester.livedata.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.repository.StationGroupsRepository;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class DeparturesRepository {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesRepository.class);

    private final StationLocationsRepository stationLocationsRepository;
    private final DueTramsSource dueTramsSource;
    private final TramchesterConfig config;

    @Inject
    public DeparturesRepository(StationLocationsRepository stationLocationsRepository, DueTramsSource dueTramsSource, TramchesterConfig config,
                                StationGroupsRepository stationGroupsRepository) {
        this.stationLocationsRepository = stationLocationsRepository;
        this.dueTramsSource = dueTramsSource;
        this.config = config;
    }

    public List<DueTram> dueTramsForLocation(Location<?> location, LocalDate date, TramTime time, Set<TransportMode> modes) {
        return switch (location.getLocationType()) {
            case Station -> getStationDepartures((Station) location, date, time, modes);
            case StationGroup -> getStationGroupDepartures((StationGroup) location, date, time, modes);
            case MyLocation, Postcode -> getDeparturesNearTo(location, date, time, modes);
            case Platform -> getPlatformDepartrues((Platform) location, date, time, modes);
        };
    }

    private List<DueTram> getPlatformDepartrues(Platform platform, LocalDate date, TramTime time, Set<TransportMode> modes) {
        if (!TransportMode.intersects(modes, platform.getTransportModes())) {
            logger.error(format("Platform %s does not match supplied modes %s", platform, modes));
        }
        Optional<PlatformDueTrams> platformDue = dueTramsSource.dueTramsForPlatform(platform.getId(), date, time);
        if (platformDue.isEmpty()) {
            return Collections.emptyList();
        }
        return platformDue.get().getDueTrams().stream().distinct().collect(Collectors.toList());
    }

    private List<DueTram> getDeparturesNearTo(Location<?> location, LocalDate date, TramTime time, Set<TransportMode> modes) {
        final MarginInMeters margin = MarginInMeters.of(config.getNearestStopRangeKM());
        final int numOfNearestStopsToOffer = config.getNumOfNearestStopsToOffer();

        List<Station> nearbyStations = stationLocationsRepository.nearestStationsSorted(location, numOfNearestStopsToOffer,
                margin, modes);

        return nearbyStations.stream().
                flatMap(station -> getStationDepartures(station, date, time, modes).stream()).
                distinct().
                collect(Collectors.toList());
    }

    private List<DueTram> getStationGroupDepartures(StationGroup stationGroup, LocalDate date, TramTime time, Set<TransportMode> modes) {
        return stationGroup.getContained().stream().
                filter(station -> TransportMode.intersects(station.getTransportModes(), modes)).
                flatMap(station -> getStationDepartures(station, date, time, modes).stream()).
                distinct().collect(Collectors.toList());

    }

    private List<DueTram> getStationDepartures(Station station, LocalDate date, TramTime time, Set<TransportMode> modes) {
        SetUtils.SetView<TransportMode> toFetch = SetUtils.intersection(station.getTransportModes(), modes);
        if (toFetch.isEmpty()) {
            logger.error(format("Station modes %s and filter modes %s do not overlap", station, modes));
        }

        return toFetch.stream().
                flatMap(mode -> getDeparturesFor(mode, station, date, time)).
                collect(Collectors.toList());
    }

    private Stream<DueTram> getDeparturesFor(TransportMode mode, Station station, LocalDate date, TramTime time) {
        return switch (mode) {
            case Tram -> dueTramsSource.dueTramsForStation(station, date, time).stream();
            default -> {
                final String msg = "TODO - live data for " + mode + " is not implemented yet";
                logger.info(msg);
                throw new RuntimeException(msg); }

        };
    }
}
