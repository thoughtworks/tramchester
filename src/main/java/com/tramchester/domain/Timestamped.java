package com.tramchester.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.TimeJsonSerializer;
import org.joda.time.DateTime;

public class Timestamped  {
    private DateTime when;
    private String id;

    public DateTime getWhen() {
        return when;
    }

    public void setWhen(DateTime when) {
        this.when = when;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamped() {
        // deserialisation
    }

    public Timestamped(String id, DateTime when) {
        this.id = id;
        this.when = when;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Timestamped that = (Timestamped) o;

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
