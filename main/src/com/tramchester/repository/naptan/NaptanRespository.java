package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaPTANDataImporter;
import com.tramchester.dataimport.NaPTAN.RailStationData;
import com.tramchester.dataimport.NaPTAN.StopsData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.HasGridPosition;
import com.tramchester.geo.MarginInMeters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class NaptanRespository {
    private static final Logger logger = LoggerFactory.getLogger(NaptanRespository.class);

    private final NaPTANDataImporter dataImporter;
    private final TramchesterConfig config;
    private Map<String, StopsData> stopData;
    private Map<String, String> tiplocToAtco;

    @Inject
    public NaptanRespository(NaPTANDataImporter dataImporter, TramchesterConfig config) {
        this.dataImporter = dataImporter;
        this.config = config;
        stopData = Collections.emptyMap();
        tiplocToAtco = Collections.emptyMap();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        if (!dataImporter.isEnabled()) {
            logger.info("Not enabled");
            return;
        } else {
            loadStopDataForConfiguredArea();
        }
        logger.info("started");
    }


    @PreDestroy
    public void stop() {
        logger.info("stopping");
        stopData.clear();
        logger.info("stopped");
    }

    private void loadStopDataForConfiguredArea() {
        BoundingBox bounds = config.getBounds();
        logger.info("Loading data for " + bounds);
        Double range = config.getNearestStopForWalkingRangeKM();

        MarginInMeters margin = MarginInMeters.of(range);

        loadStopsData(bounds, margin);
        loadStationData(bounds, margin);
    }

    private void loadStopsData(BoundingBox bounds, MarginInMeters margin) {
        Stream<StopsData> stopsData = dataImporter.getStopsData();
        stopData = filterBy(bounds, margin, stopsData).
                collect(Collectors.toMap(StopsData::getAtcoCode, Function.identity()));
        stopsData.close();

        logger.info("Loaded " + stopData.size() + " stops");
    }


    private void loadStationData(BoundingBox bounds, MarginInMeters margin) {
        Stream<RailStationData> stationDataStream = dataImporter.getRailStationData();
        tiplocToAtco = filterBy(bounds, margin, stationDataStream).
                collect(Collectors.toMap(RailStationData::getTiploc, RailStationData::getActo));
        stationDataStream.close();

        logger.info("Loaded " + tiplocToAtco.size() + " stations");
    }

    private <T extends HasGridPosition> Stream<T> filterBy(BoundingBox bounds, MarginInMeters margin, Stream<T> stream) {
        return stream.
                filter(item -> item.getGridPosition().isValid()).
                filter(item -> bounds.within(margin, item.getGridPosition()));
    }


    public boolean containsActo(IdFor<Station> actoCode) {
        return stopData.containsKey(actoCode.forDTO());
    }

    public StopsData getForActo(IdFor<Station> actoCode) {
        return stopData.get(actoCode.forDTO());
    }

    public boolean isEnabled() {
        return dataImporter.isEnabled();
    }

    public StopsData getForTiploc(IdFor<Station> tiploc) {
        String acto = tiplocToAtco.get(tiploc.forDTO());
        if (stopData.containsKey(acto)) {
            return stopData.get(acto);
        }
        return null;
    }

    public boolean containsTiploc(IdFor<Station> tiploc) {
        return tiplocToAtco.containsKey(tiploc.forDTO());
    }
}
