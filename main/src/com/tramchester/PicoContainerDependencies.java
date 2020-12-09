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
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.behaviors.Caching;
import org.picocontainer.injectors.FactoryInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class PicoContainerDependencies implements ComponentContainer {
    private static final Logger logger = LoggerFactory.getLogger(PicoContainerDependencies.class);

    private final MutablePicoContainer picoContainer = new DefaultPicoContainer(new Caching());

    public PicoContainerDependencies(GraphFilter graphFilter, TramchesterConfig configuration) {
        logger.info("Creating dependencies");

        picoContainer.addComponent(TramchesterConfig.class, configuration);
        picoContainer.addComponent(GraphFilter.class, graphFilter);
        picoContainer.addComponent(DefaultDataLoadStrategy.class);

        registerComponents();
    }

    @Override
    public void initialise() {
        DefaultDataLoadStrategy defaultDataStrategy = picoContainer.getComponent(DefaultDataLoadStrategy.class);
        initialise(defaultDataStrategy.getProvider());
    }

    @Override
    public void initialise(TransportDataProvider transportDataProvider) {
        logger.info("initialise");

        picoContainer.addAdapter(new FactoryInjector<TransportData>() {
            @Override
            public TransportData getComponentInstance(PicoContainer container, Type into) {
                return transportDataProvider.getData();
            }
        });

        if (logger.isDebugEnabled()) {
            logger.warn("Debug logging is enabled, server performance will be impacted");
        }
        
        logger.info("Start components");
        picoContainer.start();
    }

    @Override
    public <T> T get(Class<T> klass) {

        T component = picoContainer.getComponent(klass);
        if (component==null) {
            logger.warn("Missing dependency " + klass);
        }
        return component;
    }

    @Override
    public List<APIResource> getResources() {
        return picoContainer.getComponents(APIResource.class);
    }

    @Override
    public void close() {
        logger.info("Dependencies close");

        logger.info("Begin cache stats");
        List<ReportsCacheStats> components = getReportCacheStats();
        components.forEach(component -> reportCacheStats(component.getClass().getSimpleName(), component.stats()));
        logger.info("End cache stats");

        logger.info("Components stop");
        picoContainer.stop();
        logger.info("Components dispose");
        picoContainer.dispose();

        logger.info("Dependencies closed");
        System.gc(); // for tests which accumulate/free a lot of memory
    }

    private List<ReportsCacheStats> getReportCacheStats() {
        return picoContainer.getComponents(ReportsCacheStats.class);
    }

    private void reportCacheStats(String className, List<Pair<String, CacheStats>> stats) {
        stats.forEach(stat -> logger.info(format("%s: %s: %s", className, stat.getLeft(), stat.getRight().toString())));
    }

    @Override
    public List<TramchesterHealthCheck> getHealthChecks() {
        List<TramchesterHealthCheck> healthChecks = new ArrayList<>(picoContainer.getComponents(TramchesterHealthCheck.class));
        List<HealthCheckFactory> healthCheckFactorys = picoContainer.getComponents(HealthCheckFactory.class);
        healthCheckFactorys.forEach(healthCheckFactory -> healthChecks.addAll(healthCheckFactory.getHealthChecks()));

        return healthChecks;
    }

    @Override
    public List<ReportsCacheStats> getHasCacheStat() {
        return picoContainer.getComponents(ReportsCacheStats.class);
    }

    private void registerComponents() {
        picoContainer.addComponent(ProvidesLocalNow.class);
        picoContainer.addComponent(StationLocations.class);
        picoContainer.addComponent(PostcodeBoundingBoxs.class);
        picoContainer.addComponent(PostcodeDataImporter.class);
        picoContainer.addComponent(Unzipper.class);
        picoContainer.addComponent(URLDownloadAndModTime.class);
        picoContainer.addComponent(FetchFileModTime.class);
        picoContainer.addComponent(TransportDataReaderFactory.class);
        picoContainer.addComponent(FetchDataFromUrl.class);
        picoContainer.addComponent(PostcodeRepository.class);
        picoContainer.addComponent(VersionRepository.class);
        picoContainer.addComponent(StationResource.class);
        picoContainer.addComponent(PostcodeResource.class);
        picoContainer.addComponent(DeparturesResource.class);
        picoContainer.addComponent(DeparturesMapper.class);
        picoContainer.addComponent(VersionResource.class);
        picoContainer.addComponent(TransportModeRepository.class);

        // WIP
        picoContainer.addComponent(JourneysForGridResource.class);

        picoContainer.addComponent(CreateQueryTimes.class);
        picoContainer.addComponent(JourneyPlannerResource.class);
        picoContainer.addComponent(TramPositionsResource.class);
        picoContainer.addComponent(ServiceHeuristics.class);

        picoContainer.addComponent(RouteCalculator.class);
        picoContainer.addComponent(FastestRoutesForBoxes.class);
        picoContainer.addComponent(RouteCalculatorArriveBy.class);
        picoContainer.addComponent(ProcessPlanRequest.class);
        picoContainer.addComponent(ProvidesNotes.class);
        picoContainer.addComponent(TramJourneyToDTOMapper.class);
        picoContainer.addComponent(UpdateRecentJourneys.class);
        picoContainer.addComponent(SortsPositions.class);
        picoContainer.addComponent(StagedTransportGraphBuilder.class);

        picoContainer.addComponent(ConfigFromInstanceUserData.class);
        picoContainer.addComponent(FetchInstanceMetadata.class);
        picoContainer.addComponent(SignalToCloudformationReady.class);
        picoContainer.addComponent(MapPathToStages.class);
        picoContainer.addComponent(MapPathToLocations.class);
        picoContainer.addComponent(LocationJourneyPlanner.class);
        picoContainer.addComponent(SendMetricsToCloudWatch.class);
        picoContainer.addComponent(DataVersionResource.class);
        picoContainer.addComponent(RoutesMapper.class);
        picoContainer.addComponent(RouteResource.class);
        picoContainer.addComponent(LiveDataHTTPFetcher.class);
        picoContainer.addComponent(LiveDataParser.class);
        picoContainer.addComponent(PlatformMessageRepository.class);
        picoContainer.addComponent(DueTramsRepository.class);
        picoContainer.addComponent(RouteToLineMapper.class);
        picoContainer.addComponent(LiveDataUpdater.class);
        picoContainer.addComponent(ClientForS3.class);
        picoContainer.addComponent(S3Keys.class);
        picoContainer.addComponent(StationDepartureMapper.class);
        picoContainer.addComponent(UploadsLiveData.class);
        picoContainer.addComponent(DownloadsLiveData.class);
        picoContainer.addComponent(MyLocationFactory.class);
        picoContainer.addComponent(RouteReachable.class);
        picoContainer.addComponent(RouteCostCalculator.class);
        picoContainer.addComponent(TramReachabilityRepository.class);

        picoContainer.addComponent(CachedNodeOperations.class);
//        picoContainer.addComponent(NodeContentsDirect.class);

        picoContainer.addComponent(NodeIdLabelMap.class);
//        picoContainer.addComponent(NodeTypeDirect.class);

        picoContainer.addComponent(GraphQuery.class);
        picoContainer.addComponent(TramStationAdjacenyRepository.class);
        picoContainer.addComponent(new ObjectMapper());
        picoContainer.addComponent(StageDTOFactory.class);
        picoContainer.addComponent(JourneyDTOFactory.class);
        picoContainer.addComponent(TramPositionInference.class);
        picoContainer.addComponent(GraphHealthCheck.class);
        picoContainer.addComponent(DataExpiryHealthCheckFactory.class);
        picoContainer.addComponent(LiveDataHealthCheck.class);
        picoContainer.addComponent(NewDataAvailableHealthCheckFactory.class);
        picoContainer.addComponent(LiveDataMessagesHealthCheck.class);
        picoContainer.addComponent(InterchangeRepository.class);
        picoContainer.addComponent(GraphDatabase.class);
        picoContainer.addComponent(RouteCallingStations.class);
        picoContainer.addComponent(TramCentralZoneDirectionRespository.class);
        picoContainer.addComponent(CreateNeighbours.class);
        picoContainer.addComponent(TransportDataProviderFactory.class);
    }

}
