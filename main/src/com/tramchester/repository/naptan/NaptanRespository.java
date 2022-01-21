package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaptanRailReferecnesDataImporter;
import com.tramchester.dataimport.NaPTAN.NaptanStopData;
import com.tramchester.dataimport.NaPTAN.NaptanStopsDataImporter;
import com.tramchester.dataimport.NaPTAN.RailStationData;
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

// http://naptan.dft.gov.uk/naptan/schema/2.5/doc/NaPTANSchemaGuide-2.5-v0.67.pdf

@LazySingleton
public class NaptanRespository {
    private static final Logger logger = LoggerFactory.getLogger(NaptanRespository.class);

    private final NaptanStopsDataImporter stopsImporter;
    private final NaptanRailReferecnesDataImporter referecnesDataImporter;
    private final TramchesterConfig config;
    private Map<String, NaptanStopData> stopData;
    private Map<String, String> tiplocToAtco;

    @Inject
    public NaptanRespository(NaptanStopsDataImporter stopsImporter, NaptanRailReferecnesDataImporter referecnesDataImporter,
                             TramchesterConfig config) {
        this.stopsImporter = stopsImporter;
        this.referecnesDataImporter = referecnesDataImporter;
        this.config = config;
        stopData = Collections.emptyMap();
        tiplocToAtco = Collections.emptyMap();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        boolean eitherEnabled = stopsImporter.isEnabled() || referecnesDataImporter.isEnabled();

        if (!eitherEnabled) {
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
        tiplocToAtco.clear();
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
        if (!stopsImporter.isEnabled()) {
            logger.error("Not loading stops data, import is disabled. Is this data source present in the config?");
        }
        Stream<NaptanStopData> stopsData = stopsImporter.getStopsData();
        stopData = filterBy(bounds, margin, stopsData).
                collect(Collectors.toMap(NaptanStopData::getAtcoCode, Function.identity()));
        stopsData.close();

        logger.info("Loaded " + stopData.size() + " stops");
    }


    private void loadStationData(BoundingBox bounds, MarginInMeters margin) {
        if (!referecnesDataImporter.isEnabled()) {
            logger.warn("Not rail station data, import is disabled. Is this data source present in the config?");
        }
        Stream<RailStationData> stationDataStream = referecnesDataImporter.getRailStationData();
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

    public NaptanStopData getForActo(IdFor<Station> actoCode) {
        return stopData.get(actoCode.forDTO());
    }

    public boolean isEnabled() {
        return stopsImporter.isEnabled();
    }

    /***
     * Look up via train location code
     * @param tiploc the code for the station
     * @return data if present, null otherwise
     */
    public NaptanStopData getForTiploc(IdFor<Station> tiploc) {
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
