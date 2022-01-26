package com.tramchester.dataimport.NaPTAN.xml;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaptanDataFromXMLFile;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;

public class NaptanDataImporter {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataImporter.class);

    private final RemoteDataSourceConfig sourceConfig;

    public NaptanDataImporter(TramchesterConfig config, UnzipFetchedData.Ready dataIsReady) {
        sourceConfig = config.getDataRemoteSourceConfig(DataSourceID.naptanxml);
    }

    public <T extends NaptanXMLData> Stream<T> loadData(Class<T> theClass) {
        Path dataPath = sourceConfig.getDataPath();
        String filename = sourceConfig.getDownloadFilename();
        Path filePath = dataPath.resolve(filename);
        logger.info("Loading data from " + filePath.toAbsolutePath());
        TransportDataFromFile<T> dataLoader = new NaptanDataFromXMLFile<>(filePath, StandardCharsets.UTF_8, theClass);

        return dataLoader.load();
    }
}
