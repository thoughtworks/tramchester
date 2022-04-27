package com.tramchester.livedata.domain.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateTimeJsonSerializer;
import com.tramchester.mappers.serialisation.LocalTimeJsonSerializer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@SuppressWarnings("unused")
@JsonPropertyOrder(alphabetic = true)
public class DepartureDTO implements Comparable<DepartureDTO> {

    // TODO Make from and destintaion StationRefDTO?
    private TransportMode transportMode;
    private String from;
    private String destination;
    private String carriages;
    private String status;
    private LocalDateTime dueTime;

    @JsonIgnore
    private LocalDateTime lastUpdated;

    public DepartureDTO(Location<?> from, UpcomingDeparture upcomingDeparture, LocalDateTime updateTime) {
        this(upcomingDeparture.getMode(), from.getName(), upcomingDeparture.getDestination().getName(),
                upcomingDeparture.getCarriages(), upcomingDeparture.getStatus(),
                upcomingDeparture.getWhen().toDate(updateTime.toLocalDate()), updateTime);
    }

    private DepartureDTO(TransportMode mode, String from, String destination, String carriages, String status, LocalDateTime dueTime,
                         LocalDateTime lastUpdated) {
        this.transportMode = mode;
        this.from = from;
        this.destination = destination;
        this.carriages = carriages;
        this.status = status;
        this.dueTime = dueTime;
        this.lastUpdated = lastUpdated;
    }

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

    public TransportMode getTransportMode() {
        return transportMode;
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
                "transportMode=" + transportMode +
                ", from='" + from + '\'' +
                ", destination='" + destination + '\'' +
                ", carriages='" + carriages + '\'' +
                ", status='" + status + '\'' +
                ", dueTime=" + dueTime +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartureDTO that = (DepartureDTO) o;
        return transportMode == that.transportMode && from.equals(that.from) && destination.equals(that.destination)
                && carriages.equals(that.carriages) && status.equals(that.status)
                && dueTime.equals(that.dueTime) && lastUpdated.equals(that.lastUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transportMode, from, destination, carriages, status, dueTime, lastUpdated);
    }

    @JsonProperty(value = "wait", access = JsonProperty.Access.READ_ONLY)
    public int getWait() {
        Duration duration = Duration.between(lastUpdated.truncatedTo(ChronoUnit.MINUTES), dueTime);
        long minutes = duration.toMinutes();

        if (minutes<0) {
            return 0;
        }

        return (int) minutes;
    }

}
