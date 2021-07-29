package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class GTFSSourceAppConfig extends Configuration implements GTFSSourceConfig {

    @NotNull
    @JsonProperty(value = "name")
    private String name;

    @NotNull
    @JsonProperty("dataPath")
    private Path dataPath;

    @NotNull
    @JsonProperty("hasFeedInfo")
    private Boolean hasFeedInfo;

    @NotNull
    @JsonProperty("transportModes")
    private Set<GTFSTransportationType> transportModes;

    @NotNull
    @JsonProperty("transportModesWithPlatforms")
    private Set<TransportMode> transportModesWithPlatforms;

    // date format: 2020-12-25
    @NotNull
    @JsonProperty("noServices")
    private Set<LocalDate> noServices;

    @NotNull
    @JsonProperty("additionalInterchanges")
    private Set<String> additionalInterchanges;

    @NotNull
    @JsonProperty("compositeStationModes")
    private Set<TransportMode> compositeStationModes;

    @NotNull
    @JsonProperty("stationClosures")
    private List<StationClosure> closures;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean getHasFeedInfo() {
        return hasFeedInfo;
    }

    @Override
    public Set<GTFSTransportationType> getTransportGTFSModes() {
        return transportModes;
    }

    @Override
    public Set<TransportMode> getTransportModesWithPlatforms() {
        return transportModesWithPlatforms;
    }

    @Override
    public Set<LocalDate> getNoServices() {
        return noServices;
    }

    @Override
    public Set<String> getAdditionalInterchanges() {
        return additionalInterchanges;
    }

    @Override
    public Set<TransportMode> compositeStationModes() {
        return compositeStationModes;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public List<StationClosure> getStationClosures() {
        return closures;
    }

}
