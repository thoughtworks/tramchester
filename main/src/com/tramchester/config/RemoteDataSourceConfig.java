package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.DataSourceID;
import io.dropwizard.Configuration;

import java.time.Duration;

@JsonDeserialize(as=RemoteDataSourceAppConfig.class)
public abstract class RemoteDataSourceConfig extends Configuration implements HasDataPath {

    // url to check mod time against to see if newer data available
    public abstract String getDataCheckUrl();

    // url where data is located
    public abstract String getDataUrl();

    // default expiry when cannot check mod time via http(s)
    public abstract Duration getDefaultExpiry();

    /***
     * Don't use this to get the actual downloaded filename, since this config can be blank to allow the remote
     * filename from the data source to be used, instead use:
     * @link com.tramchester.dataimport.RemoteDataRefreshed
     * @return the value from config, which can be blank
     */
    public abstract String getDownloadFilename();

    // TODO Should be RemoteDataSourceId
    // useful name for data set
    public abstract String getName();

    @JsonIgnore
    public DataSourceID getDataSourceId() {
        return DataSourceID.findOrUnknown(getName());
    }

    @JsonIgnore
    public abstract boolean getIsS3();

    @Override
    public String toString() {
        return "RemoteDataSourceConfig {"+
                "dataCheckURL: '"+getDataCheckUrl()+"' " +
                "dataURL: '"+getDataUrl()+"' " +
                "downloadFilename: '"+getDownloadFilename()+"' " +
                "name: '"+getName()+"' " +
                "dataSourceId: '"+getDataSourceId()+"' " +
                "isS3: '"+getIsS3()+"' " +
                "dataPath: '"+getDataPath()+"' " +
                "defaultExpiry: " + getDefaultExpiry() +
                "}";
    }

}
