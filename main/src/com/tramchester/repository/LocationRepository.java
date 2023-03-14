package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.*;
import com.tramchester.repository.postcodes.PostcodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@LazySingleton
public class LocationRepository {
    //private static final Logger logger = LoggerFactory.getLogger(LocationRepository.class);

    private final StationRepository stationRepository;
    private final StationGroupsRepository stationGroupsRepository;
    private final PostcodeRepository postcodeRepository;
    private final PlatformRepository platformRepository;

    @Inject
    public LocationRepository(StationRepository stationRepository, StationGroupsRepository stationGroupsRepository,
                              PostcodeRepository postcodeRepository, PlatformRepository platformRepository) {
        this.stationRepository = stationRepository;
        this.stationGroupsRepository = stationGroupsRepository;
        this.postcodeRepository = postcodeRepository;
        this.platformRepository = platformRepository;
    }

    @Deprecated
    public Location<?> getLocation(LocationType type, String rawId) {
        return switch (type) {
            case Station -> stationRepository.getStationById(Station.createId(rawId));
            case Platform -> throw new RuntimeException("Not supported yet"); //platformRepository.getPlatformById(Platform.createId(rawId));
            case StationGroup -> stationGroupsRepository.getStationGroup(NaptanArea.createId(rawId));
            case Postcode -> postcodeRepository.getPostcode(PostcodeLocation.createId(rawId));
            case MyLocation -> MyLocation.parseFromId(rawId);
        };
    }

    public Location<?> getLocation(LocationType type, IdForDTO idForDTO) {
        String rawId = idForDTO.getActualId();
        return switch (type) {
            case Station -> stationRepository.getStationById(Station.createId(rawId));
            case Platform -> throw new RuntimeException("Not supported yet"); //platformRepository.getPlatformById(Platform.createId(rawId));
            case StationGroup -> stationGroupsRepository.getStationGroup(NaptanArea.createId(rawId));
            case Postcode -> postcodeRepository.getPostcode(PostcodeLocation.createId(rawId));
            case MyLocation -> MyLocation.parseFromId(rawId);
        };
    }
}
