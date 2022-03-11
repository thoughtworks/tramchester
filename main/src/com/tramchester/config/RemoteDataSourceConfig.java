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

    /***
     * Don't use this to get the actual downloaded filename, since this config can be blank to allow the remote
     * filename from the data source to be used, instead use:
     * @link com.tramchester.dataimport.RemoteDataRefreshed
     * @return the value from config, which can be blank
     */
    String getDownloadFilename();

    // TODO Should be RemoteDataSourceId
    // useful name for data set
    String getName();

    @JsonIgnore
    default DataSourceID getDataSourceId() {
        return DataSourceID.findOrUnknown(getName());
    }

    @JsonIgnore
    boolean getIsS3();
}
