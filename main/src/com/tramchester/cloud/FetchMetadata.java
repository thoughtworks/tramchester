package com.tramchester.cloud;

import com.google.inject.ImplementedBy;

@ImplementedBy(FetchInstanceMetadata.class)
public interface FetchMetadata {
    String getUserData();
}
