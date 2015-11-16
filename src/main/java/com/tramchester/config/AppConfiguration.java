package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration extends TramchesterConfig {
    @JsonProperty("rebuildGraph")
    private boolean rebuildGraph;

    @JsonProperty("pullData")
    private boolean pullData;

    @JsonProperty("graphName")
    private String graphName;

    @JsonProperty("closedStations")
    private List<String> closedStations;

    @JsonProperty("instanceDataUrl")
    private String instanceDataUrl;

    @JsonProperty("tramDataUrl")
    private String tramDataUrl;

    @JsonProperty("agencies")
    private List<String> agencies;

    @JsonProperty("filterData")
    private boolean filterData;

    public String getInstanceDataBaseURL() {
        return instanceDataUrl;
    }

    @Override
    public String getTramDataUrl() {
        return tramDataUrl;
    }

    public boolean isRebuildGraph() {
        return rebuildGraph;
    }

    public boolean isPullData() {
        return pullData;
    }

    @Override
    public boolean isFilterData() {
        return filterData;
    }

    public String getGraphName() { return graphName; }

    public List<String> getClosedStations() {
        return closedStations == null ? new ArrayList<>() : closedStations;
    }

    @Override
    public List<String> getAgencies() {
        return agencies;
    }


}
