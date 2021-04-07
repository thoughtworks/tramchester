package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;

@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class RemoteDataSourceAppConfig extends Configuration implements RemoteDataSourceConfig {

    @NotNull
    @JsonProperty(value = "dataCheckURL")
    private String dataCheckURL;

    @NotNull
    @JsonProperty(value = "dataURL")
    private String dataURL;

    @NotNull
    @JsonProperty(value = "dataPath")
    private Path dataPath;

    @NotNull
    @JsonProperty(value = "filename")
    private String filename;

    @NotNull
    @JsonProperty(value = "name")
    private String name;

    @Override
    public String getDataCheckUrl() {
        return dataCheckURL;
    }

    @Override
    public String getDataUrl() {
        return dataURL;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public String getDownloadFilename() {
        return filename;
    }

    @Override
    public String getName() {
        return name;
    }
}
