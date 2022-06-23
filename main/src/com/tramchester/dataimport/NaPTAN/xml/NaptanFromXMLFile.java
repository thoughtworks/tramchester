package com.tramchester.dataimport.NaPTAN.xml;

import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tramchester.dataimport.NaPTAN.xml.stopArea.NaptanStopAreaData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class NaptanFromXMLFile {
    private static final Logger logger = LoggerFactory.getLogger(NaptanFromXMLFile.class);

    private final Path filePath;
    private final Charset charset;
    private final XmlMapper mapper;
    private final NaptanXmlConsumer naptanXmlConsumer;
    private final WstxInputFactory factory;
    private final String stopElementName;
    private final String areaElementName;

    public NaptanFromXMLFile(Path filePath, Charset charset, XmlMapper mapper, NaptanXmlConsumer naptanXmlConsumer) {
        this.filePath = filePath.toAbsolutePath();
        this.charset = charset;
        this.mapper = mapper;
        this.naptanXmlConsumer = naptanXmlConsumer;

        factory = new WstxInputFactory();

        stopElementName = getElementName(NaptanStopData.class);
        areaElementName = getElementName(NaptanStopAreaData.class);

    }

    private String getElementName(Class<?> type) {
        final String stopElementName;
        JsonTypeName elementType = type.getAnnotation(JsonTypeName.class);
        stopElementName = elementType.value();
        return stopElementName;
    }

    public void load() {
        try {
            Reader reader = new FileReader(filePath.toString(), charset);
            logger.info("Load naptan data from " +filePath.toAbsolutePath());
            load(reader);
        } catch (IOException | XMLStreamException e) {
            String msg = "Unable to load from file " + filePath;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public void load(Reader in) throws XMLStreamException, IOException {

        logger.info("Begin load");
        XMLStreamReader streamReader = factory.createXMLStreamReader(in);

        while (streamReader.hasNext()) {
            if (streamReader.isStartElement()) {
                String localName = streamReader.getLocalName();
                if (stopElementName.equals(localName)) {
                    consumeStopElement(streamReader);
                } else if (areaElementName.equals(localName)) {
                    consumeAreaElement(streamReader);
                } else {
                    streamReader.next();
                }
            } else {
                streamReader.next();
            }
        }

        streamReader.close();
        logger.info("Finished load");

    }

    private void consumeAreaElement(XMLStreamReader streamReader) throws IOException {
        NaptanStopAreaData element = mapper.readValue(streamReader, NaptanStopAreaData.class);
        naptanXmlConsumer.process(element);
    }

    private void consumeStopElement(XMLStreamReader streamReader) throws IOException {
        NaptanStopData element = mapper.readValue(streamReader, NaptanStopData.class);
        naptanXmlConsumer.process(element);
    }

    public interface NaptanXmlConsumer {
        void process(final NaptanStopAreaData element);
        void process(final NaptanStopData element);
    }

}
