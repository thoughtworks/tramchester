package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
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
public class NaptanStopsDataImporter extends NaptanDataCSVImporter<NaptanStopData> {
    private static final Logger logger = LoggerFactory.getLogger(NaptanStopsDataImporter.class);

    @Inject
    protected NaptanStopsDataImporter(TramchesterConfig config, CsvMapper mapper,
                                      UnzipFetchedData.Ready dataIsReady) {
        super(config, mapper, NaptanStopData.class, DataSourceID.naptancsv, dataIsReady);
    }


    @PostConstruct
    public void start() {
        logger.info("starting");
        super.start();
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        super.stop();
        logger.info("Stopped");
    }

    public Stream<NaptanStopData> getStopsData() {
        return super.getDataStream();
    }

}
