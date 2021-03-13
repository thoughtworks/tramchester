package com.tramchester.unit.repository;

import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.repository.TransportDataContainer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportDataContainerTest {

    @Test
    void shouldHaveCorrectModTimeForTransportMode() {
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();
        LocalDateTime baseTime = providesLocalNow.getDateTime();

        TransportDataContainer container = new TransportDataContainer(providesLocalNow, "local");
        DataSourceInfo dataSourceA = new DataSourceInfo(new DataSourceID("A"), "v1", baseTime.plusHours(1), Collections.singleton(Tram));
        DataSourceInfo dataSourceB = new DataSourceInfo(new DataSourceID("B"), "v1", baseTime.minusHours(1), Collections.singleton(Bus));
        DataSourceInfo dataSourceC = new DataSourceInfo(new DataSourceID("C"), "v1", baseTime, Collections.singleton(Tram));

        container.addDataSourceInfo(dataSourceA);
        container.addDataSourceInfo(dataSourceB);
        container.addDataSourceInfo(dataSourceC);

        assertEquals(baseTime.plusHours(1), container.getNewestModTimeFor(Tram));
        assertEquals(baseTime.minusHours(1), container.getNewestModTimeFor(Bus));

    }
}
