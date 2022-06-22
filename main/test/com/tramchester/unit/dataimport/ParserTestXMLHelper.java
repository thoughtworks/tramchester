package com.tramchester.unit.dataimport;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.dataimport.NaPTAN.NaptanDataFromXMLFile;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ParserTestXMLHelper<T extends NaptanXMLData> {
    private TransportDataFromFile<T> dataDataLoader;

    private final XmlMapper mapper;

    public ParserTestXMLHelper() {
        mapper = XmlMapper.builder().
                addModule(new BlackbirdModule()).
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    protected void before(Class<T> klass, Charset charset) {
        dataDataLoader = new NaptanDataFromXMLFile<>(Paths.get("unused"), charset, klass, mapper);
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
