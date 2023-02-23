package com.tramchester.unit.caching;

import com.tramchester.caching.DataCache;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DataCacheTest extends EasyMockSupport  {

    static final int SIZE = 10000;
    static final Path cacheFolder = TestEnv.CACHE_DIR.resolve("DataCacheTest");

    private DataCache dataCache;
    private List<RouteIndexData> testItems;
    private RemoteDataAvailable remoteDataRefreshed;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        remoteDataRefreshed = createMock(RemoteDataAvailable.class);
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

        CacheableTestClass cacheableToSave = new CacheableTestClass(testItems);
        CacheableTestClass cacheableToLoad = new CacheableTestClass();

        EasyMock.expect(remoteDataRefreshed.refreshed(DataSourceID.tfgm)).andStubReturn(false);

        replayAll();

        dataCache.start();
        assertFalse(dataCache.has(cacheableToSave));

        dataCache.save(cacheableToSave, RouteIndexData.class);
        assertTrue(cacheFolder.resolve(cacheableToSave.getFilename()).toFile().exists());
        assertTrue(dataCache.has(cacheableToSave));

        // now load

        dataCache.loadInto(cacheableToLoad, RouteIndexData.class);
        List<RouteIndexData> results = cacheableToLoad.getItems();

        assertEquals(testItems, results);
        verifyAll();
    }

    @Test
    void shouldCacheClassToDiskStopAndReload() {

        CacheableTestClass cacheableToSave = new CacheableTestClass(testItems);
        CacheableTestClass cacheableToLoad = new CacheableTestClass();

        EasyMock.expect(remoteDataRefreshed.refreshed(DataSourceID.tfgm)).andStubReturn(false);

        replayAll();

        dataCache.start();
        assertFalse(dataCache.has(cacheableToSave));

        dataCache.save(cacheableToSave, RouteIndexData.class);
        dataCache.stop();

        ////////////////

        dataCache.start();

        assertTrue(dataCache.has(cacheableToSave));

        dataCache.loadInto(cacheableToLoad, RouteIndexData.class);

        List<RouteIndexData> results = cacheableToLoad.getItems();

        assertEquals(testItems, results);

        verifyAll();
    }

    @Test
    void shouldCacheRespectDataUpdated() {

        CacheableTestClass cacheableToSave = new CacheableTestClass(testItems);

        EasyMock.expect(remoteDataRefreshed.refreshed(DataSourceID.tfgm)).andReturn(false);
        EasyMock.expect(remoteDataRefreshed.refreshed(DataSourceID.tfgm)).andReturn(true);

        replayAll();

        dataCache.start();
        assertFalse(dataCache.has(cacheableToSave));

        dataCache.save(cacheableToSave, RouteIndexData.class);
        dataCache.stop();

        ////////////////

        dataCache.start();

        assertFalse(dataCache.has(cacheableToSave));

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
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return Collections.singletonList(new TFGMRemoteDataSourceConfig(Path.of("fake")));
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }
    }
}
