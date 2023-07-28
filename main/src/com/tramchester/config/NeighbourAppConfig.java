package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationIdPair;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class NeighbourAppConfig implements  NeighbourConfig {

    private final Integer maxNeighbourConnections;
    private final Double distanceToNeighboursKM;
    private final List<StationPairConfig> additional;

    public NeighbourAppConfig(@JsonProperty(value = "maxNeighbourConnections", required = true) Integer maxNeighbourConnections,
                              @JsonProperty(value = "distanceToNeighboursKM", required = true) Double distanceToNeighboursKM,
                              @JsonProperty(value = "additional", required = false) List<StationPairConfig> additional) {

        this.maxNeighbourConnections = maxNeighbourConnections;
        this.distanceToNeighboursKM = distanceToNeighboursKM;
        if (additional!=null) {
            this.additional = additional;
        } else {
            this.additional = new ArrayList<>();
        }
    }

    @Override
    public double getDistanceToNeighboursKM() {
        return distanceToNeighboursKM;
    }

    @Override
    public int getMaxNeighbourConnections() {
        return maxNeighbourConnections;
    }

    @JsonProperty("additional")
    public List<StationPairConfig> getAdditionalDTO() {
        return additional;
    }

    @JsonIgnore
    @Override
    public List<StationIdPair> getAdditional() {
        return additional.stream().map(StationPairConfig::getStations).collect(Collectors.toList());
    }
}
