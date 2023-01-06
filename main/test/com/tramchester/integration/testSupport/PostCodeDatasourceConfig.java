package com.tramchester.integration.testSupport;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.dates.DateRange;

import java.nio.file.Path;
import java.time.Duration;

public class PostCodeDatasourceConfig extends RemoteDataSourceConfig {
    @Override
    public Path getDataPath() {
        return Path.of("data", "postcodes");
    }

    @Override
    public String getDataCheckUrl() {
        return "https://api.os.uk/downloads/v1/products/CodePointOpen/downloads?area=GB&format=CSV&redirect";
    }

    @Override
    public String getDataUrl() {
        return "https://api.os.uk/downloads/v1/products/CodePointOpen/downloads?area=GB&format=CSV&redirect";
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
