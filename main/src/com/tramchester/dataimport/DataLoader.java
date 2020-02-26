package com.tramchester.dataimport;

import com.tramchester.dataimport.parsers.CSVEntryMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataLoader<T> {
    private final String fileName;
    private final CSVEntryMapper<T> mapper;
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    public DataLoader(String fileName, CSVEntryMapper<T> mapper) {
        this.fileName = fileName;
        this.mapper = mapper;
    }

    public Stream<T> loadAll(boolean skipHeaders) {
            try {
            Reader in = new FileReader(fileName);
                CSVParser parser = createParser(in);
                if (skipHeaders) {
                    parser.iterator().next();
                }
                Stream<T> result = StreamSupport.stream(parser.spliterator(), false)
                        .filter(mapper::filter).map(mapper::parseEntry);

                //noinspection ResultOfMethodCallIgnored
                result.onClose(() -> {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.error("Exception while closing file "+fileName, e);
                    }
            });
                return result;
        } catch (FileNotFoundException e) {
            String msg = "Unable to load from file " + fileName;
            logger.error(msg,e);
            throw new RuntimeException(msg,e);
        } catch (IOException e) {
            logger.error("Unable to parse file "+fileName, e);
            return Stream.empty();
        }
    }

    public static CSVParser createParser(Reader in) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT;
        return csvFormat.parse(in);
    }

}
