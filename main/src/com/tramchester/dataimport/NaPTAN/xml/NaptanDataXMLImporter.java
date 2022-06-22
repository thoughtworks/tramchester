package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tramchester.dataimport.NaPTAN.NaptanDataFromXMLFile;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.domain.DataSourceID;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.lang.String.format;

public class NaptanDataXMLImporter {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataXMLImporter.class);

    private final RemoteDataRefreshed remoteDataRefreshed;

    public NaptanDataXMLImporter(RemoteDataRefreshed remoteDataRefreshed) {
        this.remoteDataRefreshed = remoteDataRefreshed;
    }

    public <T extends NaptanXMLData> Stream<T> loadData(Class<T> theClass, XmlMapper mapper) {
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
        TransportDataFromFile<T> dataLoader = new NaptanDataFromXMLFile<>(filePath, StandardCharsets.UTF_8, theClass, mapper);

        return dataLoader.load();
    }
}
