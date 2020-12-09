package com.tramchester.dataimport;

import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.repository.TransportDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDataLoadStrategy {
    private static final Logger logger = LoggerFactory.getLogger(DefaultDataLoadStrategy.class);

    private final TransportDataProviderFactory providerFactory;
    private final FetchDataFromUrl fetcher;
    private final Unzipper unzipper;

    public DefaultDataLoadStrategy(TransportDataProviderFactory providerFactory, FetchDataFromUrl fetcher, Unzipper unzipper) {
        this.providerFactory = providerFactory;
        this.fetcher = fetcher;
        this.unzipper = unzipper;
    }

    public TransportDataProvider getProvider() {
        logger.info("Download and unzip data");
        fetcher.fetchData(unzipper);

        logger.info("Create TransportData provider");
        TransportDataFromFiles provider = providerFactory.create();
        provider.load();

        return provider;
    }
}
