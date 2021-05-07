package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.places.Station;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonSerializer;
import com.tramchester.mappers.serialisation.LocalTimeJsonSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(value = "when", allowGetters = true)
public class DepartureDTO implements Comparable<DepartureDTO> {

    // TODO Make from and destintaion StationRefDTO?
    private String from;
    private String destination;
    private String carriages;
    private String status;
    private LocalDateTime dueTime;
    private int wait;

    public DepartureDTO(Station from, DueTram dueTram, LocalDate queryDate) {
        this(from.getName(), dueTram.getDestination().getName(), dueTram.getCarriages(), dueTram.getStatus(),
                dueTram.getWhen().toDate(queryDate), dueTram.getWait());
    }

    private DepartureDTO(String from, String destination, String carriages, String status, LocalDateTime dueTime, int wait) {
        this.from = from;
        this.destination = destination;
        this.carriages = carriages;
        this.status = status;
        this.dueTime = dueTime;
        this.wait = wait;
    }

    @SuppressWarnings("unused")
    public DepartureDTO() {
        // for deserialisation
    }

    @JsonSerialize(using = LocalDateTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalDateTimeJsonDeserializer.class)
    public LocalDateTime getDueTime() {
        return dueTime;
    }

    @JsonProperty(value = "when", access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = LocalTimeJsonSerializer.class)
    public LocalTime getWhenForLiveUpload() {
        // for keeping upload of live data consistent, not ideal but lots of historical data in S3
        return dueTime.toLocalTime();
    }

    public String getFrom() {
        return from;
    }

    public String getCarriages() {
        return carriages;
    }

    public String getStatus() {
        return status;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public int compareTo(DepartureDTO other) {
        if (dueTime.equals(other.dueTime)) {
            // if same time use string ordering
            return destination.compareTo(other.destination);
        }
        // time ordering
        return dueTime.compareTo(other.dueTime);
    }

    @Override
    public String toString() {
        return "DepartureDTO{" +
                "from='" + from + '\'' +
                ", carriages='" + carriages + '\'' +
                ", status='" + status + '\'' +
                ", destination='" + destination + '\'' +
                ", when=" + dueTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartureDTO that = (DepartureDTO) o;
        return from.equals(that.from) &&
                carriages.equals(that.carriages) &&
                status.equals(that.status) &&
                destination.equals(that.destination) &&
                dueTime.equals(that.dueTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, carriages, status, destination, dueTime);
    }

    public int getWait() {
        return wait;
    }

}
