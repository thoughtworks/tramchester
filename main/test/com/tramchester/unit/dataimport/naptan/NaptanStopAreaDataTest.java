package com.tramchester.unit.dataimport.naptan;

import com.tramchester.dataimport.NaPTAN.xml.stopArea.NaptanStopAreaData;
import com.tramchester.geo.GridPosition;
import com.tramchester.unit.dataimport.ParserTestXMLHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NaptanStopAreaDataTest extends ParserTestXMLHelper<NaptanStopAreaData> {

    @BeforeEach
    void beforeEach() {
        super.before(NaptanStopAreaData.class, StandardCharsets.UTF_8);
    }

    @Test
    void shouldParseStandardStopArea() throws XMLStreamException, IOException {
        String text = "<Naptan><StopPoints/><StopAreas><StopArea CreationDateTime=\"2006-09-11T15:42:00\" ModificationDateTime=\"2010-10-13T14:24:00\" " +
                "Modification=\"revise\" RevisionNumber=\"3\" Status=\"active\">" +
                "<StopAreaCode>910GALTRNHM</StopAreaCode>" +
                "<Name>Altrincham Rail Station</Name>" +
                "<AdministrativeAreaRef>110</AdministrativeAreaRef>" +
                "<StopAreaType>GRLS</StopAreaType><Location>" +
                "<Translation><GridType>UKOS</GridType><Easting>377026</Easting><Northing>387931</Northing>" +
                "<Longitude>-2.34688878555</Longitude><Latitude>53.38771656569</Latitude></Translation></Location>" +
                "</StopArea></StopAreas></Naptan>";

        NaptanStopAreaData result = super.parseFirstOnly(text);

        assertEquals("Altrincham Rail Station", result.getName());
        assertEquals("910GALTRNHM", result.getStopAreaCode());
        assertEquals("active", result.getStatus());
        assertTrue(result.isActive());
        assertEquals(new GridPosition(377026,387931), result.getGridPosition());
    }

    @Test
    void shouldParseWhenMultiple() throws XMLStreamException, IOException {
        String first = "<StopArea CreationDateTime=\"2006-10-24T00:00:00\" ModificationDateTime=\"2021-04-16T09:26:08\" Modification=\"revise\" " +
                "RevisionNumber=\"6\" Status=\"active\"><StopAreaCode>940GZZMAALT</StopAreaCode><Name>Altrincham (Manchester Metrolink)</Name>" +
                "<AdministrativeAreaRef>147</AdministrativeAreaRef><StopAreaType>GTMU</StopAreaType><Location><Translation><GridType>UKOS</GridType>" +
                "<Easting>376979</Easting><Northing>387883</Northing><Longitude>-2.34759192033</Longitude><Latitude>53.38728306533</Latitude>" +
                "</Translation></Location></StopArea>";

        String second = "<StopArea CreationDateTime=\"2006-09-11T15:42:00\" ModificationDateTime=\"2010-10-13T14:24:00\" " +
                "Modification=\"revise\" RevisionNumber=\"3\" Status=\"active\">" +
                "<StopAreaCode>910GALTRNHM</StopAreaCode>" +
                "<Name>Altrincham Rail Station</Name>" +
                "<AdministrativeAreaRef>110</AdministrativeAreaRef>" +
                "<StopAreaType>GRLS</StopAreaType><Location>" +
                "<Translation><GridType>UKOS</GridType><Easting>377026</Easting><Northing>387931</Northing>" +
                "<Longitude>-2.34688878555</Longitude><Latitude>53.38771656569</Latitude></Translation></Location>" +
                "</StopArea>";

        String text = "<Naptan><StopPoints/><StopAreas>" + first + second + "</StopAreas></Naptan>";

        List<NaptanStopAreaData> results = super.parseAll(text);

        assertEquals(2, results.size());
        assertEquals("940GZZMAALT", results.get(0).getStopAreaCode());
        assertEquals("910GALTRNHM", results.get(1).getStopAreaCode());

    }


}
