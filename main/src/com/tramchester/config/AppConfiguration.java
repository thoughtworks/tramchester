package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.geo.BoundingBox;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("unused")
@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class AppConfiguration extends TramchesterConfig {

    /////////
    //
    // Use Boxed types here to get @NotNull checking (i.e. Boolean not boolean)
    //
    ////////

    @NotNull
    @JsonProperty("staticAssetCacheTimeSeconds")
    private Integer staticAssetCacheTimeSeconds;

    @NotNull
    @JsonProperty("instanceDataUrl")
    private String instanceDataUrl;

    @NotNull
    @JsonProperty("nearestStopRangeKM")
    private Double nearestStopRangeKM;

    @NotNull
    @JsonProperty("nearestStopForWalkingRangeKM")
    private Double nearestStopForWalkingRangeKM;

    @NotNull
    @JsonProperty("numOfNearestStopsToOffer")
    private Integer numOfNearestStopsToOffer;

    @NotNull
    @JsonProperty("numOfNearestStopsForWalking")
    private Integer numOfNearestStopsForWalking;

    @NotNull
    @JsonProperty("walkingMPH")
    private Double walkingMPH;

    @NotNull
    @JsonProperty("secureHost")
    private String secureHost;

    @NotNull
    @JsonProperty("maxWait")
    private Integer maxWait;

//    @NotNull
//    @JsonProperty("maxInitialWait")
//    private Integer maxInitialWait;

    @NotNull
    @JsonProperty("queryInterval")
    private Integer queryInterval;

    @NotNull
    @JsonProperty("recentStopsToShow")
    private Integer recentStopsToShow;

    @Valid
    @JsonProperty("swagger")
    private SwaggerBundleConfiguration swaggerBundleConfiguration;

    @JsonProperty("dataExpiryThreadhold")
    private Integer dataExpiryThreadhold;
    
    @NotNull
    @JsonProperty("maxJourneyDuration")
    private Integer maxJourneyDuration;

    @NotNull
    @JsonProperty("changeAtInterchangeOnly")
    private Boolean changeAtInterchangeOnly;

    @NotNull
    @JsonProperty("maxNumberResults")
    private Integer maxNumberResults;

    @NotNull
    @JsonProperty("numberQueries")
    private Integer numberQueries;

    @NotNull
    @JsonProperty("maxWalkingConnections")
    private Integer maxWalkingConnections;

    @Valid
    @JsonProperty("gtfsSourceConfig")
    private List<GTFSSourceConfig> gtfsSourceConfig;

    @Valid
    @JsonProperty("remoteSources")
    private List<RemoteDataSourceConfig> remoteDataConfig;

    @NotNull
    @JsonProperty("graphDBConfig")
    private GraphDBConfig graphDBConfig;

    @JsonProperty("tfgmTramliveData")
    private TfgmTramLiveDataAppConfig tramLiveDataAppConfig;

    @JsonProperty("openLdb")
    private OpenLdbAppConfig openLdbConfig;

    @JsonProperty("rail")
    private RailAppConfig railConfig;

    @NotNull
    @JsonProperty("bounds")
    private BoundingBox bounds;

    @NotNull
    @JsonProperty("sendCloudWatchMetrics")
    private Boolean sendCloudWatchMetrics;

    @NotNull
    @JsonProperty("cacheFolder")
    private Path cacheFolder;

    @NotNull
    @JsonProperty("calcTimeoutMillis")
    private Long calcTimeoutMillis;

    @NotNull
    @JsonProperty("planningEnabled")
    private Boolean planningEnabled;

    @NotNull
    @JsonProperty("cloudWatchMetricsFrequencyMinutes")
    private Long cloudWatchMetricsFrequencyMinutes;

    @NotNull
    @JsonProperty("distributionBucket")
    private String distributionBucket;

    @JsonProperty("neighbourConfig")
    private NeighbourAppConfig neighbourConfig;

    @Override
    public String getInstanceDataUrl() {
        return instanceDataUrl;
    }

    @Override
    public int getMaxWait() {
        return maxWait;
    }

//    @Override
//    public int getMaxInitialWait() {
//        return maxInitialWait;
//    }

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

    @Override
    public int getDataExpiryThreadhold() {
        return dataExpiryThreadhold;
    }

    @Override
    public String getSecureHost() {
        return secureHost;
    }

    @Override
    public Double getNearestStopRangeKM() {
        return nearestStopRangeKM;
    }

    @Override
    public Double getNearestStopForWalkingRangeKM() {
        return nearestStopForWalkingRangeKM;
    }

    @Override
    public int getNumOfNearestStopsToOffer() {
        return numOfNearestStopsToOffer;
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
    public Integer getStaticAssetCacheTimeSeconds() {
        return staticAssetCacheTimeSeconds;
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return changeAtInterchangeOnly;
    }

    @Override
    public NeighbourConfig getNeighbourConfig() {
        return neighbourConfig;
    }

    @Override
    public int getMaxJourneyDuration() {
        return maxJourneyDuration;
    }

    @Valid
    @Override
    public List<GTFSSourceConfig> getGTFSDataSource() {
        return gtfsSourceConfig;
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return remoteDataConfig;
    }

    // optional
    @Override
    public TfgmTramLiveDataConfig getLiveDataConfig() {
        return tramLiveDataAppConfig;
    }

    @Override
    public OpenLdbConfig getOpenldbwsConfig() {
        return openLdbConfig;
    }

    @Override
    public RailConfig getRailConfig() {
        return railConfig;
    }

    @Valid
    @Override
    public GraphDBConfig getGraphDBConfig() {
        return graphDBConfig;
    }

    @Valid
    @Override
    public BoundingBox getBounds() {
        return bounds;
    }

    @Override
    public int getMaxWalkingConnections() {
        return maxWalkingConnections;
    }

    @Override
    public boolean getSendCloudWatchMetrics() {
        return sendCloudWatchMetrics;
    }

    @Override
    public Path getCacheFolder() {
        return cacheFolder;
    }

    @Override
    public long getCalcTimeoutMillis() {
        return calcTimeoutMillis;
    }

    @Override
    public long GetCloudWatchMetricsFrequencyMinutes() {
        return cloudWatchMetricsFrequencyMinutes;
    }

    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }

    @Override
    public boolean hasNeighbourConfig() {
        return neighbourConfig!=null;
    }

    @Override
    public String getDistributionBucket() {
        return distributionBucket;
    }

}
