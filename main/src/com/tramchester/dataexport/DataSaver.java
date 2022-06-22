package com.tramchester.dataexport;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class DataSaver<T> {
    private static final Logger logger = LoggerFactory.getLogger(DataSaver.class);

    private final Class<T> dataClass;
    private final Path filePath;
    private final CsvMapper mapper;

    public DataSaver(Class<T> dataClass, Path filePath, CsvMapper mapper) {
        this.dataClass = dataClass;
        this.filePath = filePath;
        this.mapper = mapper;
    }

    public void save(List<T> dataToSave) {
        logger.info("Recording data for file in " + filePath.toAbsolutePath());

        CsvSchema schema = mapper.schemaFor(dataClass).withHeader();
        ObjectWriter myObjectWriter = mapper.writer(schema);

        try(FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            OutputStreamWriter writerOutputStream = new OutputStreamWriter(bufferedOutputStream, StandardCharsets.UTF_8);
            myObjectWriter.writeValue(writerOutputStream, dataToSave);
        } catch (IOException fileNotFoundException) {
            logger.error("Exception when saving hints", fileNotFoundException);
        }
    }

}
