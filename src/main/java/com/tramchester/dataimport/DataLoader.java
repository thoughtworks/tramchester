package com.tramchester.dataimport;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.reader.CSVEntryParser;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class DataLoader<T> {
    private String fileName;
    private CSVEntryParser<T> parser;

    public DataLoader(String fileName, CSVEntryParser<T> parser) {
        this.fileName = fileName;
        this.parser = parser;
    }

    public List<T> loadAll() throws IOException {
        Reader reader = null;
        try {
            reader = new FileReader("data/tram/" + fileName + ".txt");
            CSVReader<T> csvPersonReader = new CSVReaderBuilder<T>(reader).entryParser(parser).strategy(CSVStrategy.UK_DEFAULT).build();
            return csvPersonReader.readAll();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
        return new ArrayList<>();
    }
}
