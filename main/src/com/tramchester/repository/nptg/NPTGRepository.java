package com.tramchester.repository.nptg;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.dataimport.nptg.NPTGDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/***
 * National Public Transport Gazetteer
 *
 * https://data.gov.uk/dataset/3b1766bf-04a3-44f5-bea9-5c74cf002e1d/national-public-transport-gazetteer-nptg
 *
 *  Cross referenced by naptan data via the nptgLocalityCode
 */
@LazySingleton
public class NPTGRepository {
    private static final Logger logger = LoggerFactory.getLogger(NPTGRepository.class);

    private final NPTGDataLoader dataLoader;
    private final Map<String, NPTGData> entries;

    @Inject
    public NPTGRepository(NPTGDataLoader dataLoader) {
        this.dataLoader = dataLoader;
        entries = new HashMap<>();
    }

    @PostConstruct
    private void start() {
        if (!dataLoader.isEnabled()) {
            logger.warn("Disabled");
            return;
        }
        logger.info("Starting");
        dataLoader.getData().forEach(item -> entries.put(item.getNptgLocalityCode(), item));
        logger.info("Loaded " + entries.size() + " items ");
        logger.info("started");
    }

    @PreDestroy
    private void stop() {
        entries.clear();
    }

    public NPTGData getByNptgCode(String nptgLocalityCode) {
        return entries.get(nptgLocalityCode);
    }

    public boolean hasNptgCode(String code) {
        return entries.containsKey(code);
    }
}
