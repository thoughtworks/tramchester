package com.tramchester.repository;

import com.tramchester.domain.FeedInfo;

import java.util.Map;

public interface ProvidesFeedInfo {
    Map<String,FeedInfo> getFeedInfos();
}
