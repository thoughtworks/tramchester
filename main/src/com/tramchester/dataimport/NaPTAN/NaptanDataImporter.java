package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.xml.NaptanDataXMLImporter;
import com.tramchester.dataimport.NaPTAN.xml.stopArea.NaptanStopAreaData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.stream.Stream;

import static java.lang.String.format;

/***
 * See
 * https://data.gov.uk/dataset/ff93ffc1-6656-47d8-9155-85ea0b8f2251/national-public-transport-access-nodes-naptan
 * https://www.gov.uk/government/publications/national-public-transport-access-node-schema/naptan-and-nptg-data-sets-and-schema-guides
 */
@LazySingleton
public class NaptanDataImporter {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataImporter.class);

    private final NaptanDataXMLImporter theImporter;

    @Inject
    protected NaptanDataImporter(TramchesterConfig config, CsvMapper csvMapper, RemoteDataRefreshed dataRefreshed,
                                 UnzipFetchedData.Ready ready) {
       if (config.hasRemoteDataSourceConfig(DataSourceID.naptanxml)) {
            theImporter = new NaptanDataXMLImporter(dataRefreshed);
        } else {
            theImporter = null;
            logger.warn(format("Naptan for %s is disabled, no config section found", DataSourceID.naptanxml));
       }
    }

    @PostConstruct
    public void start() {
        if (theImporter !=null) {
            logger.info("started");
        }
    }

    @PreDestroy
    public void stop() {
        if (theImporter !=null) {
            logger.info("Stopped");
        }
    }

    public Stream<NaptanStopData> getStopsData() {
        if (theImporter == null) {
            logger.warn("Disabled, but getStopsData() called");
            return Stream.empty();
        }
        return theImporter.loadData(NaptanStopData.class);
    }

    public Stream<NaptanStopAreaData> getAreasData() {
        if (theImporter == null) {
            logger.warn("Disabled, but getAreasData() called");
            return Stream.empty();
        }
        return theImporter.loadData(NaptanStopAreaData.class);
    }

    public boolean isEnabled() {
        return theImporter != null;
    }
}
