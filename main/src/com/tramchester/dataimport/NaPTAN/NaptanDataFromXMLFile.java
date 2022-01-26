package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

public class NaptanDataFromXMLFile<T extends NaptanXMLData> implements TransportDataFromFile<T> {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataFromXMLFile.class);

    private final Path filePath;
    private final Class<T> concreteType;
    private final Charset charset;

    public NaptanDataFromXMLFile(Path filePath, Charset charset, Class<T> concreteType) {
        this.filePath = filePath.toAbsolutePath();
        this.charset = charset;
        this.concreteType = concreteType;
    }

    @Override
    public Stream<T> load() {
        try {
            Reader reader = new FileReader(filePath.toString(), charset);
            return load(reader);
        } catch (IOException | XMLStreamException e) {
            String msg = "Unable to load from file " + filePath;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    public Stream<T> load(Reader in) throws XMLStreamException, IOException {

        XMLInputFactory factory = XMLInputFactory.newFactory();
        XMLStreamReader streamReader = factory.createXMLStreamReader(in);

        streamReader.next();
        checkStartElement(streamReader);
        logger.info("Document root is " + streamReader.getLocalName());

        final ItemIterator itemIterator = new ItemIterator(streamReader, concreteType);

        Iterable<T> iterable = () -> itemIterator;

        final Stream<T> stream = StreamSupport.stream(iterable.spliterator(), false);

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(itemIterator::close);

        return stream;
    }

    private void checkStartElement(XMLStreamReader streamReader) {
        if (XMLStreamConstants.START_ELEMENT != streamReader.getEventType()) {
            logger.error("Expected first start element");
        }
    }

    private class ItemIterator implements Iterator<T> {
        private final XMLStreamReader xmlStreamReader;
        private final XmlMapper mapper;
        private final Class<T> theType;
        private boolean closed;
        private boolean insideContainingElement;
        private final String containingElement;
        private final String elementName;

        public ItemIterator(XMLStreamReader xmlStreamReader, Class<T> theType) {
            this.theType = theType;
            mapper = XmlMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
            this.xmlStreamReader = xmlStreamReader;
            closed = false;
            JsonTypeName elementType = theType.getAnnotation(JsonTypeName.class);
            JsonRootName container = theType.getAnnotation(JsonRootName.class);

            if (elementType==null || container==null) {
                throw new RuntimeException(theType.getSimpleName() + " needs have annotations for both JsonRootName and JsonTypeName");
            }

            containingElement = container.value();
            elementName = elementType.value();
            insideContainingElement = false;
            logger.info(format("Created ItemIterator for element %s container %s to populate type %s",
                    elementName, containingElement, theType.getSimpleName()));
        }

        @Override
        public boolean hasNext() {
            if (closed) {
                return false;
            }

            try {

                if (!insideContainingElement) {
                    if (!findElement(containingElement)) {
                        logger.warn("Failed to find containing element " + containingElement);
                        return false;
                    }
                    logger.info("Found containing element " + containingElement);
                }

                insideContainingElement = true;

                return findElement(elementName);
            }
            catch(XMLStreamException e) {
                logger.error("Exception during interation", e);
                return false;
            }
        }

        private boolean findElement(String name) throws XMLStreamException {
            // loop until find containing element, or no more tokens
            while (xmlStreamReader.hasNext() && (!foundStartOfElement(name))) {
                xmlStreamReader.next();
            }

            return xmlStreamReader.hasNext();
        }

        private boolean foundStartOfElement(String name) {
            if (xmlStreamReader.getEventType()!=XMLStreamConstants.START_ELEMENT) {
                return false;
            }
            return name.equals(xmlStreamReader.getLocalName());
        }

        @Override
        public T next() {
            try {
                return mapper.readValue(xmlStreamReader, theType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            logger.info("Closed ItemIterator");
            try {
                closed = true;
                xmlStreamReader.close();
            } catch (XMLStreamException e) {
                final String msg = "Unable to close ItemIterator";
                logger.error(msg);
                throw new RuntimeException(msg, e);
            }
        }
    }

}
