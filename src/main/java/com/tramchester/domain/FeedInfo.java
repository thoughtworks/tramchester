package com.tramchester.domain;

public class FeedInfo {
    private String version;
    private String validFrom;
    private String validUntil;
    private String publisherName;
    private String publisherUrl;
    private String timezone;
    private String lang;

    public FeedInfo(String publisherName, String publisherUrl, String timezone, String lang, String validFrom,
                    String validUntil, String version) {
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

    public String validFrom() {
        return validFrom;
    }

    public String validUntil() {
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
