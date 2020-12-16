package com.tramchester.dataimport;

import com.tramchester.dataimport.parsers.CSVEntryMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Deprecated
public class DataLoaderApacheCSV<T> {
    private final Path fileName;
    private final CSVEntryMapper<T> mapper;
    private static final Logger logger = LoggerFactory.getLogger(DataLoaderApacheCSV.class);

    public DataLoaderApacheCSV(Path fileName, CSVEntryMapper<T> mapper) {
        this.fileName = fileName;
        this.mapper = mapper;
    }

    public Stream<T> load() {
        if (!Files.exists(fileName)) {
            throw new RuntimeException("Unable to load from file " + fileName.toAbsolutePath());
        }

        try {

            Reader in = new FileReader(fileName.toAbsolutePath().toString());
            BufferedReader bufferedReader = new BufferedReader(in);
            CSVParser parser = createParser(bufferedReader);
            CSVRecord header = parser.iterator().next();
            mapper.initColumnIndex(header);
            Stream<T> result = StreamSupport.stream(parser.spliterator(), false)
                    .filter(mapper::shouldInclude).map(mapper::parseEntry);

            //noinspection ResultOfMethodCallIgnored
            result.onClose(() -> {
                try {
                    bufferedReader.close();
                    in.close();
                    logger.info("Close " + fileName.toAbsolutePath());
                } catch (IOException e) {
                    logger.error("Exception while closing file " + fileName.toAbsolutePath(), e);
                }
            });
            return result;
        } catch (FileNotFoundException e) {
            String msg = "Unable to load from file " + fileName.toAbsolutePath();
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (IOException e) {
            logger.error("Unable to parse file " + fileName.toAbsolutePath(), e);
            return Stream.empty();
        }
    }

    public static CSVParser createParser(Reader in) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT;
        return csvFormat.parse(in);
    }

}
