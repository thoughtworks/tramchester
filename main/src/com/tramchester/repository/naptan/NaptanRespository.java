package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaptanDataImporter;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.NaPTAN.xml.stopArea.NaptanStopAreaData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanXMLStopAreaRef;
import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.repository.nptg.NPTGRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

// http://naptan.dft.gov.uk/naptan/schema/2.5/doc/NaPTANSchemaGuide-2.5-v0.67.pdf

@LazySingleton
public class NaptanRespository {
    private static final Logger logger = LoggerFactory.getLogger(NaptanRespository.class);

    private final NaptanDataImporter naptanDataImporter;
    private final NPTGRepository nptgRepository;
    private final TramchesterConfig config;
    private IdMap<NaptanRecord> stops;
    private IdMap<NaptanArea> areas;
    private Map<IdFor<Station>, IdFor<NaptanRecord>> tiplocToAtco;

    @Inject
    public NaptanRespository(NaptanDataImporter naptanDataImporter, NPTGRepository nptgRepository, TramchesterConfig config) {
        this.naptanDataImporter = naptanDataImporter;
        this.nptgRepository = nptgRepository;
        this.config = config;
        stops = new IdMap<>();
        areas = new IdMap<>();
        tiplocToAtco = Collections.emptyMap();
    }

    @PostConstruct
    private void start() {
        logger.info("starting");

        boolean enabled = naptanDataImporter.isEnabled();

        if (!enabled) {
            logger.warn("Not enabled, imported is disable, no config for naptan?");
            return;
        } else {
            loadStopDataForConfiguredArea();
        }

        logger.info("started");
    }


    @PreDestroy
    private void stop() {
        logger.info("stopping");
        stops.clear();
        tiplocToAtco.clear();
        logger.info("stopped");
    }

    private void loadStopDataForConfiguredArea() {
        BoundingBox bounds = config.getBounds();
        logger.info("Loading data for " + bounds);
        Double range = config.getNearestStopForWalkingRangeKM();

        MarginInMeters margin = MarginInMeters.of(range);

        loadAreaData(bounds, margin);
        loadStopsData(bounds, margin);
        loadRailStationData(bounds, margin);
    }

    private void loadAreaData(BoundingBox bounds, MarginInMeters margin) {
        logger.info("Load naptan area reference data");

        Stream<NaptanStopAreaData> areaDataStream = naptanDataImporter.getAreasData().
                filter(area -> !area.getStopAreaCode().isBlank());

        areas = filterBy(bounds, margin, areaDataStream).
                map(this::createArea).
                collect(IdMap.collector());

        logger.info("Loaded " + areas.size() + " areas");
    }

    private NaptanArea createArea(NaptanStopAreaData areaData) {
        IdFor<NaptanArea> id = StringIdFor.createId(areaData.getStopAreaCode());
        if (areaData.getStatus().isEmpty()) {
            logger.warn("No status set for " + areaData.getStopAreaCode());
        }
        return new NaptanArea(id, areaData.getName(), areaData.getGridPosition(), areaData.isActive(), areaData.getAreaType());
    }

    private void loadStopsData(BoundingBox bounds, MarginInMeters margin) {
        logger.info("Load naptan stop reference data");

        Stream<NaptanStopData> stopsData = naptanDataImporter.getStopsData().
                filter(NaptanStopData::hasValidAtcoCode);

        stops = filterBy(bounds, margin, stopsData).
                map(this::createRecord).
                collect(IdMap.collector());

        stopsData.close();

        logger.info("Loaded " + stops.size() + " stops");
    }

    private void loadRailStationData(BoundingBox bounds, MarginInMeters margin) {
        logger.info("Load rail station reference data from natpan stops");

        // TODO do this in a way that avoids streaming the stops data twice

        Stream<NaptanStopData> railStops = naptanDataImporter.getStopsData().
                filter(NaptanStopData::hasRailInfo).
                filter(NaptanStopData::hasValidAtcoCode);

        tiplocToAtco = filterBy(bounds, margin, railStops)
                .collect(Collectors.toMap(data ->
                        StringIdFor.createId(data.getRailInfo().getTiploc()), NaptanStopData::getAtcoCode));
        logger.info("Loaded " + tiplocToAtco.size() + " stations");
    }

    private NaptanRecord createRecord(NaptanStopData original) {
        IdFor<NaptanRecord> id = original.getAtcoCode();

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

        final List<NaptanXMLStopAreaRef> stopAreaRefs = original.stopAreasRefs();

        List<String> areaIds = stopAreaRefs.stream().
                filter(NaptanXMLStopAreaRef::isActive). // filter out if marked in-active for *this* stop
                filter(this::checkIfActive). // filter out if area is marked in-active
                map(NaptanXMLStopAreaRef::getId).
                collect(Collectors.toList());

        if (areaIds.size()>1) {
            logger.warn("Multiple stop area refs active for " + id);
        }

        return new NaptanRecord(id, original.getCommonName(), original.getGridPosition(), original.getLatLong(),
                suburb, town, original.getStopType(), areaIds);
    }

    private boolean checkIfActive(NaptanXMLStopAreaRef area) {
        IdFor<NaptanArea> areaId = StringIdFor.createId(area.getId());
        if (areas.hasId(areaId)) {
            return areas.get(areaId).isActive();
        } else {
            // this seems to happen a lot, perhaps area ids are generated automatically?
            //logger.warn(format("Area %s is not present in areas, but is referenced by a stop", area.getId()));
            return false;
        }
    }


    private <T extends NaptanXMLData> Stream<T> filterBy(BoundingBox bounds, MarginInMeters margin, Stream<T> stream) {
        return stream.
                filter(item -> item.getGridPosition().isValid()).
                filter(item -> bounds.within(margin, item.getGridPosition()));
    }

    // TODO Check or diag on NaptanStopType
    public <T extends Location<?>>  boolean containsActo(IdFor<T> locationId) {
        IdFor<NaptanRecord> id = convertId(locationId);
        return stops.hasId(id);
    }

    // TODO Check or diag on NaptanStopType
    public <T extends Location<?>> NaptanRecord getForActo(IdFor<T> actoCode) {
        IdFor<NaptanRecord> id = convertId(actoCode);
        return stops.get(id);
    }

    private <T extends Location<?>> IdFor<NaptanRecord> convertId(IdFor<T> actoCode) {
        return StringIdFor.convert(actoCode);
    }

    public boolean isEnabled() {
        return naptanDataImporter.isEnabled();
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
        if (stops.hasId(acto)) {
            return stops.get(acto);
        }
        return null;
    }

    public boolean containsTiploc(IdFor<Station> tiploc) {
        return tiplocToAtco.containsKey(tiploc);
    }

    public NaptanArea getAreaFor(IdFor<NaptanArea> id) {
        return areas.get(id);
    }

    public boolean containsArea(IdFor<NaptanArea> id) {
        return areas.hasId(id);
    }

    public IdSet<NaptanArea> activeCodes(IdSet<NaptanArea> ids) {
        return ids.stream().
                filter(id -> areas.hasId(id)).
                filter(id -> areas.get(id).isActive()).collect(IdSet.idCollector());
    }

    /***
     * Naptan records for an area. For stations in an area use StationLocations.
     * @see com.tramchester.geo.StationLocations
     * @param areaId naptan area id
     * @return matching record
     */
    public Set<NaptanRecord> getRecordsFor(IdFor<NaptanArea> areaId) {
        return stops.filterStream(stop -> stop.getAreaCodes().contains(areaId)).collect(Collectors.toSet());
    }

    /***
     * Number of Naptan records for an area. For stations in an area use StationLocations.
     * @see com.tramchester.geo.StationLocations
     * @param areaId naptan area id
     * @return matching record
     */
    public long getNumRecordsFor(IdFor<NaptanArea> areaId) {
        return stops.filterStream(stop -> stop.getAreaCodes().contains(areaId)).count();
    }

    public Set<NaptanArea> getAreas() {
        return new HashSet<>(areas.getValues());
    }

}
