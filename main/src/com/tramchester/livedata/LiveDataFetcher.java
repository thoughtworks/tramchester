package com.tramchester.livedata;

import com.google.inject.ImplementedBy;

@ImplementedBy(LiveDataHTTPFetcher.class)
public interface LiveDataFetcher {
    String fetch();
}
