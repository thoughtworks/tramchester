package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.DataLoader;
import com.tramchester.dataimport.data.PostcodeData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Paths;

class PostcodeDataParserTest  {

    private DataLoader<PostcodeData> dataDataLoader;

    @BeforeEach
    void setUp() {

        // NOTE: no headers in postcode data csvs

        dataDataLoader = new DataLoader<>(Paths.get("unused"), PostcodeData.class, PostcodeData.CVS_HEADER);

    }


    protected PostcodeData parse(String text) {
        StringReader reader = new StringReader(text+System.lineSeparator());
        return dataDataLoader.load(reader).findFirst().get();
    }

    @Test
    void shouldMapDataCorrectly() {

        String example = "\"AB101AB\",10,394235,806529,\"S92000003\",\"\",\"S08000020\",\"\",\"S12000033\",\"S13002842\"";

        PostcodeData result = parse(example);

        Assertions.assertEquals("AB101AB", result.getId());
        Assertions.assertEquals(394235, result.getEastings());
        Assertions.assertEquals(806529, result.getNorthings());
    }


    @Test
    void shouldMapDataCorrectlyRemoveSapces() {

        String example = "\"M1  1EU\",10,384759,398488,\"E92000001\",\"E19000001\",\"E18000002\",\"\",\"E08000003\",\"E05011376\"";

        PostcodeData result = parse(example);

        Assertions.assertEquals("M11EU", result.getId());
        Assertions.assertEquals(384759, result.getEastings());
        Assertions.assertEquals(398488, result.getNorthings());
    }

}
