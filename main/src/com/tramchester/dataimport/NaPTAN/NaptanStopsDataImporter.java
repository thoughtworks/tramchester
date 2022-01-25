package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.xml.NaptanDataXMLImporter;
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

    NaptanDataXMLImporter theImporter;

    // TODO Unwrap this, no longer needed now only one kind of natpan data importer


    @Inject
    protected NaptanStopsDataImporter(TramchesterConfig config, CsvMapper csvMapper, UnzipFetchedData.Ready dataIsReady) {
       if (config.hasRemoteDataSourceConfig(DataSourceID.naptanxml)) {
            theImporter = new NaptanDataXMLImporter(config, dataIsReady);
        } else {
            theImporter = null;
            logger.warn("Naptan config is missing");
        }
    }

    @PostConstruct
    public void start() {
        if (theImporter !=null) {
            logger.info("starting");
            theImporter.start();
            logger.info("started");
        }
    }

    @PreDestroy
    public void stop() {
        if (theImporter !=null) {
            logger.info("Stopping");
            theImporter.stop();
            logger.info("Stopped");
        }
    }

    public Stream<NaptanStopXMLData> getStopsData() {
        if (theImporter ==null) {
            return Stream.empty();
        }
        return theImporter.getDataStream();
    }

    public boolean isEnabled() {
        return theImporter != null && theImporter.isEnabled();
    }
}
