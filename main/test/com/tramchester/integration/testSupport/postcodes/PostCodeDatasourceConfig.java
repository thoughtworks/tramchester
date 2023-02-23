package com.tramchester.integration.testSupport.postcodes;

import com.tramchester.config.RemoteDataSourceConfig;

import java.nio.file.Path;
import java.time.Duration;

public class PostCodeDatasourceConfig extends RemoteDataSourceConfig {

    public static final String POSTCODE_URL = "https://api.os.uk/downloads/v1/products/CodePointOpen/downloads?area=GB&format=CSV&redirect";

    @Override
    public Path getDataPath() {
        return Path.of("data", "postcodes");
    }

    @Override
    public String getDataCheckUrl() {
        return POSTCODE_URL;
    }

    @Override
    public String getDataUrl() {
        return POSTCODE_URL;
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(31);
    }

    @Override
    public String getDownloadFilename() {
        return "codepo_gb.zip";
    }

    @Override
    public String getName() {
        return "postcode";
    }

    @Override
    public boolean getIsS3() {
        return false;
    }

}
