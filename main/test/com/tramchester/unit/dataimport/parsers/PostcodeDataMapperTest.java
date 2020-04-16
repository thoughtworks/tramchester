package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.dataimport.parsers.PostcodeDataMapper;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PostcodeDataMapperTest {

    private PostcodeDataMapper mapper;

    @Before
    public void setUp() {
        mapper = new PostcodeDataMapper();
    }

    @Test
    public void shouldMapDataCorrectly() throws IOException {

        String example = "\"AB101AB\",10,394235,806529,\"S92000003\",\"\",\"S08000020\",\"\",\"S12000033\",\"S13002842\"";

        CSVRecord record = ParserBuilder.getRecordFor(example);
        PostcodeData result = mapper.parseEntry(record);

        assertEquals("AB101AB", result.getId());
        assertEquals(394235, result.getEastings());
        assertEquals(806529, result.getNorthings());
    }

    @Test
    public void shouldMapDataCorrectlyRemoveSapces() throws IOException {

        String example = "\"M1  1EU\",10,384759,398488,\"E92000001\",\"E19000001\",\"E18000002\",\"\",\"E08000003\",\"E05011376\"";

        CSVRecord record = ParserBuilder.getRecordFor(example);
        PostcodeData result = mapper.parseEntry(record);

        assertEquals("M11EU", result.getId());
        assertEquals(384759, result.getEastings());
        assertEquals(398488, result.getNorthings());
    }

    @Test
    public void shouldAllowAll() throws IOException {
        String example = "\"AB101AB\",10,394235,806529,\"S92000003\",\"\",\"S08000020\",\"\",\"S12000033\",\"S13002842\"";

        CSVRecord record = ParserBuilder.getRecordFor(example);
        assertTrue(mapper.shouldInclude(record));
    }
}
