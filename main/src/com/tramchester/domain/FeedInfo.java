package com.tramchester.domain;

import java.time.LocalDate;

public class FeedInfo {
    private final String version;
    private final LocalDate validFrom;
    private final LocalDate validUntil;
    private final String publisherName;
    private final String publisherUrl;
    private final String timezone;
    private final String lang;

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
