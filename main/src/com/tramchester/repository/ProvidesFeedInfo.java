package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.FeedInfo;

import java.util.Map;

@ImplementedBy(TransportData.class)
public interface ProvidesFeedInfo {
    Map<String,FeedInfo> getFeedInfos();
}
