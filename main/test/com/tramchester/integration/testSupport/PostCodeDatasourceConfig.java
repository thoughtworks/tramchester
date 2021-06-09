package com.tramchester.integration.testSupport;

import com.tramchester.config.RemoteDataSourceConfig;

import java.nio.file.Path;

public class PostCodeDatasourceConfig implements RemoteDataSourceConfig {
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
    public String getDownloadFilename() {
        return "codepo_gb.zip";
    }

    @Override
    public String getName() {
        return "postcodes";
    }

}
