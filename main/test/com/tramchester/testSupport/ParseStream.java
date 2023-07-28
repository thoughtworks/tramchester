package com.tramchester.testSupport;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.core.JsonToken.*;

public class ParseStream<T> {

    private final JsonFactory jsonFactory;

    public ParseStream(ObjectMapper mapper) {
        jsonFactory = mapper.getFactory();
    }

    @NotNull
    public List<T> receive(Response response, InputStream inputStream, Class<T> valueType) throws IOException {
        List<T> received = new ArrayList<>();

        try (final JsonParser jsonParser = jsonFactory.createParser(inputStream)) {
            JsonToken nextToken = jsonParser.nextToken();

            while (START_ARRAY.equals(nextToken) || START_OBJECT.equals(nextToken)) {
                if (START_OBJECT.equals(nextToken)) {
                    readObject(valueType, received, jsonParser, nextToken);
                }
                nextToken = jsonParser.nextToken();
                while (VALUE_STRING.equals(nextToken)) {
                    // consume line breaks written by server
                    nextToken = jsonParser.nextToken();
                }
            }
        }

        inputStream.close();
        response.close();
        return received;
    }

    private void readObject(Class<T> valueType, List<T> received, JsonParser jsonParser, JsonToken current) throws IOException {
        while (START_OBJECT.equals(current)) {
            JsonToken next = jsonParser.nextToken();
            if (JsonToken.FIELD_NAME.equals(next)) {
                final T item = jsonParser.readValueAs(valueType);
                received.add(item);
            }
            current = jsonParser.nextToken();
        }
    }
}
