package com.tramchester.unit.dataimport;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;

import java.io.StringReader;
import java.nio.file.Paths;

public class ParserTestCSVHelper<T> {
    private TransportDataFromCSVFile<T,T> dataDataLoader;
    private String header;

    private static CsvMapper mapper = CsvMapper.builder().addModule(new AfterburnerModule()).build();

    protected void before(Class<T> readerType, String header) {
        //CsvMapper mapper = CsvMapper.builder().build();
        this.header = header;
        dataDataLoader = new TransportDataFromCSVFile<>(Paths.get("unused"), readerType, mapper);
    }

    protected T parse(String text) {
        StringReader reader = new StringReader(header+System.lineSeparator()+text+System.lineSeparator());
        return dataDataLoader.load(reader).findFirst().orElseThrow();
    }
}
