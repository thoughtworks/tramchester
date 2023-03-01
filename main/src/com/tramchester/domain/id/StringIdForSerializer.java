package com.tramchester.domain.id;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.tramchester.domain.CoreDomain;

import java.io.IOException;

public class StringIdForSerializer<T extends CoreDomain> extends StdSerializer<StringIdFor<T>>  {

    static final String TYPE_FIELD = "type";
    static final String CONTENTS_FIELD = "id";

    protected StringIdForSerializer(Class<StringIdFor<T>> t) {
            super(t);
        }

    protected StringIdForSerializer() {
        this(null);
    }

    @Override
    public void serialize(StringIdFor<T> value, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
        Class<T> type = value.getDomainType();
        String text = value.getContainedId();
        jsonGenerator.writeStringField(TYPE_FIELD, type.getCanonicalName());
        jsonGenerator.writeStringField(CONTENTS_FIELD, text);
    }

    @Override
    public void serializeWithType(StringIdFor<T> value, JsonGenerator jsonGenerator, SerializerProvider provider,
                                  TypeSerializer serializer) throws IOException {

        serializer.writeTypePrefix(jsonGenerator, serializer.typeId(value, JsonToken.START_OBJECT));
        serialize(value, jsonGenerator, provider);
        serializer.writeTypeSuffix(jsonGenerator, serializer.typeId(value, JsonToken.START_OBJECT));
    }
}

