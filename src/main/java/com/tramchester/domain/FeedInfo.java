package com.tramchester.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.integration.mappers.LocalDateJsonDeserializer;
import com.tramchester.integration.mappers.LocalDateJsonSerializer;
import org.joda.time.LocalDate;

public class FeedInfo {
    private String version;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String publisherName;
    private String publisherUrl;
    private String timezone;
    private String lang;

    public FeedInfo() {
        // for JSON deserialisation
    }

    public FeedInfo(String publisherName, String publisherUrl, String timezone, String lang, LocalDate validFrom,
                    LocalDate validUntil, String version) {
        this.publisherName = publisherName;
        this.publisherUrl = publisherUrl;
        this.timezone = timezone;
        this.lang = lang;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.version = version;
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

    @Override
    public String toString() {
        return "FeedInfo{" +
                "version='" + version + '\'' +
                ", validFrom='" + validFrom + '\'' +
                ", validUntil='" + validUntil + '\'' +
                ", publisherName='" + publisherName + '\'' +
                ", publisherUrl='" + publisherUrl + '\'' +
                ", timezone='" + timezone + '\'' +
                ", lang='" + lang + '\'' +
                '}';
    }
}
