package com.tramchester.dataimport.rail.records;

import com.tramchester.dataimport.rail.records.reference.RailInterchangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tramchester.dataimport.rail.records.RecordHelper.extract;
import static java.lang.String.format;

// https://stackoverflow.com/questions/54555623/what-format-are-atoc-master-stations-file-eastings-and-northings-in

public class PhysicalStationRecord {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalStationRecord.class);
    public static final String MISSING_POSITION = "00000";
    private static final int INVALID_MIN_CHANGE = Integer.MAX_VALUE;

    private final String name;
    private final String tiplocCode;
    private final int easting;
    private final int northing;
    private final RailInterchangeType railInterchangeType;
    private final int minChangeTime;
    private final String crs;

    public PhysicalStationRecord(String name, String tiplocCode, int easting, int northing,
                                 RailInterchangeType railInterchangeType, int minChangeTime, String crs) {
        this.name = name;
        this.tiplocCode = tiplocCode;
        this.easting = easting;
        this.northing = northing;
        this.railInterchangeType = railInterchangeType;
        this.minChangeTime = minChangeTime;
        this.crs = crs;
    }

    public static PhysicalStationRecord parse(String text) {
        String name = extract(text, 6, 31);
        String tiplocCode = extract(text, 37, 43+1); // docs?
        int easting = getEasting(text);
        int northing = getNorthing(text);
        char textRailInterchangeType = text.charAt(35);
        RailInterchangeType railInterchangeType = RailInterchangeType.getFor(textRailInterchangeType);
        int minChangeTime = getMinChangeTime(text);
        String crs = extract(text,50, 52+1);
        return new PhysicalStationRecord(name, tiplocCode, easting, northing, railInterchangeType, minChangeTime, crs);
    }

    private static int getMinChangeTime(String text) {
        String raw = extract(text, 64, 65+1).trim();
        if (raw.isBlank()) {
            return INVALID_MIN_CHANGE;
        }
        try {
            return Integer.parseInt(raw);
        }
        catch(NumberFormatException exception) {
            return INVALID_MIN_CHANGE;
        }
    }

    public boolean isMinChangeTimeValid() {
        return minChangeTime!=INVALID_MIN_CHANGE;
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

    public String getCRS() {
        return crs;
    }

    @Override
    public String toString() {
        return "PhysicalStationRecord{" +
                "name='" + name + '\'' +
                ", tiplocCode='" + tiplocCode + '\'' +
                ", easting=" + easting +
                ", northing=" + northing +
                ", railInterchangeType=" + railInterchangeType +
                ", minChangeTime=" + minChangeTime +
                ", crs='" + crs + '\'' +
                '}';
    }

    public RailInterchangeType getRailInterchangeType() {
        return railInterchangeType;
    }

    public int getMinChangeTime() {
        return minChangeTime;
    }


}

// from main PDF
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

// https://github.com/OddEssay/atoc-timetable-import/blob/master/master_station_names.php
//    			 * 3.2.7.2 Station Details
//   			 * Name 					Start 	Length 	Description
//			 * Record Type                  0       1       "A"
//			 * Spaces 					1 		4 		Spaces
//			 * Station Name 			5 		30 		Station Name
//			 * CATE Type 				35 		1 		Interchange Status. Values:
//			 * 												0 Not an interchange Point
//			 * 												1 Small Interchange Point
//			 * 												2 Medium Interchange Point
//			 * 												3 Large Interchange Point
//			 * 												9 This is a subsidiary TIPLOC at a station which has more than one TIPLOC. Stations which have more than one TIPLOC always have the same principal 3-Alpha Code.
//			 *													This field enables a Timetables enquiry system to give some precedence for changing at large interchange
//			 * 													points ahead of medium interchange points ahead of  small interchange points.
//			 * TIPLOC code 				36 		7 		Location code as held in the CIF data
//			 * Subsidiary 3-Alpha code  43 		3 		Where a station has more than one TIPLOC e.g.Tamworth, this is set to the 3-Alpha code that is not in
//			 * 											the field below. Normally this is a repeat of the 3-Alpha code
//			 * Spaces 					46 		3 		Spaces
//			 * 3-Alpha Code 			49		3 		Principal 3-Alpha Code of Station. Part of location code for the manual trains CIF data
//			 * Easting 					52 		5 		Easting in units of 100m. Stations too far south (Channel Islands) or too far north (Orkneys) or too far
//			 * 												west (west of Carrick on Shannon) have both their Easting and Northing set to 00000. The most westerly
//			 * 												station in range, Carrick on Shannon, has value 10000. The most easterly station, Amsterdam, has value 18690.
//			 * Estimated                57      1       "E" means estimated coordinates, space otherwise
//			 * Northing 				58 		5 		Northing in units of 100m. Stations too far south (Channel Islands) or too far north (Orkneys) or too far
//			 * 												west (west of Carrick on Shannon) have both their Easting and Northing set to 00000. The most
//			 * 												southerly station in range, Lizard (Bus), has value 60126. The most northerly station in range, Scrabster, has value 69703.
//			 * Change Time 				63 		2 		Change time in minutes
//			 * Footnote 				65 		2 		CATE footnote. This data is historic, is not maintained and should be ignored.
//			 * Spaces 					67 		11 Spaces
//			 */

