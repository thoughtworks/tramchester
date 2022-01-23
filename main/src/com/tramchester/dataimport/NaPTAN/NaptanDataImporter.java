package com.tramchester.dataimport.NaPTAN;

import java.util.stream.Stream;

public interface NaptanDataImporter<R> {
    void start();

    boolean isEnabled();

    void stop();

    Stream<R> getDataStream();
}
