package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.graph.graphbuild.CompositeStationGraphBuilder;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataFactory;

public class GetReadyModule extends AbstractModule {
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
    CompositeStationGraphBuilder.Ready providesReadyToken(CompositeStationGraphBuilder graphBuilder) {
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

}
