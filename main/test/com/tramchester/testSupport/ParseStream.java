package com.tramchester.testSupport;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ParseStream<T> {

    private final JsonFactory jsonFactory;

    public ParseStream(ObjectMapper mapper) {
        jsonFactory = mapper.getFactory();
    }

    @NotNull
    public List<T> receive(Response response, InputStream inputStream, Class<T> valueType) throws IOException {
        List<T> received = new ArrayList<>();

        try (final JsonParser jsonParser = jsonFactory.createParser(inputStream)) {
            final JsonToken nextToken = jsonParser.nextToken();

            if (JsonToken.START_ARRAY.equals(nextToken)) {
                // Iterate through the objects of the array.
                JsonToken current = jsonParser.nextToken();
                while (JsonToken.START_OBJECT.equals(current)) {
                    JsonToken next = jsonParser.nextToken();
                    if (JsonToken.FIELD_NAME.equals(next)) {
                        final T item = jsonParser.readValueAs(valueType);
                        received.add(item);
                    }
                    current = jsonParser.nextToken();
                }
            }

        }
        inputStream.close();
        response.close();
        return received;
    }
}
