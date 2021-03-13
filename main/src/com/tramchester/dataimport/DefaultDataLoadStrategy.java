package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.repository.TransportDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class DefaultDataLoadStrategy implements DataLoadStrategy {
    private static final Logger logger = LoggerFactory.getLogger(DefaultDataLoadStrategy.class);

    private TransportDataFactory provider=null;

    private final FetchDataFromUrl fetcher;
    private final Unzipper unzipper;

    private DefaultDataLoadStrategy(FetchDataFromUrl fetcher, Unzipper unzipper) {
        this.fetcher = fetcher;
        this.unzipper = unzipper;
    }

    public void start() {
        logger.info("start");
        fetcher.fetchData(unzipper);
        //provider = builder.create();
        logger.info("started");
    }

    @Override
    public TransportDataFactory getTransportDataFactory() {
//        if (provider==null) {
//            logger.info("Download and unzip data");
//            fetcher.fetchData(unzipper);
//
//            logger.info("Create TransportData provider");
//            provider = builder.create();
//        } else {
//            logger.info("Provider cached");
//        }

        return provider;
    }
}
