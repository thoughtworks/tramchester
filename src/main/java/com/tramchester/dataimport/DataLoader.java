package com.tramchester.dataimport;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.reader.CSVEntryParser;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataLoader<T> {
    private final String fileName;
    private final CSVEntryParser<T> parser;
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    private Optional<Reader> reader;


    public DataLoader(String fileName, CSVEntryParser<T> parser) {
        this.fileName = fileName;
        this.parser = parser;
        reader = Optional.empty();
    }

    private void close() {
        reader.ifPresent(r -> {
            try {
                r.close();
            } catch (IOException e) {
                logger.warn("could not close "+fileName, e);
            }
        });
    }


    public Stream<T> loadAll(boolean skipHeader) {
        logger.info("Loading data from " + fileName + ".txt file.");
        try {
            FileReader theReader = new FileReader(String.format("%s.txt", fileName));
            reader = Optional.of(theReader);

            CSVStrategy csvStrategy;
            if (skipHeader) {
                csvStrategy = new CSVStrategy(',', '"', '#', true, true);
            } else
            {
                csvStrategy = CSVStrategy.UK_DEFAULT;
            }

            CSVReader<T> csvPersonReader = new CSVReaderBuilder<T>(theReader)
                    .entryParser(parser)
                    .strategy(csvStrategy)
                    .build();

            logger.info("Finished loading data from " + fileName + ".txt file.");

            Stream<T> resultStream = StreamSupport.stream(csvPersonReader.spliterator(), false);
            Runnable closeHandler = () -> close();
            resultStream.onClose(closeHandler);
            return resultStream;

        } catch (FileNotFoundException e) {
            logger.error("File not found: " + fileName + ".txt",e);
        }
        return Stream.empty();
    }
}
