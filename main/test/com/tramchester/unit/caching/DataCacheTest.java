package com.tramchester.unit.caching;

import com.tramchester.caching.DataCache;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.testSupport.TestConfig;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DataCacheTest extends EasyMockSupport  {

    static final int SIZE = 10000;
    static final Path cacheFolder = Path.of("testData");

    private DataCache dataCache;
    private List<RouteIndexData> testItems;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        RemoteDataRefreshed remoteDataRefreshed = createMock(RemoteDataRefreshed.class);
        dataCache = new DataCache(new LocalTestConfig(cacheFolder), remoteDataRefreshed);

        dataCache.clearFiles();

        testItems = ThreadLocalRandom.current().ints().
                limit(SIZE).
                boxed().map(number -> new RouteIndexData(number, StringIdFor.createId("route"+number))).
                collect(Collectors.toUnmodifiableList());

    }

    @AfterEach
    void onceAfterEachTestRuns() {
        dataCache.clearFiles();
    }

    @Test
    void shouldCacheClassToDisk() {

        dataCache.start();

        CacheableTestClass cacheableToSave = new CacheableTestClass(testItems);

        replayAll();

        assertFalse(dataCache.has(cacheableToSave));

        dataCache.save(cacheableToSave, RouteIndexData.class);

        assertTrue(cacheFolder.resolve(cacheableToSave.getFilename()).toFile().exists());

        assertTrue(dataCache.has(cacheableToSave));


        // now load

        CacheableTestClass cacheableToLoad = new CacheableTestClass();
        dataCache.loadInto(cacheableToLoad, RouteIndexData.class);

        List<RouteIndexData> results = cacheableToLoad.getItems();

        assertEquals(testItems, results);

        verifyAll();
    }

    @Test
    void shouldCacheClassToDiskStopAndReload() {

        dataCache.start();

        CacheableTestClass cacheableToSave = new CacheableTestClass(testItems);

        replayAll();

        assertFalse(dataCache.has(cacheableToSave));

        dataCache.save(cacheableToSave, RouteIndexData.class);

        dataCache.stop();

        ////////////////

        dataCache.start();

        assertTrue(dataCache.has(cacheableToSave));

        CacheableTestClass cacheableToLoad = new CacheableTestClass();
        dataCache.loadInto(cacheableToLoad, RouteIndexData.class);

        List<RouteIndexData> results = cacheableToLoad.getItems();

        assertEquals(testItems, results);

        verifyAll();
    }

    private static class CacheableTestClass implements DataCache.Cacheable<RouteIndexData> {

        private final List<RouteIndexData> list;

        private CacheableTestClass() {
            this(new ArrayList<>());
        }

        public CacheableTestClass(List<RouteIndexData> items) {
            this.list = items;
        }

        @Override
        public void cacheTo(DataSaver<RouteIndexData> saver) {
            saver.open();
            list.forEach(saver::write);
            saver.close();

        }

        @Override
        public String getFilename() {
            return "cacheableTestClass.csv";
        }

        @Override
        public void loadFrom(Stream<RouteIndexData> stream) {
            stream.forEach(list::add);
        }

        public List<RouteIndexData> getItems() {
            return list;
        }
    }


    private static class LocalTestConfig extends TestConfig {
        private final Path cacheFolder;

        public LocalTestConfig(Path cacheFolder) {

            this.cacheFolder = cacheFolder;
        }

        @Override
        public Path getCacheFolder() {
            return cacheFolder;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }
    }
}
