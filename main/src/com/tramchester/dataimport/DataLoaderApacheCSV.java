package com.tramchester.dataimport;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.dataimport.parsers.CSVEntryMapper;
import com.tramchester.dataimport.parsers.StopTimeDataMapper;
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
            // WIP
            if (StopTimeDataMapper.class.isAssignableFrom(mapper.getClass())) {
                return loadFilteredJackson(); // experimental mapper using jackson
            }

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

    private Stream<T> loadFilteredJackson() throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        // TODO buffered reader or not? Performance test....
        Reader in = new FileReader(fileName.toAbsolutePath().toString());
        BufferedReader bufferedReader = new BufferedReader(in);
        MappingIterator<T> reader = mapper.readerFor(StopTimeData.class).with(schema).readValues(bufferedReader);

        Iterable<T> iterable = () -> reader;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static CSVParser createParser(Reader in) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT;
        return csvFormat.parse(in);
    }

}
