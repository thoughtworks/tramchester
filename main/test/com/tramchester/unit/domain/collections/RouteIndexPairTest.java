package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouteIndexPairTest {

    public static final int NUMBER_OF_ROUTES = 1000;
    private RouteIndexPairFactory indexPairFactory;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        indexPairFactory = new RouteIndexPairFactory(() -> NUMBER_OF_ROUTES);
    }

    @NotNull
    private RouteIndexPair getPair(int first, int second) {
        return indexPairFactory.get(first, second);
    }

    @Test
    void shouldHaveAPair() {
        RouteIndexPair pair = getPair(42, 95);
        assertEquals(42, pair.firstAsInt());
        assertEquals(95, pair.secondAsInt());
    }

    @Test
    void shouldHaveSame() {
        RouteIndexPair pair = getPair(42, 42);
        assertTrue(pair.isSame());
    }

}
