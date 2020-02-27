package com.tramchester.unit.domain;


import com.tramchester.testSupport.TestConfig;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.TramTime;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class CreateQueryTimesTest {

    private final LocalConfig config = new LocalConfig();

    CreateQueryTimes createQueryTimes = new CreateQueryTimes(config);

    @Test
    public void shouldGenerateCorrectTimesForTrams() {

        TramTime seedTime = TramTime.of(13, 20);
        List<TramTime> result = createQueryTimes.generate(seedTime, false);
        assertEquals(3,result.size());

        assertEquals(seedTime, result.get(0));
        assertEquals(seedTime.plusMinutes(12), result.get(1));
        assertEquals(seedTime.plusMinutes(24), result.get(2));
    }

    @Test
    public void shouldGenerateCorrectTimesForWalkAtStart() {

        TramTime seedTime = TramTime.of(13, 20);
        List<TramTime> result = createQueryTimes.generate(seedTime, true);
        assertEquals(1,result.size());

        assertEquals(seedTime, result.get(0));
    }

    private class LocalConfig extends TestConfig {
        @Override
        public Path getDataFolder() {
            return null;
        }

        @Override
        public String getGraphName() {
            return null;
        }

        @Override
        public Set<String> getAgencies() {
            return null;
        }

        @Override
        public boolean getCreateLocality() {
            return false;
        }
    }
}
