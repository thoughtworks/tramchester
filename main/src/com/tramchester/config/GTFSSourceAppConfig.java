package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
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

    // NOTE: don't use primitive types here, blocks the null detection

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
    private Set<IdFor<Station>> additionalInterchanges;

    @NotNull
    @JsonProperty("compositeStationModes")
    private Set<TransportMode> compositeStationModes;

    @NotNull
    @JsonProperty("stationClosures")
    private List<StationClosures> closures;

    @NotNull
    @JsonProperty("addWalksForClosed")
    private Boolean addWalksForClosed;

    @NotNull
    @JsonProperty("markedInterchangesOnly")
    private Boolean markedInterchangesOnly;

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
    public IdSet<Station> getAdditionalInterchanges() {
        return IdSet.wrap(additionalInterchanges);
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
    public List<StationClosures> getStationClosures() {
        return closures;
    }

    @Override
    public boolean getAddWalksForClosed() {
        return addWalksForClosed;
    }

    @NotNull
    @Override
    public boolean getOnlyMarkedInterchanges() {
        return markedInterchangesOnly;
    }
}
