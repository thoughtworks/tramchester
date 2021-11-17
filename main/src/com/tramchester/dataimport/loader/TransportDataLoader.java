package com.tramchester.dataimport.loader;

import com.google.inject.ImplementedBy;

import java.util.List;

@ImplementedBy(TransportDataLoaderFiles.class)
public interface TransportDataLoader {
    List<TransportDataReader> getReaders();
}
