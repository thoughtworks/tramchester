package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class NeighbourAppConfig implements  NeighbourConfig {

    public NeighbourAppConfig(@JsonProperty(value = "maxNeighbourConnections", required = true) Integer maxNeighbourConnections,
                              @JsonProperty(value = "distanceToNeighboursKM", required = true) Double distanceToNeighboursKM) {

        this.maxNeighbourConnections = maxNeighbourConnections;
        this.distanceToNeighboursKM = distanceToNeighboursKM;
    }

    private Integer maxNeighbourConnections;

    private Double distanceToNeighboursKM;

    @Override
    public double getDistanceToNeighboursKM() {
        return distanceToNeighboursKM;
    }

    @Override
    public int getMaxNeighbourConnections() {
        return maxNeighbourConnections;
    }
}
