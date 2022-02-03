package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.CaseInsensitiveId;
import com.tramchester.domain.places.*;
import com.tramchester.repository.postcodes.PostcodeRepository;

import javax.inject.Inject;

@LazySingleton
public class LocationRepository {
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

    public Location<?> getLocation(LocationType type, String rawId) {
        return switch (type) {
            case Station -> stationRepository.getStationById(Station.createId(rawId));
            case Platform -> platformRepository.getPlatformById(Platform.createId(rawId));
            case StationGroup -> stationGroupsRepository.getStationGroup(NaptanArea.createId(rawId));
            case Postcode -> postcodeRepository.getPostcode(CaseInsensitiveId.createIdFor(rawId));
            case MyLocation -> MyLocation.parseFromId(rawId);
        };
    }
}
