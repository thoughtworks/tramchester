package com.tramchester.unit.dataimport.nptg;

import com.tramchester.dataimport.nptg.NPTGData;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParseNPTGDataTest extends ParserTestCSVHelper<NPTGData> {

    @BeforeEach
    void beforeEachTestRuns() {
        String header = "\"NptgLocalityCode\",\"LocalityName\",\"LocalityNameLang\",\"ShortName\",\"ShortNameLang\"," +
                "\"QualifierName\",\"QualifierNameLang\",\"QualifierLocalityRef\",\"QualifierDistrictRef\"," +
                "\"AdministrativeAreaCode\",\"NptgDistrictCode\",\"SourceLocalityType\",\"GridType\",\"Easting\",\"Northing\"," +
                "\"CreationDateTime\",\"ModificationDateTime\",\"RevisionNumber\",\"Modification\",\"\"";
        super.before(NPTGData.class, header);
    }

    @Test
    void shouldParseNPTGData() {
        String text = "\"E0028261\",\"Altrincham\",\"EN\",\"\",\"EN\",\"\",\"EN\",\"\",\"\",\"083\",\"270\",\"Lo\",\"U\"," +
                "376920,387910,\"2005-10-05T10:44:51\",\"2020-02-06T17:01:05\",3,\"rev\",\"\"";

        NPTGData item = super.parse(text);

        assertEquals("E0028261", item.getNptgLocalityCode());
        assertEquals("Altrincham", item.getLocalityName());
        assertEquals("083", item.getAdministrativeAreaCode());

    }

}
