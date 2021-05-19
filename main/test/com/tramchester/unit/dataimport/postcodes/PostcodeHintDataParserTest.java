package com.tramchester.unit.dataimport.postcodes;

import com.tramchester.dataimport.data.PostcodeHintData;
import com.tramchester.unit.dataimport.ParserTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostcodeHintDataParserTest extends ParserTestHelper<PostcodeHintData> {

    @BeforeEach
    void beforeEach() {
        super.before(PostcodeHintData.class, "code,minEasting,minNorthing,maxEasting,maxNorthing");
    }

    @Test
    void shouldParseData() {
        PostcodeHintData result = parse("pl,185207,41008,271210,95036");

        assertEquals("pl", result.getCode());
        assertEquals(185207, result.getMinEasting());
        assertEquals(41008, result.getMinNorthing());
        assertEquals(271210, result.getMaxEasting());
        assertEquals(95036, result.getMaxNorthing());

    }
}
