package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateJsonSerializer;

import java.time.LocalDate;

public class DataVersionDTO {
    private String version;
    private LocalDate validFrom;
    private LocalDate validUntil;

    public DataVersionDTO() {
        // for JSON deserialisation
    }

    public DataVersionDTO(FeedInfo feedInfo, TramchesterConfig config) {
        this.validFrom = feedInfo.validFrom();
        this.validUntil = feedInfo.validUntil();
        this.version = feedInfo.getVersion();
    }

    public String getVersion() {
        return version;
    }

    @JsonSerialize(using = LocalDateJsonSerializer.class)
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public LocalDate validFrom() {
        return validFrom;
    }

    @JsonSerialize(using = LocalDateJsonSerializer.class)
    @JsonDeserialize(using = LocalDateJsonDeserializer.class)
    public LocalDate validUntil() {
        return validUntil;
    }

}
