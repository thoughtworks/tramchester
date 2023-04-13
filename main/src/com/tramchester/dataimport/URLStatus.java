package com.tramchester.dataimport;

import org.apache.http.HttpStatus;

import java.net.URI;
import java.time.LocalDateTime;

public class URLStatus {

    private final String url;
    private final int responseCode;
    private final LocalDateTime modTime;

    public URLStatus(String url, int responseCode) {
        this(url, responseCode, LocalDateTime.MIN);
    }

    public URLStatus(String url, int responseCode, LocalDateTime modTime) {
        this.url = url;
        this.responseCode = responseCode;
        this.modTime = modTime;
    }

    public URLStatus(URI uri, int responseCode) {
        this(uri, responseCode, LocalDateTime.MIN);
    }

    public URLStatus(URI uri, int responseCode, LocalDateTime modTime) {
        this(uri.toASCIIString(), responseCode, modTime);
    }

    public LocalDateTime getModTime() {
        return modTime;
    }

    public boolean isOk() {
        return HttpStatus.SC_OK == responseCode;
    }

    public int getStatusCode() {
        return responseCode;
    }

    public String getActualURL() {
        return url;
    }

    @Override
    public String toString() {
        return "URLStatus{" +
                "url='" + url + '\'' +
                ", responseCode=" + responseCode +
                ", modTime=" + modTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        URLStatus urlStatus = (URLStatus) o;

        if (responseCode != urlStatus.responseCode) return false;
        if (!url.equals(urlStatus.url)) return false;
        return modTime.equals(urlStatus.modTime);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + responseCode;
        result = 31 * result + modTime.hashCode();
        return result;
    }

    public boolean isRedirect() {
        return isRedirectCode(responseCode);
    }

    public static boolean isRedirectCode(int code) {
        return code == HttpStatus.SC_MOVED_PERMANENTLY
                || code == HttpStatus.SC_MOVED_TEMPORARILY
                || code == HttpStatus.SC_TEMPORARY_REDIRECT;
    }

}
