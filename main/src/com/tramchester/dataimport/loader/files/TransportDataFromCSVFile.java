package com.tramchester.dataimport.loader.files;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class TransportDataFromCSVFile<T,R extends T> implements TransportDataFromFile<T> {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromCSVFile.class);

    private final Path filePath;
    private final ObjectReader reader;

    public TransportDataFromCSVFile(Path filePath, Class<R> readerType, CsvMapper mapper) {
        this(filePath, readerType, Collections.emptyList(), mapper);
    }

    public TransportDataFromCSVFile(Path filePath, Class<R> readerType, String cvsHeader, CsvMapper mapper) {
        this(filePath, readerType, Arrays.asList(cvsHeader.split(",")), mapper);
    }

    private TransportDataFromCSVFile(Path filePath, Class<R> readerType, List<String> columns, CsvMapper mapper) {

        // TODO Set file encoding explicitly here?

        this.filePath = filePath.toAbsolutePath();

        CsvSchema schema;
        if (columns.isEmpty()) {
            schema = CsvSchema.emptySchema().withHeader();
        } else {
            CsvSchema.Builder builder = CsvSchema.builder();
            columns.forEach(builder::addColumn);
            schema = builder.build();
        }

        // set-up a reader ahead of time, helps with performance
        reader = mapper.readerFor(readerType).
                with(schema).
                without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public Stream<T> load() {
        try {
            Reader reader = new FileReader(filePath.toString());
            return load(reader);
        } catch (FileNotFoundException e) {
            String msg = "Unable to load from file " + filePath;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    // public, test support inject of reader
    @Override
    public Stream<T> load(Reader in) {

        try {
            // TODO buffered reader or not? Performance test....
            BufferedReader bufferedReader = new BufferedReader(in);

            MappingIterator<T> readerIter = reader.readValues(bufferedReader);

            Iterable<T> iterable = () -> readerIter;
            return StreamSupport.stream(iterable.spliterator(), false);

        } catch (FileNotFoundException e) {
            String msg = "Unable to load from file " + filePath;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (IOException e) {
            logger.error("Unable to parse file " + filePath, e);
            return Stream.empty();
        }

    }

}
