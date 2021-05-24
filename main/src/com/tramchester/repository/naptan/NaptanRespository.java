package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaPTANDataImporter;
import com.tramchester.dataimport.NaPTAN.StopsData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.BoundingBox;
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

    @Inject
    public NaptanRespository(NaPTANDataImporter dataImporter, TramchesterConfig config) {
        this.dataImporter = dataImporter;
        this.config = config;
        stopData = Collections.emptyMap();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        if (!dataImporter.isEnabled()) {
            logger.info("Not enabled");
            return;
        } else {
            loadDataForConfiguredArea();
        }
        logger.info("started");
    }


    @PreDestroy
    public void stop() {
        logger.info("stopping");
        stopData.clear();
        logger.info("stopped");
    }

    private void loadDataForConfiguredArea() {
        BoundingBox bounds = config.getBounds();
        logger.info("Loading data for " + bounds);
        Double range = config.getNearestStopForWalkingRangeKM();

        MarginInMeters margin = MarginInMeters.of(range);
        Stream<StopsData> dataStream = dataImporter.getAll();
        stopData = dataStream.
                filter(item -> item.getGridPosition().isValid()).
                filter(item -> bounds.within(margin, item.getGridPosition())).
                collect(Collectors.toMap(StopsData::getAtcoCode, Function.identity()));
        dataStream.close();
        logger.info("Loaded " + stopData.size() + " stops");
    }


    public boolean contains(IdFor<Station> actoCode) {
        return stopData.containsKey(actoCode.forDTO());
    }

    public StopsData get(IdFor<Station> actoCode) {
        return stopData.get(actoCode.forDTO());
    }

    public boolean isEnabled() {
        return dataImporter.isEnabled();
    }
}
