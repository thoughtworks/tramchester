package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.csv.NaptanDataCSVImporter;
import com.tramchester.dataimport.NaPTAN.csv.NaptanStopCSVData;
import com.tramchester.dataimport.NaPTAN.xml.NaptanDataXMLImporter;
import com.tramchester.dataimport.NaPTAN.xml.NaptanStopData;
import com.tramchester.dataimport.NaPTAN.xml.NaptanStopXMLData;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.stream.Stream;

/***
 * See
 * https://data.gov.uk/dataset/ff93ffc1-6656-47d8-9155-85ea0b8f2251/national-public-transport-access-nodes-naptan
 * https://www.gov.uk/government/publications/national-public-transport-access-node-schema/naptan-and-nptg-data-sets-and-schema-guides
 */
@LazySingleton
public class NaptanStopsDataImporter  {
    private static final Logger logger = LoggerFactory.getLogger(NaptanStopsDataImporter.class);

    NaptanDataImporter<NaptanStopData> importer;

    @Inject
    protected NaptanStopsDataImporter(TramchesterConfig config, CsvMapper csvMapper, UnzipFetchedData.Ready dataIsReady) {
        if (config.hasRemoteDataSourceConfig(DataSourceID.naptanStopsCSV)) {
            importer = new NaptanDataCSVImporter<>(config, csvMapper, NaptanStopCSVData.class, DataSourceID.naptanStopsCSV, dataIsReady);
        } else if (config.hasRemoteDataSourceConfig(DataSourceID.naptanxml)) {
            importer = new NaptanDataXMLImporter<>(config, NaptanStopXMLData.class, dataIsReady);
        } else {
            importer = null;
            logger.warn("Naptan config is missing");
        }
    }

    @PostConstruct
    public void start() {
        if (importer!=null) {
            logger.info("starting");
            importer.start();
            logger.info("started");
        }
    }

    @PreDestroy
    public void stop() {
        if (importer!=null) {
            logger.info("Stopping");
            importer.stop();
            logger.info("Stopped");
        }
    }

    public Stream<NaptanStopData> getStopsData() {
        if (importer==null) {
            return Stream.empty();
        }
        return importer.getDataStream();
    }

    public boolean isEnabled() {
        return importer != null && importer.isEnabled();
    }
}
