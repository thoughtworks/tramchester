package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.GTFSTransportationType;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.Set;

@SuppressWarnings("unused")
@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class DataSourceAppConfig extends Configuration implements DataSourceConfig  {

    @NotNull
    @JsonProperty(value = "name")
    private String name;

    @NotNull
    @JsonProperty("URL")
    private String tramDataUrl;

    @NotNull
    @JsonProperty("checkURL")
    private String tramDataCheckUrl;

    @NotNull
    @JsonProperty("dataPath")
    private Path dataPath;

    @NotNull
    @JsonProperty("unzipPath")
    private Path unzipPath;

    @NotNull
    @JsonProperty("hasFeedInfo")
    private Boolean hasFeedInfo;

    @NotNull
    @JsonProperty("transportModes")
    private Set<GTFSTransportationType> transportModes;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean getHasFeedInfo() {
        return hasFeedInfo;
    }

    @Override
    public Set<GTFSTransportationType> getTransportModes() {
        return transportModes;
    }

    @Override
    public String getTramDataUrl() {
        return tramDataUrl;
    }

    @Override
    public String getTramDataCheckUrl() {
        return tramDataCheckUrl;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public Path getUnzipPath() {
        return unzipPath;
    }

    @Override
    public String getZipFilename() {
        return "data.zip";
    }

    @Override
    public String toString() {
        return "DataSourceAppConfig{" +
                "name='" + name + '\'' +
                ", tramDataUrl='" + tramDataUrl + '\'' +
                ", tramDataCheckUrl='" + tramDataCheckUrl + '\'' +
                ", dataPath=" + dataPath +
                ", unzipPath=" + unzipPath +
                '}';
    }
}
