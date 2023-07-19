package com.tramchester.cloud.data;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@LazySingleton
public class LiveDataClientForS3  {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataClientForS3.class);
    private final ClientForS3 clientForS3;
    private final String bucket;

    @Inject
    public LiveDataClientForS3(TramchesterConfig config, ClientForS3 clientForS3) {
        TfgmTramLiveDataConfig liveDataConfig = config.getLiveDataConfig();
        if (liveDataConfig!=null) {
            bucket = liveDataConfig.getS3Bucket();
        } else {
            bucket = "";
        }
        this.clientForS3 = clientForS3;
    }

    @PostConstruct
    public void start() {
        if (bucket.isEmpty()) {
            logger.info("Not starting, not live data config");
        }
        logger.info("Started");
    }

    @PreDestroy
    public void stop() {
        if (bucket.isEmpty()) {
            return;
        }
        logger.info("Stopped");
    }

    /***
     * @param keys set of keys to download
     * @param responseMapper mapper function to apply to the resulting s3objects
     * @param <T> return type for the mapper and hence resulting stream
     * @return retreieved s3objects with the mappng applied
     */
    public <T> Stream<T> downloadAndMap(final Set<String> keys, ResponseMapper<T> responseMapper) {
        logger.info("Downloading data and map for " + keys.size() + " keys");
        return downloadAndMap(keys.stream(), responseMapper);
    }

    public <T> Stream<T> downloadAndMap(final Stream<String> keys, ResponseMapper<T> responseMapper) {
        if (bucket.isEmpty()) {
            logger.error("not started");
            return Stream.empty();
        }
        logger.info("Downloading keys for bucket " + bucket);
        Stream<T> stream = keys.map(key -> clientForS3.downloadAndMapForKey(bucket, key, responseMapper)).flatMap(Collection::stream);
        logger.info("Return stream");
        return stream;
    }

    public boolean isStarted() {
        return !bucket.isEmpty();
    }

    public boolean itemExists(String prefix, String item) {
        logger.info(String.format("Check for prefix '%s' item '%s'", prefix, item));
        return clientForS3.keyExists(bucket, prefix, item);
    }

    public boolean upload(String key, String json) {
        return clientForS3.upload(bucket, key, json);
    }

    public Stream<String> getKeysFor(String prefix) {
        return clientForS3.getKeysFor(bucket, prefix);
    }

    public Stream<String> getAllKeysAsStream() {
        logger.info("Getting all keys for " + bucket);
        return clientForS3.getAllKeys(bucket);
    }

    public interface ResponseMapper<T> {
        List<T> map(byte[] bytes);
    }

}
