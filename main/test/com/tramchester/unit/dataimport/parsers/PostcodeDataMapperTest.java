package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.dataimport.parsers.PostcodeDataMapper;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class PostcodeDataMapperTest {

    private PostcodeDataMapper mapper;

    @BeforeEach
    void setUp() throws IOException {

        mapper = new PostcodeDataMapper();
        mapper.initColumnIndex(ParserBuilder.getRecordFor("Postcode,Positional_quality_indicator,Eastings,Northings,Country_code,NHS_regional_HA_code," +
                "NHS_HA_code,Admin_county_code,Admin_district_code,Admin_ward_code"));

    }

    @Test
    void shouldMapDataCorrectly() throws IOException {

        String example = "\"AB101AB\",10,394235,806529,\"S92000003\",\"\",\"S08000020\",\"\",\"S12000033\",\"S13002842\"";

        CSVRecord record = ParserBuilder.getRecordFor(example);
        PostcodeData result = mapper.parseEntry(record);

        Assertions.assertEquals("AB101AB", result.getId());
        Assertions.assertEquals(394235, result.getEastings());
        Assertions.assertEquals(806529, result.getNorthings());
    }

    @Test
    void shouldMapDataCorrectlyRemoveSapces() throws IOException {

        String example = "\"M1  1EU\",10,384759,398488,\"E92000001\",\"E19000001\",\"E18000002\",\"\",\"E08000003\",\"E05011376\"";

        CSVRecord record = ParserBuilder.getRecordFor(example);
        PostcodeData result = mapper.parseEntry(record);

        Assertions.assertEquals("M11EU", result.getId());
        Assertions.assertEquals(384759, result.getEastings());
        Assertions.assertEquals(398488, result.getNorthings());
    }

    @Test
    void shouldAllowAll() throws IOException {
        String example = "\"AB101AB\",10,394235,806529,\"S92000003\",\"\",\"S08000020\",\"\",\"S12000033\",\"S13002842\"";

        CSVRecord record = ParserBuilder.getRecordFor(example);
        Assertions.assertTrue(mapper.shouldInclude(record));
    }
}
