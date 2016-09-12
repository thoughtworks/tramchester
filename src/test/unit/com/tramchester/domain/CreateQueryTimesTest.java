package com.tramchester.domain;


import com.tramchester.TestConfig;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class CreateQueryTimesTest {

    private final LocalConfig config = new LocalConfig();

    CreateQueryTimes createQueryTimes = new CreateQueryTimes(config);

    @Test
    public void shouldGenerateCorrectTimes() {

        List<Integer> result = createQueryTimes.generate(800);
        assertEquals(5,result.size());

        assertEquals(800, (int)result.get(0));
        assertEquals(806, (int)result.get(1));
        assertEquals(812, (int)result.get(2));
        assertEquals(818, (int)result.get(3));
        assertEquals(824, (int)result.get(4));
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
