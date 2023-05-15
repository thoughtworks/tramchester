package com.tramchester.caching;

import com.google.inject.ImplementedBy;

import java.nio.file.Path;

@ImplementedBy(FileDataCache.class)
public interface DataCache {
    <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> boolean has(T cachesData);
    <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> void save(T data, Class<CACHETYPE> theClass);
    <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> void loadInto(T cachesData, Class<CACHETYPE> theClass);
    <CACHETYPE extends CachableData, T extends FileDataCache.CachesData<CACHETYPE>> Path getPathFor(T data);
}
