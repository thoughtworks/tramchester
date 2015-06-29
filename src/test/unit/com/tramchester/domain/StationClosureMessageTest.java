package com.tramchester.domain;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class StationClosureMessageTest {

    @Test
    public void shouldNotHaveClosedStations() throws Exception {
        StationClosureMessage stationClosureMessage = new StationClosureMessage(new ClosedStations());
        assertThat(stationClosureMessage.isClosed()).isFalse();
        assertThat(stationClosureMessage.getMessage()).isEmpty();
    }

    @Test
    public void shouldHaveOneClosedStation() throws Exception {
        StationClosureMessage stationClosureMessage = new StationClosureMessage(new ClosedStations(asList("St Peter Square")));
        assertThat(stationClosureMessage.isClosed()).isTrue();
        assertThat(stationClosureMessage.getMessage()).isEqualTo("St Peter Square station temporary closure.");
    }

    @Test
    public void shouldHaveMultipleClosedStation() throws Exception {
        StationClosureMessage stationClosureMessage = new StationClosureMessage(new ClosedStations(asList("St Peter Square", "Victoria")));
        assertThat(stationClosureMessage.isClosed()).isTrue();
        assertThat(stationClosureMessage.getMessage()).isEqualTo("St Peter Square, Victoria stations temporary closure.");
    }
}