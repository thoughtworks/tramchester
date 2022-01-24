package com.tramchester.integration.testSupport.nptg;

import com.tramchester.config.RemoteDataSourceConfig;

import java.nio.file.Path;

/****
 *   - name: nptg
 *     dataURL: http://www.dft.gov.uk/nptg/snapshot/nptgcsv.zip
 *     dataCheckURL: http://www.dft.gov.uk/nptg/snapshot/nptgcsv.zip
 *     dataPath: data/nptg
 *     filename: nptg_data.zip
 */

public class NPTGDataSourceTestConfig implements RemoteDataSourceConfig {
    @Override
    public Path getDataPath() {
        return Path.of("data", "nptg");
    }

    @Override
    public String getDataCheckUrl() {
        return "http://www.dft.gov.uk/nptg/snapshot/nptgcsv.zip";
    }

    @Override
    public String getDataUrl() {
        return "http://www.dft.gov.uk/nptg/snapshot/nptgcsv.zip";
    }

    @Override
    public String getDownloadFilename() {
        return "nptgcsv.zip";
    }

    @Override
    public String getName() {
        return "nptg";
    }

    @Override
    public boolean getIsS3() {
        return false;
    }
}
