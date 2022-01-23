package com.tramchester.unit.dataimport;

import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.dataimport.loader.files.TransportDataFromXMLFile;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ParserTestXMLHelper<T> {
    private TransportDataFromFile<T> dataDataLoader;

    protected void before(Class<T> klass, Charset charset) {
        dataDataLoader = new TransportDataFromXMLFile<>(Paths.get("unused"), charset, klass);
    }

    protected T parseFirstOnly(String text) throws XMLStreamException, IOException {
        StringReader reader = new StringReader(text);
        return dataDataLoader.load(reader).findFirst().orElseThrow();
    }

    protected List<T> parseAll(String text) throws XMLStreamException, IOException {
        StringReader reader = new StringReader(text);
        return dataDataLoader.load(reader).collect(Collectors.toList());
    }
}
