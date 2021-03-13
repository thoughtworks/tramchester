package com.tramchester.dataimport;

import com.tramchester.repository.TransportDataFactory;

public interface DataLoadStrategy {
    TransportDataFactory getTransportDataFactory();
}
