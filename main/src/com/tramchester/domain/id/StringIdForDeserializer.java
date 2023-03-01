package com.tramchester.domain.id;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.tramchester.domain.CoreDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.tramchester.domain.id.StringIdForSerializer.CONTENTS_FIELD;
import static com.tramchester.domain.id.StringIdForSerializer.TYPE_FIELD;
import static java.lang.String.format;

public class StringIdForDeserializer<T extends CoreDomain> extends StdDeserializer<IdFor<T>>  {
    private static final Logger logger = LoggerFactory.getLogger(StringIdForDeserializer.class);

    protected StringIdForDeserializer(Class<?> vc) {
        super(vc);
    }

    protected StringIdForDeserializer() {
        this(null);
    }

    @Override
    public IdFor<T> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        Map<String, JsonNode> fields = new HashMap<>();
        String logPostfix = format(" current node '%s' current name '%s'", node.toPrettyString(), jsonParser.currentName());

        if (node.isObject()) {
            node.fields().forEachRemaining(item -> fields.put(item.getKey(),item.getValue()));
        } else {
            String msg = "Did not find an object" + logPostfix;
            logger.error(msg);
            throw new JsonParseException(jsonParser, msg);
        }

        guardForField(jsonParser, fields, logPostfix, TYPE_FIELD);
        guardForField(jsonParser, fields, logPostfix, CONTENTS_FIELD);

        String domainTypeName = fields.get(TYPE_FIELD).asText();
        String contents = fields.get(CONTENTS_FIELD).asText();

        try {
            Class<?> domainClass = Class.forName(domainTypeName);
            return createIdFor(jsonParser, domainClass, contents);
        } catch (ClassNotFoundException e) {
            String msg = "Could not find class for " + domainTypeName + logPostfix;
            logger.error(msg);
            throw new JsonParseException(jsonParser, msg, e);
        }

    }

    private void guardForField(JsonParser jsonParser, Map<String, JsonNode> fields, String logPostfix, String fieldName) throws JsonParseException {
        if (!fields.containsKey(fieldName)) {
            String msg = "Could not find " + fieldName + " in " + fields + logPostfix;
            logger.error(msg);
            throw new JsonParseException(jsonParser, msg);
        }
    }

    private IdFor<T> createIdFor(JsonParser parser, Class<?> domainClass, String contents) throws JsonParseException {

        if (!CoreDomain.class.isAssignableFrom(domainClass)) {
            throw new JsonParseException(parser, domainClass.getCanonicalName() + " does not implement CoreDomain");
        }

        // T extends CoreDomain so should be safe
        Class<T> coreDomainClass = (Class<T>) domainClass.asSubclass(CoreDomain.class);

        return StringIdFor.createId(contents, coreDomainClass);

    }
}
