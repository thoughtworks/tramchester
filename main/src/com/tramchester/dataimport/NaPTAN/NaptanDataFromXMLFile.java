package com.tramchester.dataimport.NaPTAN;

import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
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
    private final XmlMapper mapper;
    private final XMLInputFactory factory;

    public NaptanDataFromXMLFile(Path filePath, Charset charset, Class<T> concreteType, XmlMapper mapper) {
        this.filePath = filePath.toAbsolutePath();
        this.charset = charset;
        this.concreteType = concreteType;
        this.mapper = mapper;

        factory = new WstxInputFactory();

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

        XMLStreamReader streamReader = factory.createXMLStreamReader(in);

        streamReader.next();
        checkStartElement(streamReader);
        logger.info("Document root is " + streamReader.getLocalName());

        final ItemIterator itemIterator = new ItemIterator(streamReader, mapper, concreteType);

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
        private final JavaType deserializeType;
        private final ObjectReader reader;
        private boolean closed;
        private boolean insideContainingElement;
        private final String containingElement;
        private final String elementName;

        public ItemIterator(XMLStreamReader xmlStreamReader, XmlMapper mapper, Class<T> theType) throws IOException {
            this.mapper = mapper;
            this.xmlStreamReader = xmlStreamReader;
            closed = false;

            JsonRootName container = theType.getAnnotation(JsonRootName.class);
            JsonTypeName elementType = theType.getAnnotation(JsonTypeName.class);

            TypeFactory typeFactory =  mapper.getTypeFactory();

            deserializeType = typeFactory.constructType(theType);

            if (elementType==null || container==null) {
                throw new RuntimeException(theType.getSimpleName() + " needs annotations for both JsonRootName and JsonTypeName");
            }

            containingElement = container.value();
            elementName = elementType.value();
            insideContainingElement = false;

            reader = mapper.readerFor(deserializeType);

            logger.info(format("Created ItemIterator for element %s container %s to populate type %s",
                    elementName, containingElement, theType.getSimpleName()));
        }

        @Override
        public boolean hasNext() {
            if (closed) {
                return false;
            }

            try {

                if (insideContainingElement) {
                    return findElement(elementName);
                } else if (findElement(containingElement)) {
                    insideContainingElement = true;
                    logger.info("Found containing element " + containingElement);
                    return findElement(elementName);
                } else {
                    logger.warn("Failed to find containing element " + containingElement);
                    return false;
                }

            }
            catch(XMLStreamException e) {
                logger.error("Exception during iteration", e);
                return false;
            }
        }

        private boolean findElement(String name) throws XMLStreamException {
            // skip until next start element
            while (xmlStreamReader.hasNext()) {
                if (xmlStreamReader.isStartElement() && name.equals(xmlStreamReader.getLocalName())) {
                    return true;
                } else {
                    xmlStreamReader.next();
                }

                while (xmlStreamReader.hasNext() && !xmlStreamReader.isStartElement()) {
                    xmlStreamReader.next();
                }
            }

            return false;

        }

        @Override
        public T next() {
            try {
                // done this way to allow reuse of the reader
                FromXmlParser parser = mapper.getFactory().createParser(xmlStreamReader);
                return reader.readValue(parser, deserializeType);
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
