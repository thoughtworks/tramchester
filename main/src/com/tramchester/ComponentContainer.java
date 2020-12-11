package com.tramchester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.FetchInstanceMetadata;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import com.tramchester.cloud.SignalToCloudformationReady;
import com.tramchester.cloud.data.*;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.places.MyLocationFactory;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.SortsPositions;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.*;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.search.*;
import com.tramchester.healthchecks.*;
import com.tramchester.livedata.LiveDataHTTPFetcher;
import com.tramchester.livedata.LiveDataUpdater;
import com.tramchester.livedata.TramPositionInference;
import com.tramchester.mappers.*;
import com.tramchester.repository.*;
import com.tramchester.resources.*;
import com.tramchester.router.ProcessPlanRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static java.lang.String.format;

public abstract class ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(ComponentContainer.class);

    // init dependencies but possibly with alternative source of transport data
    public abstract void initContainer();

    public abstract <T> T get(Class<T> klass);

    protected abstract <T> Set<T> getAll(Class<T> klass);

    protected abstract <T> void addComponent(Class<T> klass);

    protected abstract <T> void addComponent(Class<T> klass, T instance);

    protected abstract <I,T extends I> void addComponent(Class<I> face, Class<T> concrete);

    protected abstract void stop();

    public void initialise() {
        logger.info("Init");
        initContainer();
    }

    public Set<ReportsCacheStats> getCacheStatReporters() {
        return getAll(ReportsCacheStats.class);
    }

    public Set<APIResource> getResources() {
        Set<APIResource> results = getAll(APIResource.class);
        logger.info("Got " + results.size() + " API Resources");
        return results;
    }

    public Set<TramchesterHealthCheck> getHealthChecks() {
        Set<TramchesterHealthCheck> healthChecks = getAll(TramchesterHealthCheck.class);
        Set<HealthCheckFactory> healthCheckFactorys = getAll(HealthCheckFactory.class);
        healthCheckFactorys.forEach(healthCheckFactory -> healthChecks.addAll(healthCheckFactory.getHealthChecks()));

        return healthChecks;
    }

    public void close() {
        logger.info("Dependencies close");

        logger.info("Begin cache stats");
        Set<ReportsCacheStats> components = getReportCacheStats();
        components.forEach(component -> reportCacheStats(component.getClass().getSimpleName(), component.stats()));
        logger.info("End cache stats");

        logger.info("Stop components");
        stop();

        logger.info("Dependencies closed");
        System.gc(); // for tests which accumulate/free a lot of memory
    }

    private Set<ReportsCacheStats> getReportCacheStats() {
        return getAll(ReportsCacheStats.class);
    }

    private void reportCacheStats(String className, List<Pair<String, CacheStats>> stats) {
        stats.forEach(stat -> logger.info(format("%s: %s: %s", className, stat.getLeft(), stat.getRight().toString())));
    }

    protected void registerResources() {
        addComponent(StationResource.class);
        addComponent(PostcodeResource.class);
        addComponent(DeparturesResource.class);
        addComponent(VersionResource.class);
        addComponent(JourneysForGridResource.class);
        addComponent(JourneyPlannerResource.class);
        addComponent(TramPositionsResource.class);
        addComponent(RouteResource.class);
        addComponent(DataVersionResource.class);
    }

    protected void registerConfiguration(TramchesterConfig configuration, GraphFilter graphFilter) {
        addComponent(ProvidesNow.class, ProvidesLocalNow.class);
        addComponent(TramchesterConfig.class, configuration);
        addComponent(GraphFilter.class, graphFilter);
        addComponent(DefaultDataLoadStrategy.class);
    }

    protected void registerComponents() {
        logger.info("Register components");

        addComponent(ObjectMapper.class, new ObjectMapper());

        addComponent(StationLocations.class);
        addComponent(PostcodeBoundingBoxs.class);
        addComponent(PostcodeDataImporter.class);
        addComponent(Unzipper.class);
        addComponent(URLDownloadAndModTime.class);
        addComponent(FetchFileModTime.class);
        addComponent(TransportDataReaderFactory.class);
        addComponent(FetchDataFromUrl.class);
        addComponent(PostcodeRepository.class);
        addComponent(VersionRepository.class);
        addComponent(DeparturesMapper.class);
        addComponent(TransportModeRepository.class);
        addComponent(CreateQueryTimes.class);

        //addComponent(ServiceHeuristics.class);

        addComponent(RouteCalculator.class);
        addComponent(FastestRoutesForBoxes.class);
        addComponent(RouteCalculatorArriveBy.class);
        addComponent(ProcessPlanRequest.class);
        addComponent(ProvidesNotes.class);
        addComponent(TramJourneyToDTOMapper.class);
        addComponent(UpdateRecentJourneys.class);
        addComponent(SortsPositions.class);
        addComponent(StagedTransportGraphBuilder.class);

        addComponent(ConfigFromInstanceUserData.class);
        addComponent(FetchInstanceMetadata.class);
        addComponent(SignalToCloudformationReady.class);
        addComponent(MapPathToStages.class);
        addComponent(MapPathToLocations.class);
        addComponent(LocationJourneyPlanner.class);
        addComponent(SendMetricsToCloudWatch.class);
        addComponent(RoutesMapper.class);
        addComponent(LiveDataHTTPFetcher.class);
        addComponent(LiveDataParser.class);
        addComponent(PlatformMessageRepository.class);
        addComponent(DueTramsRepository.class);
        addComponent(RouteToLineMapper.class);
        addComponent(LiveDataUpdater.class);
        addComponent(ClientForS3.class);
        addComponent(S3Keys.class);
        addComponent(StationDepartureMapper.class);
        addComponent(UploadsLiveData.class);
        addComponent(DownloadsLiveData.class);
        addComponent(MyLocationFactory.class);
        addComponent(RouteReachable.class);
        addComponent(RouteCostCalculator.class);
        addComponent(TramReachabilityRepository.class);

        addComponent(CachedNodeOperations.class);
//        addComponent(NodeContentsDirect.class);

        addComponent(NodeIdLabelMap.class);
//        addComponent(NodeTypeDirect.class);

        addComponent(GraphQuery.class);
        addComponent(TramStationAdjacenyRepository.class);
        addComponent(StageDTOFactory.class);
        addComponent(JourneyDTOFactory.class);
        addComponent(TramPositionInference.class);
        addComponent(GraphHealthCheck.class);
        addComponent(DataExpiryHealthCheckFactory.class);
        addComponent(LiveDataHealthCheck.class);
        addComponent(NewDataAvailableHealthCheckFactory.class);
        addComponent(LiveDataMessagesHealthCheck.class);
        addComponent(InterchangeRepository.class);
        addComponent(GraphDatabase.class);
        addComponent(RouteCallingStations.class);
        addComponent(TramCentralZoneDirectionRespository.class);
        addComponent(CreateNeighbours.class);
        addComponent(TransportDataFromFilesBuilder.class);
    }

}
