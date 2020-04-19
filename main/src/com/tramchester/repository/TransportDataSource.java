package com.tramchester.repository;

import org.picocontainer.Disposable;
import org.picocontainer.Startable;

public interface TransportDataSource extends TransportData, AreasRepository, PlatformRepository, HasTrips, Startable, Disposable {
}
