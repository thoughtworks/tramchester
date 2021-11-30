package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.DataSourceID;

@JsonDeserialize(as=RemoteDataSourceAppConfig.class)
public interface RemoteDataSourceConfig extends HasDataPath {

    // url to check mod time against to see if newer data available
    String getDataCheckUrl();

    // url where data is located
    String getDataUrl();

    // downloaded filename
    String getDownloadFilename();

    // useful name for data set
    String getName();

    @JsonIgnore
    default DataSourceID getDataSourceId() {
        return DataSourceID.findOrUnknown(getName());
    }

    @JsonIgnore
    boolean getIsS3();
}
