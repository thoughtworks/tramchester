package com.tramchester.integration.testSupport.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.rail.*;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.dataimport.rail.repository.RailStationCRSRepository;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.naptan.NaptanRepository;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.stream.Stream;

public class LoadRailServicesFromText  {

    private final TramchesterConfig config;
    private final RailRouteIDBuilder railRouteIdBuilder;
    private final CacheMetrics cacheMetric;
    private final RailDataRecordFactory railDataRecordFactory;
    private final UnzipFetchedData.Ready ready;
    private final ProvidesRailStationRecords providesRailStationRecords;
    private final RailStationCRSRepository crsRepository;
    private final NaptanRepository naptanRepository;
    private final GraphFilterActive filter;

    public LoadRailServicesFromText(TramchesterConfig config, ComponentContainer componentContainer, UnzipFetchedData.Ready ready) {
        this.config = config;
        this.ready = ready;

        railRouteIdBuilder = componentContainer.get(RailRouteIDBuilder.class);
        cacheMetric = componentContainer.get(CacheMetrics.class);

        crsRepository = componentContainer.get(RailStationCRSRepository.class);
        naptanRepository = componentContainer.get(NaptanRepository.class);
        filter = componentContainer.get(GraphFilterActive.class);
        providesRailStationRecords = componentContainer.get(ProvidesRailStationRecords.class);
        railDataRecordFactory = componentContainer.get(RailDataRecordFactory.class);

    }

    public void loadInto(TransportDataContainer dataContainer, String text) {

        ProvidesRailTimetableRecords loadTimeTableRecords = new LocalRailRecords(config, railDataRecordFactory, ready, text);

        RailConfig railConfig = config.getRailConfig();

        RailRouteIdRepository railRouteIdRepository = new RailRouteIdRepository(loadTimeTableRecords, railRouteIdBuilder, config,
                cacheMetric);
        railRouteIdRepository.start();

        RailTransportDataFromFiles.Loader loader = new RailTransportDataFromFiles.Loader(providesRailStationRecords, loadTimeTableRecords,
                railRouteIdRepository, crsRepository, naptanRepository, railConfig, filter);

        loader.loadInto(dataContainer, config.getBounds());

    }

    private static class LocalRailRecords extends LoadRailTimetableRecords {
        private final String text;

        public LocalRailRecords(TramchesterConfig config, RailDataRecordFactory factory, UnzipFetchedData.Ready ready, String text) {
            super(config, factory, ready);
            this.text = text;
        }

        @Override
        public Stream<RailTimetableRecord> load() {
            String padded = padTo80Cols();
            Reader paddedReader = new StringReader(padded);
            return super.load(paddedReader);
        }

        @NotNull
        private String padTo80Cols() {
            // since cut and paste of example text into the tests messes up columns
            Reader reader = new StringReader(text);
            BufferedReader bufferedReader = new BufferedReader(reader);
            StringBuilder padded = new StringBuilder();
            bufferedReader.lines().forEach(line -> {
                String clean = line.stripTrailing();
                padded.append(StringUtils.rightPad(clean, 80)).append(System.lineSeparator());
            });
            return padded.toString();
        }
    }



}
