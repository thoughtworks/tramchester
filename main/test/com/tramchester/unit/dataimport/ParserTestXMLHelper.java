package com.tramchester.unit.dataimport;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.NaPTAN.xml.NaptanFromXMLFile;
import com.tramchester.dataimport.NaPTAN.xml.stopArea.NaptanStopAreaData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ParserTestXMLHelper<T extends NaptanXMLData> {

    private final XmlMapper mapper;
    private NaptanFromXMLFile loader;
    private List<NaptanXMLData> received;

    public ParserTestXMLHelper() {
        mapper = XmlMapper.builder().
                addModule(new BlackbirdModule()).
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    protected void before(Charset charset) {
        received = new ArrayList<>();
        loader = new NaptanFromXMLFile(Paths.get("unused"), charset, mapper, new NaptanFromXMLFile.NaptanXmlConsumer() {
            @Override
            public void process(NaptanStopAreaData naptanStopAreaData) {
                received.add(naptanStopAreaData);
            }

            @Override
            public void process(NaptanStopData naptanStopData) {
                received.add(naptanStopData);
            }
        });
    }

    protected NaptanXMLData parseFirstOnly(String text) throws XMLStreamException, IOException {
        StringReader reader = new StringReader(text);
        loader.load(reader);

        return received.get(0);

    }

    protected List<NaptanXMLData> parseAll(String text) throws XMLStreamException, IOException {
        StringReader reader = new StringReader(text);
        loader.load(reader);
        return received;
    }
}
