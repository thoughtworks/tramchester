package com.tramchester.dataimport;

import com.google.inject.ImplementedBy;

import java.util.List;

@ImplementedBy(TransportDataReaderFactory.class)
public interface TransportDataLoader {
    List<TransportDataReader> getReaders();
}
