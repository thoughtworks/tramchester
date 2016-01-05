package com.tramchester.graph;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class GraphBranchStateTest {
    @Test
    public void shouldCreateAndGetCorrectly() throws Exception {
        int queriedTime = 678;
        DaysOfWeek day = DaysOfWeek.Friday;
        String dest = "destination";
        TramServiceDate queryDate = new TramServiceDate("20150630");
        GraphBranchState branchState = new GraphBranchState(day, queryDate, queriedTime);

        assertThat(branchState.getQueriedTime()).isEqualTo(queriedTime);
        assertThat(branchState.getDay()).isEqualTo(day);
        //assertThat(branchState.getDest()).isEqualTo(dest);
        assertThat(branchState.getQueryDate()).isEqualTo(queryDate);

        assertFalse(branchState.hasStartTime());

        GraphBranchState newState = branchState.updateStartTime(800);
        assertTrue(newState.hasStartTime());
        assertFalse(branchState.hasStartTime());
        assertThat(newState.getStartTime()).isEqualTo(800);

    }
}