package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.GTFSTransportationType;
import io.dropwizard.bundles.assets.AssetsConfiguration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class AppConfiguration extends TramchesterConfig {

    @NotNull
    @JsonProperty("rebuildGraph")
    private boolean rebuildGraph;

    @NotNull
    @JsonProperty("graphName")
    private String graphName;

    @JsonProperty("closedStations")
    private List<String> closedStations;

    @NotNull
    @JsonProperty("instanceDataUrl")
    private String instanceDataUrl;

    @NotNull
    @JsonProperty("loadPostcodes")
    private boolean loadPostcodes;

    @JsonProperty("postcodeDataPath")
    private Path postcodeDataPath;

    @JsonProperty("postcodeZip")
    private Path postcodeZip;

    @NotNull
    @JsonProperty("nearestStopRangeKM")
    private Double nearestStopRangeKM;

    @NotNull
    @JsonProperty("numOfNearestStops")
    private int numOfNearestStops;

    @NotNull
    @JsonProperty("numOfNearestStopsForWalking")
    private int numOfNearestStopsForWalking;

    @NotNull
    @JsonProperty("walkingMPH")
    private double walkingMPH;

    @JsonProperty("secureHost")
    private String secureHost;

    @Deprecated
    @JsonProperty("addWalkingRoutes")
    private boolean addWalkingRoutes;

    @NotNull
    @JsonProperty("maxWait")
    private int maxWait;

    @NotNull
    @JsonProperty("queryInterval")
    private int queryInterval;

    @NotNull
    @JsonProperty("recentStopsToShow")
    private int recentStopsToShow;

    @Valid
    @JsonProperty("swagger")
    private SwaggerBundleConfiguration swaggerBundleConfiguration;

    @JsonProperty("dataExpiryThreadhold")
    private int dataExpiryThreadhold;

    @JsonProperty("liveDataUrl")
    private String liveDataUrl;

    @NotNull
    @JsonProperty("liveDataSubscriptionKey")
    private String liveDataSubscriptionKey;

    @JsonProperty("liveDataS3Bucket")
    private String liveDataS3Bucket;

    @JsonProperty("removeRouteNameSuffix")
    private boolean removeRouteNameSuffix;

    @JsonProperty("liveDataRefreshPeriodSeconds")
    private long liveDataRefreshPeriodSeconds;

    @NotNull
    @JsonProperty("maxJourneyDuration")
    private int maxJourneyDuration;

    @NotNull
    @JsonProperty("changeAtInterchangeOnly")
    private boolean changeAtInterchangeOnly;

    @NotNull
    @JsonProperty("maxNumberResults")
    private int maxNumberResults;

    @JsonProperty("numberQueries")
    private int numberQueries;

    @NotNull
    @JsonProperty("createNeighbours")
    private boolean createNeighbours;

    @NotNull
    @JsonProperty("distanceToNeighboursKM")
    private double distanceToNeighboursKM;

    @NotNull
    @JsonProperty("maxNumberMissingLiveMessages")
    private int maxNumberMissingLiveMessages;

    @JsonProperty("transportModes")
    private List<GTFSTransportationType> transportModes;

    @Valid
    @JsonProperty("dataSources")
    private List<DataSourceConfig> dataSourceConfig;

    public String getInstanceDataUrl() {
        return instanceDataUrl;
    }

    @Override
    public int getMaxWait() {
        return maxWait;
    }

    @Override
    public int getMaxNumResults() {
        return maxNumberResults;
    }

    @Override
    public int getNumberQueries() {
        return numberQueries;
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

    @Valid
    @NotNull
    @JsonProperty
    private final AssetsConfiguration assets = AssetsConfiguration.builder().build();

    @Override
    public AssetsConfiguration getAssetsConfiguration() {
        return assets;
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
    public String getSecureHost() {
        return secureHost;
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
    public int getNumOfNearestStopsForWalking() {
        return numOfNearestStopsForWalking;
    }

    @Override
    public double getWalkingMPH() {
        return walkingMPH;
    }

    @Override
    public boolean getRebuildGraph() {
        return rebuildGraph;
    }

    public String getGraphName() { return graphName; }

    public List<String> getClosedStations() {
        return closedStations == null ? new ArrayList<>() : closedStations;
    }

    @Override
    public List<GTFSTransportationType> getTransportModes() {
        return transportModes;
    }

    @Override
    public String getLiveDataS3Bucket() {
        return liveDataS3Bucket.toLowerCase();
    }

    @Override
    public boolean getRemoveRouteNameSuffix() {
        return removeRouteNameSuffix;
    }

    @Override
    public long getLiveDataRefreshPeriodSeconds() { return liveDataRefreshPeriodSeconds; }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return changeAtInterchangeOnly;
    }

    @Override
    public int getMaxJourneyDuration() {
        return maxJourneyDuration;
    }

    @Override
    public int getMaxNumberMissingLiveMessages() {
        return maxNumberMissingLiveMessages;
    }

    @Override
    public boolean getLoadPostcodes() {
        return loadPostcodes;
    }

    @Override
    public boolean getCreateNeighbours() {
        return createNeighbours;
    }

    @Override
    public double getDistanceToNeighboursKM() {
        return distanceToNeighboursKM;
    }

    @Override
    public Path getPostcodeDataPath() {
        return postcodeDataPath;
    }

    @Override
    public Path getPostcodeZip() {
        return postcodeZip;
    }

    @Override
    public List<DataSourceConfig> getDataSourceConfig() {
        return dataSourceConfig;
    }

}
