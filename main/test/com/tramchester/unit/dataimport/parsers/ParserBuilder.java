package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.DataLoaderApacheCSV;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class ParserBuilder {

    public static CSVRecord getRecordFor(String source) throws IOException {
        Reader in = new StringReader(source);
        CSVParser parser = DataLoaderApacheCSV.createParser(in);
        return parser.getRecords().get(0);
    }
}
