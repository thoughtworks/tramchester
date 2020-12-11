package com.tramchester.dataimport;

import com.tramchester.repository.TransportDataProvider;

public interface DataLoadStrategy {
    TransportDataProvider getProvider();
}
