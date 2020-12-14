package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosure;
import com.tramchester.geo.BoundingBox;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
    @JsonProperty("graphName")
    private String graphName;

    @NotNull
    @JsonProperty("staticAssetCacheTimeSeconds")
    private Integer staticAssetCacheTimeSeconds;

    @NotNull
    @JsonProperty("neo4jPagecacheMemory")
    private String neo4jPagecacheMemory;

    @NotNull
    @JsonProperty("instanceDataUrl")
    private String instanceDataUrl;

    @NotNull
    @JsonProperty("loadPostcodes")
    private Boolean loadPostcodes;

    @JsonProperty("postcodeDataPath")
    private Path postcodeDataPath;

    @JsonProperty("postcodeZip")
    private Path postcodeZip;

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
    @JsonProperty("removeRouteNameSuffix")
    private Boolean removeRouteNameSuffix;

    @NotNull
    @JsonProperty("maxJourneyDuration")
    private Integer maxJourneyDuration;

    @NotNull
    @JsonProperty("changeAtInterchangeOnly")
    private Boolean changeAtInterchangeOnly;

    @NotNull
    @JsonProperty("maxNumberResults")
    private Integer maxNumberResults;

    @JsonProperty("numberQueries")
    private Integer numberQueries;

    @NotNull
    @JsonProperty("createNeighbours")
    private Boolean createNeighbours;

    @NotNull
    @JsonProperty("distanceToNeighboursKM")
    private Double distanceToNeighboursKM;

    @Valid
    @JsonProperty("dataSources")
    private List<DataSourceConfig> dataSourceConfig;

    @NotNull
    @JsonProperty("stationClosures")
    private List<StationClosure> stationClosures;

    @NotNull
    @JsonProperty("liveData")
    private LiveDataAppConfig liveDataConfig;

    @NotNull
    @JsonProperty("bounds")
    private BoundingBox bounds;

    @Override
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

//    @Valid
//    @NotNull
//    @JsonProperty
//    private final AssetsConfiguration assets = AssetsConfiguration.builder().build();
//
//    @Override
//    public AssetsConfiguration getAssetsConfiguration() {
//        return assets;
//    }

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
    public String getGraphName() { return graphName; }

    @Override
    public String getNeo4jPagecacheMemory() {
        return neo4jPagecacheMemory;
    }

    @Override
    public boolean getRemoveRouteNameSuffix() {
        return removeRouteNameSuffix;
    }

    @Override
    public boolean getChangeAtInterchangeOnly() {
        return changeAtInterchangeOnly;
    }

    @Override
    public int getMaxJourneyDuration() {
        return maxJourneyDuration;
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

    @Valid
    @Override
    public List<DataSourceConfig> getDataSourceConfig() {
        return dataSourceConfig;
    }

    @Valid
    @Override
    public List<StationClosure> getStationClosures() {
        return stationClosures;
    }

    @Override
    public LiveDataConfig getLiveDataConfig() {
        return liveDataConfig;
    }

    @Valid
    @Override
    public BoundingBox getBounds() {
        return bounds;
    }

}
