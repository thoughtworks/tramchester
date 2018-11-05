package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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

    @JsonProperty("dataPath")
    private Path dataPath;

    @JsonProperty("timeWindow")
    private int timeWindow;

    @JsonProperty("showMyLocation")
    private boolean showMyLocation;

    @JsonProperty("nearestStopRangeKM")
    private Double nearestStopRangeKM;

    @JsonProperty("numOfNearestStops")
    private int numOfNearestStops;

    @JsonProperty("walkingMPH")
    private double walkingMPH;

    @JsonProperty("createLocality")
    private boolean createLocality;

    @JsonProperty("redirectHTTP")
    private boolean redirectHTTP;

    @JsonProperty("secureHost")
    private String secureHost;

    @Deprecated
    @JsonProperty("addWalkingRoutes")
    private boolean addWalkingRoutes;

    @JsonProperty("maxWait")
    private int maxWait;

    @JsonProperty("queryInterval")
    private int queryInterval;

    @JsonProperty("recentStopsToShow")
    private int recentStopsToShow;

    @JsonProperty("swagger")
    private SwaggerBundleConfiguration swaggerBundleConfiguration;

    @JsonProperty("dataExpiryThreadhold")
    private int dataExpiryThreadhold;

    @JsonProperty("liveDataUrl")
    private String liveDataUrl;

    @JsonProperty("liveDataSubscriptionKey")
    private String liveDataSubscriptionKey;

    public String getInstanceDataUrl() {
        return instanceDataUrl;
    }

    @Override
    public String getTramDataUrl() {
        return tramDataUrl;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public int getMaxWait() {
        return maxWait;
    }

    @Override
    public int getQueryInterval() {
        return queryInterval;
    }

    @Override
    public int getRecentStopsToShow() {
        return recentStopsToShow;
    }

    @Override
    public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
        return swaggerBundleConfiguration;
    }

    @Override
    public int getDataExpiryThreadhold() {
        return dataExpiryThreadhold;
    }

    @Override
    public String getLiveDataUrl() {
        return liveDataUrl;
    }

    @Override
    public String getLiveDataSubscriptionKey() { return liveDataSubscriptionKey; }

    @Override
    public boolean getRedirectHTTP() {
        return redirectHTTP;
    }

    @Override
    public String getSecureHost() {
        return secureHost;
    }

    @Override
    public boolean getAddWalkingRoutes() {
        return addWalkingRoutes;
    }

    @Override
    public int getTimeWindow() {
        return timeWindow;
    }

    @Override
    public Double getNearestStopRangeKM() {
        return nearestStopRangeKM;
    }

    @Override
    public int getNumOfNearestStops() {
        return numOfNearestStops;
    }

    @Override
    public double getWalkingMPH() {
        return walkingMPH;
    }

    @Override
    public boolean getRebuildGraph() {
        return rebuildGraph;
    }

    @Override
    public boolean getPullData() {
        return pullData;
    }

    @Override
    public boolean getCreateLocality() { return createLocality; }

    public String getGraphName() { return graphName; }

    public List<String> getClosedStations() {
        return closedStations == null ? new ArrayList<>() : closedStations;
    }

    @Override
    public Set<String> getAgencies() {
        return new HashSet<>(agencies);
    }

}
