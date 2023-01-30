package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.domain.DataSourceID;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static java.lang.String.format;

public class NaptanDataCallbackImporter {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataCallbackImporter.class);

    private final RemoteDataRefreshed remoteDataRefreshed;
    private final boolean enabled;
    private final XmlMapper mapper;

    @Inject
    public NaptanDataCallbackImporter(RemoteDataRefreshed remoteDataRefreshed, TramchesterConfig config,
                                      FetchDataFromUrl.Ready dataIsReady) {
        this.remoteDataRefreshed = remoteDataRefreshed;
        enabled = config.hasRemoteDataSourceConfig(DataSourceID.naptanxml);

        mapper = XmlMapper.builder().
                addModule(new BlackbirdModule()).
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).
                disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).
                enable(DeserializationFeature.EAGER_DESERIALIZER_FETCH).
                build();
    }

    public void loadData(NaptanFromXMLFile.NaptanXmlConsumer consumer) {
        if (!enabled) {
            logger.warn("Not enabled");
            return;
        }

        if (!remoteDataRefreshed.hasFileFor(DataSourceID.naptanxml)) {
            final String message = "Missing source file for " + DataSourceID.naptanxml;
            logger.error(message);
            throw new RuntimeException(message);
        }

        Path filePath = remoteDataRefreshed.fileFor(DataSourceID.naptanxml);

        String name = filePath.getFileName().toString();

        if (name.toLowerCase().endsWith(".zip")) {
            String newPath = FilenameUtils.removeExtension(filePath.toString());
            logger.info(format("Zip was downloaded as %s, use unzipped file %s", filePath, newPath));
            filePath = Path.of(newPath);
        }

        logger.info("Loading data from " + filePath.toAbsolutePath());
        // naptan xml is UTF-8
        NaptanFromXMLFile dataLoader = new NaptanFromXMLFile(filePath, StandardCharsets.UTF_8, mapper, consumer);

        dataLoader.load();
    }

    public boolean isEnabled() {
        return enabled;
    }

}
