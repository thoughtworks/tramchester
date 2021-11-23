package com.tramchester.dataimport.rail.records;


// 1 Record Identity 2 1-2 With the constant value ‘TI’.
// 2 TIPLOC code 7 3-9
// 3 Capitals 2 10-11 Defines capitalisation of TIPLOC.
// 4 NALCO 6 12-17
// 5 NLC Check Character 1 18-18
// 6 TPS Description 26 19-44
// 7 STANOX 5 45-49 TOPS location code.
// 8 PO MCP Code 4 50-53 Post Office Location Code. (Not used but may contain historical data).
// 9 CRS Code 3 54-56
// 10 Description 16 57-72 Description used in CAPRI.
// 11 Spare 8 73-80

import com.tramchester.dataimport.rail.RailRecordType;

public class TIPLOCInsert implements RailTimetableRecord {

    private final String tiplocCode;
    private final String name;

    public TIPLOCInsert(String tiplocCode, String name) {
        this.tiplocCode = tiplocCode;
        this.name = name;
    }

    public static TIPLOCInsert parse(String line) {
        String tiplocCode = RecordHelper.extract(line, 3, 9+1);
        String name = RecordHelper.extract(line, 19, 44+1);
        return new TIPLOCInsert(tiplocCode, name);
    }

    public String getTiplocCode() {
        return tiplocCode;
    }

    @Override
    public RailRecordType getRecordType() {
        return RailRecordType.TiplocInsert;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TIPLOCInsert{" +
                "tiplocCode='" + tiplocCode + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
