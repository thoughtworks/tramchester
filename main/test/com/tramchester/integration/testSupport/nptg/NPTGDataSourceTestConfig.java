package com.tramchester.integration.testSupport.nptg;

import com.tramchester.config.RemoteDataSourceConfig;

import java.nio.file.Path;

/****
 *   - name: nptg
 *     dataURL: https://naptan.app.dft.gov.uk/datarequest/nptg.ashx?format=csv
 *     dataCheckURL:
 *     dataPath: data/nptg
 *     filename: nptg_data.zip
 */

public class NPTGDataSourceTestConfig extends RemoteDataSourceConfig {
    @Override
    public Path getDataPath() {
        return Path.of("data", "nptg");
    }

    @Override
    public String getDataCheckUrl() {
        return "";
    }

    @Override
    public String getDataUrl() {
        return "https://naptan.app.dft.gov.uk/datarequest/nptg.ashx?format=csv";
        //return "http://www.dft.gov.uk/nptg/snapshot/nptgcsv.zip";
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
