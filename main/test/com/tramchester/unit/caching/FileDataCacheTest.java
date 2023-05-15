package com.tramchester.unit.caching;

import com.tramchester.caching.CachableData;
import com.tramchester.caching.FileDataCache;
import com.tramchester.caching.LoaderSaverFactory;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.data.CostsPerDegreeData;
import com.tramchester.dataimport.data.PostcodeHintData;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.geo.BoundingBox;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.tfgm.TFGMRemoteDataSourceConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class FileDataCacheTest extends EasyMockSupport  {

    static final int NUM_ROUTE_INDEX_DATA = 10000;
    static final Path cacheFolder = TestEnv.CACHE_DIR.resolve("DataCacheTest");

    public static final String DATA_INDEX_FILENAME = "cacheableTestClass.json";
    public static final String HINTS_FILENAME = "cacheableTestClassCSV.csv";

    private FileDataCache dataCache;
    private List<RouteIndexData> routeIndexTestItems;
    private List<PostcodeHintData> postcodeHintItems;
    private RemoteDataAvailable remoteDataRefreshed;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        remoteDataRefreshed = createMock(RemoteDataAvailable.class);

        LoaderSaverFactory loaderSaverFactory = new LoaderSaverFactory();
        loaderSaverFactory.start();

        dataCache = new FileDataCache(new LocalTestConfig(cacheFolder), remoteDataRefreshed, loaderSaverFactory);

        dataCache.clearFiles();

        // assume NUM_ROUTE_INDEX_DATA < Short.max

        routeIndexTestItems = ThreadLocalRandom.current().ints().
                limit(NUM_ROUTE_INDEX_DATA).
                mapToObj(i -> (short)i).
                map(number -> new RouteIndexData(number, Route.createId("route"+number))).
                collect(Collectors.toUnmodifiableList());

        postcodeHintItems = new ArrayList<>();
        postcodeHintItems.add(new PostcodeHintData("boxA", new BoundingBox(3,4,5,6)));
        postcodeHintItems.add(new PostcodeHintData("boxB", new BoundingBox(8,9,10,11)));
        postcodeHintItems.add(new PostcodeHintData("boxC", new BoundingBox(12,13,14,15)));

    }

    @AfterEach
    void onceAfterEachTestRuns() {
        dataCache.clearFiles();
    }

    @Test
    void shouldCacheRouteIndexDataToDisc() {

        TestData<RouteIndexData> toSave = new TestData<>(routeIndexTestItems, DATA_INDEX_FILENAME);
        TestData<RouteIndexData> toLoad = new TestData<>(DATA_INDEX_FILENAME);

        validateCacheClassToDisk(toSave, toLoad, RouteIndexData.class, routeIndexTestItems);
    }

    @Test
    void shouldCachePostcodeHintsToDisk() {

        TestData<PostcodeHintData> toSave = new TestData<>(postcodeHintItems, HINTS_FILENAME);
        TestData<PostcodeHintData> toLoad = new TestData<>(HINTS_FILENAME);

        validateCacheClassToDisk(toSave, toLoad, PostcodeHintData.class, postcodeHintItems);
    }

    @Test
    void shouldCacheCostsPerDegreeDataToDisk() {

        String filename = "costsPerDegreeTest.csv";
        List<CostsPerDegreeData> items = new ArrayList<>();
        items.add(new CostsPerDegreeData(1,2, asShorts(4, 5, 6, 7)));
        items.add(new CostsPerDegreeData(8,9, asShorts(1, 2, 3, 4)));
        items.add(new CostsPerDegreeData(11,12, asShorts(42,43)));

        TestData<CostsPerDegreeData> toSave = new TestData<>(items, filename);
        TestData<CostsPerDegreeData> toLoad = new TestData<>(filename);

        validateCacheClassToDisk(toSave, toLoad, CostsPerDegreeData.class, items);
    }

    @NotNull
    private List<Short> asShorts(int...values) {
        return Arrays.stream(values).boxed().map(Integer::shortValue).
                collect(Collectors.toList());
    }

    @Test
    void shouldCacheCRouteIndexDataToDiskStopAndReload() {

        TestData<RouteIndexData> cacheableToSave = new TestData<>(routeIndexTestItems, DATA_INDEX_FILENAME);
        TestData<RouteIndexData> cacheableToLoad = new TestData<>(DATA_INDEX_FILENAME);

        validateCacheToDiskStopAndReload(cacheableToSave, cacheableToLoad, RouteIndexData.class, routeIndexTestItems);
    }

    @Test
    void shouldCachePostcodeHintsToDiskStopAndReloadCSV() {

        TestData<PostcodeHintData> cacheableToSave = new TestData<>(postcodeHintItems, HINTS_FILENAME);
        TestData<PostcodeHintData> cacheableToLoad = new TestData<>(HINTS_FILENAME);

        validateCacheToDiskStopAndReload(cacheableToSave, cacheableToLoad, PostcodeHintData.class, postcodeHintItems);
    }

    @Test
    void shouldCacheRespectDataUpdated() {

        TestData<RouteIndexData> cacheableToSave = new TestData<>(routeIndexTestItems, DATA_INDEX_FILENAME);

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

    private <T extends CachableData> void validateCacheClassToDisk(TestData<T> cacheableToSave,
                                                                   TestData<T> cacheableToLoad,
                                                                   Class<T> theClass,
                                                                   List<T> items) {
        Path filePath = cacheFolder.resolve(cacheableToSave.getFilename());

        EasyMock.expect(remoteDataRefreshed.refreshed(DataSourceID.tfgm)).andStubReturn(false);

        replayAll();

        dataCache.start();
        assertFalse(dataCache.has(cacheableToSave));

        dataCache.save(cacheableToSave, theClass);
        assertTrue(filePath.toFile().exists());
        assertTrue(dataCache.has(cacheableToSave));

        // now load

        dataCache.loadInto(cacheableToLoad, theClass);
        List<T> results = cacheableToLoad.getItems();

        assertEquals(items, results);
        verifyAll();
    }

    private <T extends CachableData> void validateCacheToDiskStopAndReload(TestData<T> cacheableToSave, TestData<T> cacheableToLoad,
                                                                           Class<T> theClass, List<T> items) {
        EasyMock.expect(remoteDataRefreshed.refreshed(DataSourceID.tfgm)).andStubReturn(false);

        replayAll();

        dataCache.start();
        assertFalse(dataCache.has(cacheableToSave));

        dataCache.save(cacheableToSave, theClass);
        dataCache.stop();

        ////////////////

        dataCache.start();

        assertTrue(dataCache.has(cacheableToSave));

        dataCache.loadInto(cacheableToLoad, theClass);

        List<T> results = cacheableToLoad.getItems();

        assertEquals(items, results);

        verifyAll();
    }

    private static class TestData<T extends CachableData> implements FileDataCache.CachesData<T> {

        private final List<T> list;
        private final String filename;

        private TestData(String filename) {
            this(new ArrayList<>(), filename);
        }

        public TestData(List<T> items, String filename) {
            this.list = items;
            this.filename = filename;
        }

        @Override
        public void cacheTo(DataSaver<T> saver) {
            saver.open();
            list.forEach(saver::write);
            saver.close();

        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public void loadFrom(Stream<T> stream) {
            stream.forEach(list::add);
        }

        public List<T> getItems() {
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
