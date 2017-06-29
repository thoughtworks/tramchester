package com.tramchester.integration.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.integration.dataimport.data.AgencyData;

public class AgencyDataParser  implements CSVEntryParser<AgencyData> {

    @Override
    public AgencyData parseEntry(String... data) {
        String agencyId = data[0];
        String agencyName = data[1];
        String agencyUrl = data[2];

        return new AgencyData(agencyId, agencyName, agencyUrl);
    }
}
