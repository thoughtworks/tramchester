package com.tramchester.dataimport;

import com.google.inject.ImplementedBy;

@ImplementedBy(FetchDataFromUrl.class)
public interface RemoteDataRefreshed {
    boolean refreshed(String name);
}
