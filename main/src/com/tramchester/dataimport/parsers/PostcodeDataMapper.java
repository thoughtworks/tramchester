package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.PostcodeData;
import org.apache.commons.csv.CSVRecord;

import java.util.List;

public class PostcodeDataMapper extends CSVEntryMapper<PostcodeData> {
    private static final int indexOfPostcode = 0;
    private static final int indexOfEastings = 2;
    private static final int indexOfNorthing = 3;

//    private enum Columns implements ColumnDefination {
//        Postcode, Eastings, Northings
//    }

    // Header from docs
    // Postcode,Positional_quality_indicator,Eastings,Northings,Country_code,NHS_regional_HA_code,
    //                                      NHS_HA_code,Admin_county_code,Admin_district_code,Admin_ward_code

    @Override
    public PostcodeData parseEntry(CSVRecord data) {
        String rawPostcode = data.get(indexOfPostcode);
        String rawEastings = data.get(indexOfEastings);
        String rawNorthing = data.get(indexOfNorthing);

        int eastings = Integer.parseInt(rawEastings);
        int northings = Integer.parseInt(rawNorthing);

        // inconsistent use of whitespace in source files, so remove
        return new PostcodeData(rawPostcode.replace(" ",""), eastings, northings);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        return true;
    }

    @Override
    protected void initColumnIndex(List<String> headers) {
        // No headers on postcode csv files
    }
}
