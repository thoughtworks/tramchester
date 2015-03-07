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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataLoader<T> {
    private final String fileName;
    private final CSVEntryParser<T> parser;
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    public DataLoader(String fileName, CSVEntryParser<T> parser) {
        this.fileName = fileName;
        this.parser = parser;
    }

    public Stream<T> loadAll() throws IOException {
        logger.info("Loading data from " + fileName + ".txt file.");
        Reader reader = null;
        try {
            reader = new FileReader(String.format("%s.txt", fileName));

            CSVReader<T> csvPersonReader = new CSVReaderBuilder<T>(reader)
                    .entryParser(parser)
                    .strategy(CSVStrategy.UK_DEFAULT)
                    .build();

            logger.info("Finished loading data from " + fileName + ".txt file.");

            Stream<T> resultStream = StreamSupport.stream(csvPersonReader.spliterator(), false);
            return resultStream;

        } catch (FileNotFoundException e) {
            logger.error("File not found: " + fileName + ".txt");
            e.printStackTrace();
        } finally {
            //reader.close();
        }
        return Stream.empty();
    }
}
