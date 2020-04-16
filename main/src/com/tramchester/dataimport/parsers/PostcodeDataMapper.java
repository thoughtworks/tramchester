package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.PostcodeData;
import org.apache.commons.csv.CSVRecord;

public class PostcodeDataMapper implements CSVEntryMapper<PostcodeData> {
    //Header:
    // Postcode,Positional_quality_indicator,Eastings,Northings,Country_code,NHS_regional_HA_code,
    //                                      NHS_HA_code,Admin_county_code,Admin_district_code,Admin_ward_code

    @Override
    public PostcodeData parseEntry(CSVRecord data) {
        String rawPostcode = data.get(0);
        String rawEastings = data.get(2);
        String rawNorthing = data.get(3);

        int eastings = Integer.parseInt(rawEastings);
        int northings = Integer.parseInt(rawNorthing);

        return new PostcodeData(rawPostcode.replace(" ",""), eastings, northings);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        return true;
    }
}
