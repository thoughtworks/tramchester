package com.tramchester.dataimport.rail.records;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tramchester.dataimport.rail.records.RecordHelper.extract;
import static java.lang.String.format;

// 1 Record Type 1 1-1 Constant value ‘A’.
// 2 Reserved 4 2-5
// 3 Station Name 26 6-31
// 4 Reserved 4 32-35
// 5 CATE Interchange status 1 36-36 Always populated with ‘0’, ‘1’, ‘2’, ‘3’ or ‘9’.
// 6 TIPLOC Code 7 37-43
// 7 Minor CRS Code 3 44-46
// 8 Reserved 3 47-49
// 9 CRS Code 3 50-52
// 10 Ordnance Survey Grid Ref East 5 53-57 Values are in 0.1 km units. Format is
//    ‘1nnnn’ where nnnn is the distance in 0.1 km units.
// 11 Blank/Estimate 1 58-58 Value is blank or ‘E’ if Grid Reference is an estimate.
// 12 Ordnance Survey Grid Ref North 5 59-63 Values are in 0.1 km units. Format is
//    ‘6nnnn’ where nnnn is the distance in 0.1 km units.
// 13 Minimum Change Time 2 64-65 A one or two-digit number, in minutes, in the range 0-99. This is regardless of
//    whether or not Field 5: ‘CATE Interchange status‘ shows the station as an interchange.
// 14 Reserved 1 66-66
// 15 Footnote/Closed/Staff/Not-advertised code
// 1 67-67 Redundant and not supported in PMS. Will always be blank.
// 16 Reserved 11 68-78
// 17 Sub-sector code 3 79-81 Redundant and not supported in PMS.
//    Will always be blank.

public class PhysicalStationRecord {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalStationRecord.class);
    public static final String MISSING_POSITION = "00000";

    private final String name;
    private final String tiplocCode;
    private final int easting;
    private final int northing;

    public PhysicalStationRecord(String name, String tiplocCode, int easting, int northing) {
        this.name = name;
        this.tiplocCode = tiplocCode;
        this.easting = easting;
        this.northing = northing;
    }

    public static PhysicalStationRecord parse(String line) {
        String name = extract(line, 6, 31);
        String tiplocCode = extract(line, 37, 43+1); // docs?
        int easting = getEasting(line);
        int northing = getNorthing(line);
        return new PhysicalStationRecord(name, tiplocCode, easting, northing);
    }

    private static int getEasting(String line) {
        String field = extract(line, 53, 57+1); // docs wrong?

        return parseGrid(line, field, "easting", '1');
    }

    private static int getNorthing(String line) {
        String field = extract(line, 59, 63+1); // docs wrong?
        return parseGrid(line, field, "northing", '6');
    }

    private static int parseGrid(String line, String field, String fieldName, char expectedPrefix) {
        if (field.equals(MISSING_POSITION)) {
            return Integer.MIN_VALUE;
        }
        if (field.charAt(0) != expectedPrefix) {
            logger.warn(format("Expected %s field to being with '%s', got %s and line %s", fieldName, expectedPrefix, field, line));
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(field.substring(1));
        }
        catch (NumberFormatException numberFormatException) {
            logger.warn(format("Cannot extract %s from '%s' as not a valid integer, line was %s", fieldName, field, line));
            return Integer.MAX_VALUE;
        }
    }

    public String getName() {
        return name;
    }

    public String getTiplocCode() {
        return tiplocCode;
    }

    public int getEasting() {
        return easting;
    }

    public int getNorthing() {
        return northing;
    }

    @Override
    public String toString() {
        return "PhysicalStationRecord{" +
                "name='" + name + '\'' +
                ", tiplocCode='" + tiplocCode + '\'' +
                ", easting=" + easting +
                ", northing=" + northing +
                '}';
    }
}
