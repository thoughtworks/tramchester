package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.repository.TransportDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@LazySingleton
public class DefaultDataLoadStrategy implements DataLoadStrategy {
    private static final Logger logger = LoggerFactory.getLogger(DefaultDataLoadStrategy.class);

    private TransportDataProvider provider=null;

    private final TransportDataFromFilesBuilder builder;
    private final FetchDataFromUrl fetcher;
    private final Unzipper unzipper;

    @Inject
    public DefaultDataLoadStrategy(TransportDataFromFilesBuilder builder, FetchDataFromUrl fetcher, Unzipper unzipper) {
        this.builder = builder;
        this.fetcher = fetcher;
        this.unzipper = unzipper;
    }

    @Override
    public TransportDataProvider getProvider() {
        if (provider==null) {
            logger.info("Download and unzip data");
            fetcher.fetchData(unzipper);

            logger.info("Create TransportData provider");
            provider = builder.create();
        } else {
            logger.info("Provider cached");
        }

        return provider;
    }
}
