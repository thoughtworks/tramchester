package com.tramchester.dataimport;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataLoader<T> {
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final Path filePath;
    private final Class<T> targetType;

    public DataLoader(Path filePath, Class<T> targetType) {
        this.filePath = filePath.toAbsolutePath();
        this.targetType = targetType;
    }

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

    public Stream<T> load(Reader in) {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        try {
            // TODO buffered reader or not? Performance test....
            BufferedReader bufferedReader = new BufferedReader(in);
            MappingIterator<T> reader = mapper.readerFor(targetType).with(schema).readValues(bufferedReader);

            Iterable<T> iterable = () -> reader;
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
