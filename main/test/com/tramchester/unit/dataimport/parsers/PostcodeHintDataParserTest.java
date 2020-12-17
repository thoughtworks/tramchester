package com.tramchester.unit.dataimport.parsers;

import com.tramchester.dataimport.data.PostcodeHintData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostcodeHintDataParserTest extends ParserTestHelper<PostcodeHintData> {

    @BeforeEach
    void beforeEach() {
        super.before(PostcodeHintData.class, "file,minEasting,minNorthing,maxEasting,maxNorthing");
    }

    @Test
    void shouldParseData() {
        PostcodeHintData result = parse("data/codepo_gb/Data/CSV/pl.csv,185207,41008,271210,95036");

        assertEquals("data/codepo_gb/Data/CSV/pl.csv", result.getFile());
        assertEquals(185207, result.getMinEasting());
        assertEquals(41008, result.getMinNorthing());
        assertEquals(271210, result.getMaxEasting());
        assertEquals(95036, result.getMaxNorthing());

    }
}
