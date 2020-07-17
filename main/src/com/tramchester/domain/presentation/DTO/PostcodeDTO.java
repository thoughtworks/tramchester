package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.places.ProximityGroups;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.domain.time.TramTime;

import java.io.IOException;
import java.util.Optional;

@SuppressWarnings("unused")
public class PostcodeDTO extends LocationDTO {

    public static final String PREFIX = "POSTCODE_";
    private ProximityGroup proximityGroup;

    public PostcodeDTO(PostcodeLocation postcodeLocation) {
        super(postcodeLocation);
        this.proximityGroup = ProximityGroups.POSTCODES;
    }

    public PostcodeDTO() {
        // deserialisation
    }

    public ProximityGroup getProximityGroup() {
        return proximityGroup;
    }

    public boolean getPostcode() {
        return true;
    }

    @JsonSerialize(using = PostcodeSerialize.class)
    @JsonDeserialize(using = PostcodeDeserialize.class)
    @Override
    public String getId() {
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
        public String deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException, JsonProcessingException {
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
