package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.ProximityGroups;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class StationDTO extends LocationDTO {

    private ProximityGroup proximityGroup;

    public StationDTO(Location other, ProximityGroup proximityGroup) {
       super(other);
       this.proximityGroup = proximityGroup;
    }

    public StationDTO() {
        // deserialisation
    }

    public StationDTO(Station station, List<PlatformDTO> platformDTOS, ProximityGroup proximityGroup) {
        super(station, platformDTOS);
        this.proximityGroup = proximityGroup;
    }

    public ProximityGroup getProximityGroup() {
        return proximityGroup;
    }

    public static class Serializer extends JsonSerializer<Station> {
        @Override
        public void serialize(Station value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            StationDTO dto = new StationDTO(value, ProximityGroups.STOPS);
            gen.writeObject(dto);
        }
    }

    public static class Deserializer extends JsonDeserializer<Station> {
        @Override
        public Station deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
            StationDTO dto = jsonParser.readValueAs(StationDTO.class);
            return new Station(dto.getId(), dto.getArea(), dto.getName(), dto.getLatLong(), dto.isTram());
        }
    }

}
