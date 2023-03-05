package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.PostcodeLocationId;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.time.TramTime;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@SuppressWarnings("unused")
public class PostcodeDTO extends LocationDTO {

    private static final String PREFIX = "POSTCODE_";

    public PostcodeDTO(PostcodeLocation postcodeLocation) {
        // no platforms or routes
        super(postcodeLocation, Collections.emptyList(), Collections.emptyList());
    }

    public PostcodeDTO() {
        // deserialisation
    }

    public static boolean isPostcodeId(String startId) {
        return startId.startsWith(PREFIX);
    }

    public static PostcodeLocationId decodePostcodeId(String text) {
        String prefixRemovedText = text.replaceFirst(PostcodeDTO.PREFIX, "");
        return PostcodeLocation.createId(prefixRemovedText);
    }

    public boolean getPostcode() {
        return true;
    }

    @JsonSerialize(using = PostcodeSerialize.class)
    @JsonDeserialize(using = PostcodeDeserialize.class)
    @Override
    public IdForDTO getId() {
        return super.getId();
    }

    private static class PostcodeSerialize extends JsonSerializer<String> {
        @Override
        public void serialize(String id, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(PREFIX + id);
        }
    }

    private static class PostcodeDeserialize extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            Optional<TramTime> result;
            String idWithPrefix = node.asText();
            if (!idWithPrefix.startsWith(PREFIX)) {
                throw new IOException("Missing prefix " + PREFIX + " for value " +idWithPrefix);
            }
            return idWithPrefix.replaceFirst(PREFIX,"");
        }
    }
}
