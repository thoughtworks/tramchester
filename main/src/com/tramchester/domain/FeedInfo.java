package com.tramchester.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.dataimport.data.ParsesDate;

import java.time.LocalDate;

public class FeedInfo extends ParsesDate {

    //feed_publisher_name,feed_publisher_url,feed_timezone,feed_lang,feed_valid_from,feed_valid_to,feed_version

    @JsonProperty("feed_version")
    private String version;
    @JsonProperty("feed_publisher_name")
    private String publisherName;
    @JsonProperty("feed_publisher_url")
    private String publisherUrl;
    @JsonProperty("feed_timezone")
    private String timezone;
    @JsonProperty("feed_lang")
    private String lang;

    private LocalDate validFrom;
    private LocalDate validUntil;

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

    public FeedInfo() {
        // for deserialization
    }

    @JsonProperty("feed_valid_from")
    private void setValidFrom(String text) {
        this.validFrom = parseDate(text);
    }

    @JsonProperty("feed_valid_to")
    private void setValidTo(String text) {
        this.validUntil = parseDate(text);
    }

    public String getVersion() {
        return version;
    }

    public LocalDate validFrom() {
        return validFrom;
    }

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
