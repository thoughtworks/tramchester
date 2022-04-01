package com.tramchester.livedata.tfgm;

import com.google.inject.ImplementedBy;

@ImplementedBy(LiveDataHTTPFetcher.class)
public interface LiveDataFetcher {
    String fetch();
}
