package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.NaPTAN.xml.NaptanDataCallbackImporter;
import com.tramchester.dataimport.NaPTAN.xml.NaptanFromXMLFile;
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
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.repository.nptg.NPTGRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

// http://naptan.dft.gov.uk/naptan/schema/2.5/doc/NaPTANSchemaGuide-2.5-v0.67.pdf

@LazySingleton
public class NaptanRepositoryContainer implements NaptanRepository {
    private static final Logger logger = LoggerFactory.getLogger(NaptanRepositoryContainer.class);

    private final NaptanDataCallbackImporter naptanDataImporter;
    private final NPTGRepository nptgRepository;
    private final TramchesterConfig config;

    private final IdMap<NaptanRecord> stops;
    private final IdMap<NaptanArea> areas;
    private final Map<IdFor<Station>, IdFor<NaptanRecord>> tiplocToAtco;

    @Inject
    public NaptanRepositoryContainer(NaptanDataCallbackImporter naptanDataImporter, NPTGRepository nptgRepository, TramchesterConfig config) {
        this.naptanDataImporter = naptanDataImporter;
        this.nptgRepository = nptgRepository;
        this.config = config;
        stops = new IdMap<>();
        areas = new IdMap<>();
        tiplocToAtco = new HashMap<>();
    }

    @PostConstruct
    public void start() {
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
    public void stop() {
        logger.info("stopping");
        stops.clear();
        tiplocToAtco.clear();
        areas.clear();
        logger.info("stopped");
    }

    private void loadStopDataForConfiguredArea() {

        Map<IdFor<NaptanRecord>, List<NaptanXMLStopAreaRef>> pendingAreaIds = new HashMap<>();

        BoundingBox bounds = config.getBounds();
        logger.info("Loading data for " + bounds);
        Double range = config.getNearestStopForWalkingRangeKM();

        MarginInMeters margin = MarginInMeters.of(range);

        naptanDataImporter.loadData(new NaptanFromXMLFile.NaptanXmlConsumer() {
            @Override
            public void process(NaptanStopAreaData element) {
                consumeStopArea(element, bounds, margin);
            }

            @Override
            public void process(NaptanStopData element) {
                consumeStop(element, bounds, margin, pendingAreaIds);
            }
        });

        logger.info("Loaded " + areas.size() + " areas");
        logger.info("Loaded " + stops.size() + " stops");
        logger.info("Loaded " + tiplocToAtco.size() + " mappings for rail stations" );

        logger.info("Post filter stops for active areas codes");
        pendingAreaIds.forEach((recordId, areas) -> {
            List<String> active = areas.stream().
                    filter(this::checkIfActive).
                    map(NaptanXMLStopAreaRef::getId).
                    collect(Collectors.toList());
            stops.get(recordId).setAreaCodes(active);
        });

        pendingAreaIds.clear();
        logger.info("Finished updating area codes");

    }

    private void consumeStopArea(final NaptanStopAreaData areaData, final BoundingBox bounds, final MarginInMeters margin) {
        if (areaData.getStopAreaCode().isBlank()) {
            return;
        }
        if (filterBy(bounds, margin, areaData)) {
            final NaptanArea record = createArea(areaData);
            areas.add(record);
        }
    }

    private void consumeStop(final NaptanStopData stopData, final BoundingBox bounds, final MarginInMeters margin,
                             final Map<IdFor<NaptanRecord>, List<NaptanXMLStopAreaRef>> pendingAreaIds) {
        if (!stopData.hasValidAtcoCode()) {
            return;
        }

        if (filterBy(bounds, margin, stopData)) {
            final NaptanRecord record = createRecord(stopData, pendingAreaIds);
            stops.add(record);

            if (stopData.hasRailInfo()) {
                final IdFor<Station> id = StringIdFor.createId(stopData.getRailInfo().getTiploc());
                tiplocToAtco.put(id, stopData.getAtcoCode());
            }
        }
    }

    private NaptanArea createArea(final NaptanStopAreaData areaData) {
        if (areaData.getStatus().isEmpty()) {
            logger.warn("No status set for " + areaData.getStopAreaCode());
        }
        final IdFor<NaptanArea> id = StringIdFor.createId(areaData.getStopAreaCode());
        return new NaptanArea(id, areaData.getName(), areaData.getGridPosition(), areaData.isActive(), areaData.getAreaType());
    }

    private NaptanRecord createRecord(final NaptanStopData original, final Map<IdFor<NaptanRecord>, List<NaptanXMLStopAreaRef>> pendingAreaIds) {
        final IdFor<NaptanRecord> id = original.getAtcoCode();

        String suburb = original.getSuburb();
        String town = original.getTown();

        final String nptgLocality = original.getNptgLocality();
        if (nptgRepository.hasNptgCode(nptgLocality)) {
            final NPTGData extra = nptgRepository.getByNptgCode(nptgLocality);
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

        // record pending areas, need to have loaded entire file before can properly check if active or not
        // see load method above
        List<NaptanXMLStopAreaRef> areaIds = stopAreaRefs.stream().
                filter(NaptanXMLStopAreaRef::isActive). // filter out if marked inactive for *this* stop
                collect(Collectors.toList());
        pendingAreaIds.put(id, areaIds);

        // Seems very common in the data, not sure what we can about it anyway?
//        if (areaIds.size()>1) {
//            logger.warn("Multiple stop area refs active for acto code: " + id);
//        }

        return new NaptanRecord(id, original.getCommonName(), original.getGridPosition(), original.getLatLong(),
                suburb, town, original.getStopType());
    }

    private boolean checkIfActive(NaptanXMLStopAreaRef area) {
        final IdFor<NaptanArea> areaId = StringIdFor.createId(area.getId());
        if (areas.hasId(areaId)) {
            return areas.get(areaId).isActive();
        } else {
            // this seems to happen a lot, perhaps area ids are generated automatically?
            //logger.warn(format("Area %s is not present in areas, but is referenced by a stop", area.getId()));
            return false;
        }
    }

    private boolean filterBy(final BoundingBox bounds, final MarginInMeters margin, final NaptanXMLData item) {
        final GridPosition gridPosition = item.getGridPosition();
        if (!gridPosition.isValid()) {
            return false;
        }
        return bounds.within(margin, gridPosition);
    }

    // TODO Check or diag on NaptanStopType
    @Override
    public <T extends Location<?>>  boolean containsActo(final IdFor<T> locationId) {
        final IdFor<NaptanRecord> id = convertId(locationId);
        return stops.hasId(id);
    }

    // TODO Check or diag on NaptanStopType
    @Override
    public <T extends Location<?>> NaptanRecord getForActo(final IdFor<T> actoCode) {
        final IdFor<NaptanRecord> id = convertId(actoCode);
        return stops.get(id);
    }

    private <T extends Location<?>> IdFor<NaptanRecord> convertId(final IdFor<T> actoCode) {
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
    @Override
    public NaptanRecord getForTiploc(IdFor<Station> railStationTiploc) {
        if (!tiplocToAtco.containsKey(railStationTiploc)) {
            return null;
        }
        final IdFor<NaptanRecord> acto = tiplocToAtco.get(railStationTiploc);
        if (stops.hasId(acto)) {
            return stops.get(acto);
        }
        return null;
    }

    @Override
    public boolean containsTiploc(IdFor<Station> tiploc) {
        return tiplocToAtco.containsKey(tiploc);
    }

    @Override
    public NaptanArea getAreaFor(IdFor<NaptanArea> id) {
        return areas.get(id);
    }

    @Override
    public boolean containsArea(IdFor<NaptanArea> id) {
        return areas.hasId(id);
    }

    @Override
    public IdSet<NaptanArea> activeCodes(IdSet<NaptanArea> ids) {
        return ids.stream().
                filter(areas::hasId).
                filter(id -> areas.get(id).isActive()).collect(IdSet.idCollector());
    }

    /***
     * Naptan records for an area. For stations in an area use StationLocations.
     * @see com.tramchester.geo.StationLocations
     * @param areaId naptan area id
     * @return matching record
     */
    @Override
    public Set<NaptanRecord> getRecordsFor(IdFor<NaptanArea> areaId) {
        return stops.filterStream(stop -> stop.getAreaCodes().contains(areaId)).collect(Collectors.toSet());
    }

    /***
     * Number of Naptan records for an area. For stations in an area use StationLocations.
     * @see com.tramchester.geo.StationLocations
     * @param areaId naptan area id
     * @return matching record
     */
    @Override
    public long getNumRecordsFor(IdFor<NaptanArea> areaId) {
        return stops.filterStream(stop -> stop.getAreaCodes().contains(areaId)).count();
    }

    @Override
    public Set<NaptanArea> getAreas() {
        return new HashSet<>(areas.getValues());
    }

}
