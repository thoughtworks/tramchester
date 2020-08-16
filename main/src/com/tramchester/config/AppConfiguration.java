package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.StationClosure;
import com.tramchester.geo.BoundingBox;
import io.dropwizard.bundles.assets.AssetsConfiguration;
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
    @JsonProperty("numOfNearestStops")
    private Integer numOfNearestStops;

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
    @JsonProperty("liveDataUrl")
    private String liveDataUrl;

    @NotNull
    @JsonProperty("liveDataSubscriptionKey")
    private String liveDataSubscriptionKey;

    @NotNull
    @JsonProperty("liveDataS3Bucket")
    private String liveDataS3Bucket;

    @NotNull
    @JsonProperty("removeRouteNameSuffix")
    private Boolean removeRouteNameSuffix;

    @NotNull
    @JsonProperty("liveDataRefreshPeriodSeconds")
    private Long liveDataRefreshPeriodSeconds;

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

    @NotNull
    @JsonProperty("maxNumberMissingLiveMessages")
    private Integer maxNumberMissingLiveMessages;

    @Valid
    @JsonProperty("dataSources")
    private List<DataSourceConfig> dataSourceConfig;

    @NotNull
    @JsonProperty("stationClosures")
    private List<StationClosure> stationClosures;

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
    public String getGraphName() { return graphName; }


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

    @Valid
    @Override
    public BoundingBox getBounds() {
        return bounds;
    }

}
