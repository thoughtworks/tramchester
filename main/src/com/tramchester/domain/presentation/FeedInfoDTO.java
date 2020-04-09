package com.tramchester.domain.presentation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.FeedInfo;
import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import com.tramchester.mappers.serialisation.LocalDateJsonSerializer;

import java.time.LocalDate;

public class FeedInfoDTO {
    private boolean bus;
    private String version;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String publisherName;
    private String publisherUrl;
    private String timezone;
    private String lang;

    public FeedInfoDTO() {
        // for JSON deserialisation
    }

    public FeedInfoDTO(FeedInfo feedInfo, TramchesterConfig config) {
        this.publisherName = feedInfo.getPublisherName();
        this.publisherUrl = feedInfo.getPublisherUrl();
        this.timezone = feedInfo.getTimezone();
        this.lang = feedInfo.getLang();
        this.validFrom = feedInfo.validFrom();
        this.validUntil = feedInfo.validUntil();
        this.version = feedInfo.getVersion();
        this.bus = config.getBus();
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

    public String getPublisherName() {
        return publisherName;
    }

    public String getPublisherUrl() {
        return publisherUrl;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getLang() {
        return lang;
    }

    public boolean getBus() {
        return bus;
    }
}
