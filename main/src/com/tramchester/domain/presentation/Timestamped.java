package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonDeserializerAsMillis;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonSerializeAsMillis;

import java.time.LocalDateTime;

/***
 * Support for cookies on front-end
 */
@SuppressWarnings("unused")
public class Timestamped  {
    private LocalDateTime when; // Serialised as Millis since epoch
    private IdForDTO id;

    public Timestamped() {
        // deserialisation
    }

    public Timestamped(IdFor<Station> id, LocalDateTime when) {
        this(new IdForDTO(id), when);
    }

    public Timestamped(Location<?> location, LocalDateTime when) {
        this(IdForDTO.createFor(location), when);
        if (location.getLocationType()!= LocationType.Station) {
            throw new RuntimeException("Only recent stations support in cookies currently");
        }
    }

    public Timestamped(IdForDTO id, LocalDateTime when) {
        this.id = id;
        this.when = when;
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializeAsMillis.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializerAsMillis.class)
    public LocalDateTime getWhen() {
        return when;
    }

    public IdForDTO getId() {
        return id;
    }

    public void setId(IdForDTO id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Timestamped that = (Timestamped) o;

        // JUST ID?
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // not using comparable as that causes equality issues, we just want ordering by time and NOT equality by time
    public static int compare(Timestamped first, Timestamped second) {
        return first.when.compareTo(second.when);
    }
}