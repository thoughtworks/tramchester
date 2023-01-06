package com.tramchester.integration.testSupport.nptg;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataimport.nptg.NPTGDataLoader;
import com.tramchester.domain.dates.DateRange;

import java.nio.file.Path;
import java.time.Duration;

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
        return "https://beta-naptan.dft.gov.uk/Download/File/Localities.csv";
        //return "https://naptan.app.dft.gov.uk/datarequest/nptg.ashx?format=csv";
        //return "http://www.dft.gov.uk/nptg/snapshot/nptgcsv.zip";
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(14);
    }

    @Override
    public String getDownloadFilename() {
        return NPTGDataLoader.LOCALITIES_CSV;
        //return "nptgcsv.zip";
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
