package com.tramchester.domain;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonDeserializerAsMillis;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonSerializeAsMillis;

import java.time.LocalDateTime;

public class Timestamped  {
    private LocalDateTime when; // Serialised as Millis since epoch
    private String id;

    @JsonSerialize(using = LocalDateTimeJsonSerializeAsMillis.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializerAsMillis.class)
    public LocalDateTime getWhen() {
        return when;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @SuppressWarnings("unused")
    public Timestamped() {
        // deserialisation
    }

    public Timestamped(StringIdFor<Station> id, LocalDateTime when) {
        this(id.forDTO(), when);
    }

    public Timestamped(String id, LocalDateTime when) {
        this.id = id;
        this.when = when;
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
