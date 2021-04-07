package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaPTANDataImporter;
import com.tramchester.dataimport.NaPTAN.StopsData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
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

    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        BoundingBox bounds = config.getBounds();
        Double range = config.getNearestStopForWalkingRangeKM();

        long margin = Math.round(range*1000D);
        Stream<StopsData> dataStream = dataImporter.getAll();
        stopData = dataStream.
                filter(item -> item.getGridPosition().isValid()).
                filter(item -> bounds.within(margin, item.getGridPosition())).
                collect(Collectors.toMap(StopsData::getAtcoCode, Function.identity()));
        dataStream.close();
        logger.info("Loaded " + stopData.size() + " stops");

        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        stopData.clear();
        logger.info("stopped");
    }

    public boolean contains(IdFor<Station> actoCode) {
        return stopData.containsKey(actoCode.forDTO());
    }

    public StopsData get(IdFor<Station> actoCode) {
        return stopData.get(actoCode.forDTO());
    }
}
