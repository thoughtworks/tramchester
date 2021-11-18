package com.tramchester.unit.dataimport.postcodes;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.dataimport.postcodes.PostcodeData;
import com.tramchester.geo.GridPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PostcodeDataParserTest  {

    private TransportDataFromFile<PostcodeData> dataDataLoader;

    @BeforeEach
    void setUp() {

        // NOTE: no headers in postcode data csvs
        CsvMapper mapper = CsvMapper.builder().build();
        dataDataLoader = new TransportDataFromFile<>(Paths.get("unused"), PostcodeData.class, PostcodeData.CVS_HEADER, mapper);

    }


    protected PostcodeData parse(String text) {
        StringReader reader = new StringReader(text+System.lineSeparator());
        return dataDataLoader.load(reader).findFirst().orElseThrow();
    }

    @Test
    void shouldMapDataCorrectly() {

        String example = "\"AB101AB\",10,394235,806529,\"S92000003\",\"\",\"S08000020\",\"\",\"S12000033\",\"S13002842\"";

        PostcodeData result = parse(example);

        assertEquals("AB101AB", result.getId());
        GridPosition gridPosition = result.getGridPosition();
        assertTrue(gridPosition.isValid());
        assertEquals(394235, gridPosition.getEastings());
        assertEquals(806529, gridPosition.getNorthings());
    }


    @Test
    void shouldMapDataCorrectlyRemoveSapces() {

        String example = "\"M1  1EU\",10,384759,398488,\"E92000001\",\"E19000001\",\"E18000002\",\"\",\"E08000003\",\"E05011376\"";

        PostcodeData result = parse(example);

        assertEquals("M11EU", result.getId());
        GridPosition gridPosition = result.getGridPosition();
        assertTrue(gridPosition.isValid());
        assertEquals(384759, gridPosition.getEastings());
        assertEquals(398488, gridPosition.getNorthings());
    }

    @Test
    void shouldMapToPostcodeWithInvalidGridPosition() {
        String example = "\"M1  1EU\",10,0,0,\"E92000001\",\"E19000001\",\"E18000002\",\"\",\"E08000003\",\"E05011376\"";

        PostcodeData result = parse(example);

        assertEquals("M11EU", result.getId());
        assertFalse(result.getGridPosition().isValid());
    }

}
