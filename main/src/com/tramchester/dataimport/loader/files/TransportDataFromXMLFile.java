package com.tramchester.dataimport.loader.files;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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

public class TransportDataFromXMLFile<T, R extends T> implements TransportDataFromFile<T> {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromXMLFile.class);

    private final Path filePath;
    private final Class<R> concreteType;
    private final Charset charset;

    public TransportDataFromXMLFile(Path filePath, Charset charset, Class<R> concreteType) {
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

        streamReader.next();
        checkStartElement(streamReader);
        logger.info("Container element " + streamReader.getLocalName());

        streamReader.next();

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
        private final XMLStreamReader streamReader;
        private final XmlMapper mapper;
        private final Class<R> theType;
        private boolean closed;

        public ItemIterator(XMLStreamReader streamReader, Class<R> theType) {
            this.theType = theType;
            mapper = XmlMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
            this.streamReader = streamReader;
            closed = false;
            logger.info("Created ItemIterator for " + theType.getSimpleName());
        }

        @Override
        public boolean hasNext() {
            if (closed) {
                return false;
            }
            return streamReader.getEventType() == XMLStreamConstants.START_ELEMENT && "StopPoint".equals(streamReader.getLocalName());
        }

        @Override
        public T next() {
            try {
                final T readValue = mapper.readValue(streamReader, theType);
                streamReader.next();
                return readValue;
            } catch (IOException | XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            logger.info("Closed ItemIterator");
            try {
                closed = true;
                streamReader.close();
            } catch (XMLStreamException e) {
                final String msg = "Unable to close ItemIterator";
                logger.error(msg);
                throw new RuntimeException(msg, e);
            }
        }
    }

}
