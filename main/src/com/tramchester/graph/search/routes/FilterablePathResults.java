package com.tramchester.graph.search.routes;

import com.tramchester.domain.places.InterchangeStation;

import java.util.function.Function;

public interface FilterablePathResults {
    QueryPathsWithDepth.QueryPath filter(Function<InterchangeStation, Boolean> filter);
}
