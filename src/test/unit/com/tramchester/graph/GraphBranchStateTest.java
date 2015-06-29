package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphBranchStateTest {
    @Test
    public void shouldCreateAndGetCorrectly() throws Exception {
        int time = 678;
        DaysOfWeek day = DaysOfWeek.Friday;
        String dest = "destination";
        TramServiceDate queryDate = new TramServiceDate("20150630");
        GraphBranchState branchState = new GraphBranchState(time, day, dest, queryDate);

        assertThat(branchState.getTime()).isEqualTo(time);
        assertThat(branchState.getDay()).isEqualTo(day);
        assertThat(branchState.getDest()).isEqualTo(dest);
        assertThat(branchState.getQueryDate()).isEqualTo(queryDate);
    }
}