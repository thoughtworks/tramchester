package com.tramchester.dataimport.loader.files;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class TransportDataFromJSONFile<T> implements TransportDataFromFile<T> {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromJSONFile.class);

    private final Path filePath;
    private final ObjectReader reader;

    public TransportDataFromJSONFile(Path filePath, Class<T> type, ObjectMapper mapper) {

        this.filePath = filePath.toAbsolutePath();

        // set-up a reader ahead of time, helps with performance
        reader = mapper.readerFor(type);
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
