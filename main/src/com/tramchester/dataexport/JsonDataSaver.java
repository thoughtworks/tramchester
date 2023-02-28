package com.tramchester.dataexport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class JsonDataSaver<T> implements DataSaver<T> {
    private static final Logger logger = LoggerFactory.getLogger(JsonDataSaver.class);

    private final Path filePath;
    private final ObjectMapper mapper;

    private Writer<T> writer;

    public JsonDataSaver(Path filePath, ObjectMapper mapper) {
        this.filePath = filePath;
        this.mapper = mapper;
    }

    @Override
    public void write(T itemToSave) {
        if (writer==null) {
            String message = "Writer is not ready for " + filePath.toAbsolutePath();
            logger.error(message);
            throw new RuntimeException(message);
        }
        try {
            writer.write(itemToSave);
        } catch (IOException e) {
            String message = String.format("Could not write %s to %s", itemToSave, filePath.toAbsolutePath());
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public void open() {
        if (writer!=null) {
            throw new RuntimeException("Already open for " + filePath.toAbsolutePath());
        }

        writer = new Writer<>(filePath, mapper);
        logger.info("Open for " + filePath.toAbsolutePath());
    }

    @Override
    public void close() {
        writer.close();
        writer = null;
        logger.info("Closed for " + filePath.toAbsolutePath());
    }

    private static class Writer<T> {
        private final BufferedOutputStream bufferedOutputStream;
        private final Path path;
        private final SequenceWriter sequenceWriter;

        private Writer(Path path, ObjectMapper mapper) {
            this.path = path;
            try {
                FileOutputStream outputStream = new FileOutputStream(path.toFile());
                bufferedOutputStream = new BufferedOutputStream(outputStream);
                sequenceWriter = mapper.writer().writeValues(bufferedOutputStream);

            } catch (IOException e) {
                String msg = "Cannot save to cache for " + path.toAbsolutePath();
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        public void write(T itemToWrite) throws IOException {
            sequenceWriter.write(itemToWrite);
        }

        public void close() {
            try {
                sequenceWriter.close();
                bufferedOutputStream.close();
            } catch (IOException e) {
                String msg = "Unable to close for " + path.toAbsolutePath();
                logger.error(msg,e);
                throw new RuntimeException(msg);
            }
        }


    }
}
