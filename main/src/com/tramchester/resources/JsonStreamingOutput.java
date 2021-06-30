package com.tramchester.resources;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

class JsonStreamingOutput<T> implements StreamingOutput {
    private static final Logger logger = LoggerFactory.getLogger(JsonStreamingOutput.class);

    private final Stream<T> theStream;
    private final Transaction txn;

    private final JsonFactory jsonFactory ;

    JsonStreamingOutput(Transaction txn, Stream<T> theStream, ObjectMapper mapper) {
        this.txn = txn;
        this.theStream = theStream;
        jsonFactory = mapper.getFactory();
    }

    JsonStreamingOutput(Stream<T> theStream, ObjectMapper mapper) {
        this(null, theStream, mapper);
    }

    /**
     * Writes theStream to outputStream, closes theStream and the txn (if present)
     * @param outputStream the stream being written to
     */
    @Override
    public void write(final OutputStream outputStream)  {
        // NOTE: by default there is an 8K output buffer on outputStream

        logger.info("Write stream to response");

        theStream.onClose(() -> {
            logger.info("Closed source stream");
            if (txn!=null) {
                logger.info("Closing transaction");
                txn.close();
            }
        });


        try (final JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream)) {
            jsonGenerator.writeStartArray();
            theStream.forEach(item -> {
                synchronized (outputStream) {
                    try {
                        jsonGenerator.writeObject(item);
                        jsonGenerator.writeString(System.lineSeparator());
                        jsonGenerator.writeString(System.lineSeparator());
                        jsonGenerator.flush();
                    } catch (IOException innerException) {
                        logger.error("Exception during streaming item " + item.toString(), innerException);
                    }
                }
            });
            jsonGenerator.writeEndArray();
            jsonGenerator.flush();
        }
        catch (IOException ioException) {
           logger.warn("Exception during streaming", ioException);
        } finally {
            theStream.close();
            logger.info("Stream closed");
        }
    }
}
