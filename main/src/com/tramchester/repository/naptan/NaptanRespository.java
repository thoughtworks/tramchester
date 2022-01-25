package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaptanStopsDataImporter;
import com.tramchester.dataimport.NaPTAN.xml.NaptanStopData;
import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.HasGridPosition;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.repository.nptg.NPTGRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

// http://naptan.dft.gov.uk/naptan/schema/2.5/doc/NaPTANSchemaGuide-2.5-v0.67.pdf

@LazySingleton
public class NaptanRespository {
    private static final Logger logger = LoggerFactory.getLogger(NaptanRespository.class);

    private final NaptanStopsDataImporter stopsImporter;
    private final NPTGRepository nptgRepository;
    private final TramchesterConfig config;
    private IdMap<NaptanRecord> stopData;
    private Map<IdFor<Station>, IdFor<NaptanRecord>> tiplocToAtco;

    @Inject
    public NaptanRespository(NaptanStopsDataImporter stopsImporter, NPTGRepository nptgRepository, TramchesterConfig config) {
        this.stopsImporter = stopsImporter;
        this.nptgRepository = nptgRepository;
        this.config = config;
        stopData = new IdMap<>();
        tiplocToAtco = Collections.emptyMap();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        boolean enabled = stopsImporter.isEnabled();

        if (!enabled) {
            logger.warn("Not enabled, imported is disable, no config for naptan?");
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

        Stream<NaptanStopData> stopsData = stopsImporter.getStopsData().
                filter(stopData -> stopData.getAtcoCode() != null).
                filter(stopData -> !stopData.getAtcoCode().isBlank());

        stopData = filterBy(bounds, margin, stopsData).
                map(this::createRecord).
                collect(IdMap.collector());

        stopsData.close();

        logger.info("Loaded " + stopData.size() + " stops");
    }

    private NaptanRecord createRecord(NaptanStopData original) {
        IdFor<NaptanRecord> id = StringIdFor.createId(original.getAtcoCode());

        String suburb = original.getSuburb();
        String town = original.getTown();

        final String nptgLocality = original.getNptgLocality();
        if (nptgRepository.hasNptgCode(nptgLocality)) {
            NPTGData extra = nptgRepository.getByNptgCode(nptgLocality);
            if (suburb.isBlank()) {
                suburb = extra.getLocalityName();
            }
            if (town.isBlank()) {
                town = extra.getQualifierName();
            }
        } else {
            logger.warn(format("Missing NptgLocalityRef '%s' for naptan acto '%s", nptgLocality, id));
        }

        return new NaptanRecord(id, original.getCommonName(), original.getGridPosition(), suburb, town, original.getStopType());
    }

    private void loadStationData(BoundingBox bounds, MarginInMeters margin) {
        logger.info("Load rail station reference data from natpan stops");

        Stream<NaptanStopData> railStops = stopsImporter.getStopsData().
                filter(NaptanStopData::hasRailInfo).
                filter(stopData -> stopData.getAtcoCode() != null).
                filter(stopData -> !stopData.getAtcoCode().isBlank());

        tiplocToAtco = filterBy(bounds, margin, railStops)
                .collect(Collectors.toMap(data -> StringIdFor.createId(data.getRailInfo().getTiploc()),
                        data -> StringIdFor.createId(data.getAtcoCode())));
        logger.info("Loaded " + tiplocToAtco.size() + " stations");
    }

    private <T extends HasGridPosition> Stream<T> filterBy(BoundingBox bounds, MarginInMeters margin, Stream<T> stream) {
        return stream.
                filter(item -> item.getGridPosition().isValid()).
                filter(item -> bounds.within(margin, item.getGridPosition()));
    }

    public boolean containsActo(IdFor<Station> actoCode) {
        IdFor<NaptanRecord> id = convertId(actoCode);
        return stopData.hasId(id);
    }

    public NaptanRecord getForActo(IdFor<Station> actoCode) {
        IdFor<NaptanRecord> id = convertId(actoCode);
        return stopData.get(id);
    }

    private IdFor<NaptanRecord> convertId(IdFor<Station> actoCode) {
        return StringIdFor.convert(actoCode);
    }

    public boolean isEnabled() {
        return stopsImporter.isEnabled();
    }

    /***
     * Look up via train location code
     * @param railStationTiploc the code for the station
     * @return data if present, null otherwise
     */
    public NaptanRecord getForTiploc(IdFor<Station> railStationTiploc) {
        if (!tiplocToAtco.containsKey(railStationTiploc)) {
            return null;
        }
        IdFor<NaptanRecord> acto = tiplocToAtco.get(railStationTiploc);
        if (stopData.hasId(acto)) {
            return stopData.get(acto);
        }
        return null;
    }

    public boolean containsTiploc(IdFor<Station> tiploc) {
        return tiplocToAtco.containsKey(tiploc);
    }
}
