package com.tramchester.dataimport.loader;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

public class StopDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(StopDataLoader.class);

    private final TransportEntityFactory factory;
    private final TramchesterConfig config;

    public StopDataLoader(TransportEntityFactory factory, TramchesterConfig config) {
        this.factory = factory;
        this.config = config;
    }

    public PreloadedStationsAndPlatforms load(Stream<StopData> stops) {
        logger.info("Loading stops within bounds");
        BoundingBox bounds = config.getBounds();

        PreloadedStationsAndPlatforms allStations = new PreloadedStationsAndPlatforms(factory);

        stops.forEach((stopData) -> {
            LatLong latLong = stopData.getLatLong();
            if (latLong.isValid()) {
                GridPosition position = getGridPosition(stopData.getLatLong());
                if (bounds.contained(position)) {
                    preLoadStation(allStations, stopData, factory);
                } else {
                    // Don't know which transport modes the station serves at this stage, so no way to filter further
                    logger.info("Excluding stop outside of bounds" + stopData);
                }
            } else {
                preLoadStation(allStations, stopData, factory);
            }
        });
        logger.info("Pre Loaded " + allStations.size() + " stations");
        return allStations;
    }

    private void preLoadStation(PreloadedStationsAndPlatforms allStations, StopData stopData,TransportEntityFactory factory) {
        String stopId = stopData.getId();

        IdFor<Station> stationId = factory.formStationId(stopId);

        if (allStations.hasId(stationId)) {
            allStations.updateStation(stationId, stopData);
        } else {
            allStations.createAndAdd(stationId, stopData);
        }
    }

    private GridPosition getGridPosition(LatLong latLong) {
        return CoordinateTransforms.getGridPosition(latLong);
    }
}
