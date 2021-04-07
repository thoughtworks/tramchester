package com.tramchester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.FetchDataFromUrlAndUnzip;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.NodeIdLabelMap;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.graphbuild.GraphFilter;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataFactory;

public class Module<T extends TransportDataFactory>  extends AbstractModule {
    private static final CsvMapper csvMapper;
    private static final ObjectMapper objectMapper;

    private final GuiceContainerDependencies<T> parent;
    private final GraphFilter filter;
    private final TramchesterConfig config;
    private final CacheMetrics.RegistersCacheMetrics registersCacheMetrics;
    private final Class<T> factoryType;

    static {
        csvMapper = CsvMapper.builder().addModule(new AfterburnerModule()).build();
        objectMapper = new ObjectMapper();
    }

    public Module(GuiceContainerDependencies<T> parent, GraphFilter filter, TramchesterConfig config,
                  CacheMetrics.RegistersCacheMetrics registersCacheMetrics, Class<T> factoryType) {
        this.parent = parent;
        this.filter = filter;
        this.config = config;
        this.registersCacheMetrics = registersCacheMetrics;
        this.factoryType = factoryType;
    }

    @Override
    protected void configure() {
        parent.registerComponents(config, filter, registersCacheMetrics);
        bindClass(NodeContentsRepository.class, CachedNodeOperations.class);
        bindClass(NodeTypeRepository.class, NodeIdLabelMap.class);
        bindClass(TransportDataFactory.class, factoryType);
    }

    @SuppressWarnings("unused")
    @Provides
    TransportData providesTransportdata(TransportDataFactory transportDataFactory) {
        return transportDataFactory.getData();
    }

    @Provides
    StationsAndLinksGraphBuilder.Ready providesReadyToken(StationsAndLinksGraphBuilder graphBuilder) {
        return graphBuilder.getReady();
    }

    @Provides
    StagedTransportGraphBuilder.Ready providesReadyToken(StagedTransportGraphBuilder graphBuilder) {
        return graphBuilder.getReady();
    }

    @Provides
    FetchDataFromUrl.Ready providesReadyToken(FetchDataFromUrl fetchDataFromUrl) {
        return fetchDataFromUrl.getReady();
    }

    @Provides
    UnzipFetchedData.Ready providesReadyToken(UnzipFetchedData unzipFetchedData) {
        return unzipFetchedData.getReady();
    }

    @Provides
    CsvMapper providesCsvMapper() {
        return csvMapper;
    }

    @Provides
    ObjectMapper providesObjectMapper() {
        return objectMapper;
    }

    public <I, T extends I> void bindClass(Class<I> face, Class<T> klass) {
        bind(face).to(klass);
    }

    public <T> void bindInstance(Class<T> klass, T instance) {
        bind(klass).toInstance(instance);
    }

}
