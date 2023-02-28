package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdFor;

import java.io.IOException;

public class IdForSerializer<T extends CoreDomain> extends StdSerializer<IdFor<T>> {
    protected IdForSerializer(Class<IdFor<T>> t) {
        super(t);
    }

    protected IdForSerializer() {
        this(null);
    }

    @Override
    public void serialize(IdFor<T> idFor, JsonGenerator gen, SerializerProvider provider) throws IOException {

    }
}
