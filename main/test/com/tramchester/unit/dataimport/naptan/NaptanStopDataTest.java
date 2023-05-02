package com.tramchester.unit.dataimport.naptan;

import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanXMLStopAreaRef;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopType;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.unit.dataimport.ParserTestXMLHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static com.tramchester.repository.naptan.NaptanStopType.busCoachTrolleyStationBay;
import static com.tramchester.repository.naptan.NaptanStopType.tramMetroUndergroundPlatform;
import static org.junit.jupiter.api.Assertions.*;

class NaptanStopDataTest extends ParserTestXMLHelper<NaptanStopData> {

    /***
     * xml into split lines, useful for diagnosis etc
     * cat ./data/naptan/Stops.xml | sed 's/<\/StopPoint>/<\/StopPoint>\n/g' | sed 's/<\/StopArea>/<\/StopArea>\n/g' > output.txt
     */

    @BeforeEach
    void beforeEach() {
        super.before(StandardCharsets.UTF_8);
    }

    @Test
    void shouldParseDataForMetrolinkTram() throws XMLStreamException, IOException {
        String text = "<NaPTAN><StopPoints><StopPoint CreationDateTime=\"2013-12-17T14:30:00\" ModificationDateTime=\"202" +
                "1-04-16T09:27:13\" Modification=\"revise\" RevisionNumber=\"2\" Status=\"active\"><AtcoCode>9400ZZMAWWD2</AtcoCode><NaptanCode>" +
                "mantwgdp</NaptanCode><Descriptor><CommonName>Westwood (Manchester Metrolink)</CommonName><ShortCommonName>Westwood</Shor" +
                "tCommonName><Landmark>Richmond Academy</Landmark><Street>Middleton Road</Street><Crossing>Winterbottom Street</Crossing>" +
                "<Indicator>To Manchester</Indicator></Descriptor><Place><NptgLocalityRef>E0029527</NptgLocalityRef><LocalityCentre>1</Lo" +
                "calityCentre><Location><Translation><GridType>UKOS</GridType><Easting>391760</Easting><Northing>405082</Northing><Longit" +
                "ude>-2.12581352411</Longitude><Latitude>53.54231581884</Latitude></Translation></Location></Place><StopClassification><S" +
                "topType>PLT</StopType><OffStreet><Metro><Platform /></Metro></OffStreet></StopClassification><StopAreas><StopAreaRef Cre" +
                "ationDateTime=\"2013-12-17T14:31:00\" ModificationDateTime=\"2013-12-17T14:31:00\" Modification=\"new\" RevisionNumber=\"0\" Sta" +
                "tus=\"active\">940GZZMAWWD</StopAreaRef></StopAreas><AdministrativeAreaRef>147</AdministrativeAreaRef><PlusbusZones><Plusb" +
                "usZoneRef CreationDateTime=\"2013-12-17T14:30:00\" ModificationDateTime=\"2013-12-17T14:31:00\" Modification=\"revise\" Revisi" +
                "onNumber=\"1\" Status=\"active\">MNCRPIC</PlusbusZoneRef></PlusbusZones></StopPoint></StopPoints></NaPTAN>";

        NaptanStopData data = (NaptanStopData) super.parseFirstOnly(text);

        assertNotNull(data);

        assertIdEquals("9400ZZMAWWD2", data.getAtcoCode(), data.toString());
        assertEquals("mantwgdp", data.getNaptanCode());
        assertEquals("Westwood (Manchester Metrolink)", data.getCommonName());
        assertEquals("", data.getSuburb());
        assertEquals("", data.getTown());
        assertEquals(new GridPosition(391760,405082), data.getGridPosition());
        assertEquals(new LatLong(53.54231581884D, -2.12581352411D), data.getLatLong());
        // no longer needed, nptg now keyed by ATCOCode
        //assertEquals("E0029527", data.getNptgLocality());
        assertEquals(tramMetroUndergroundPlatform, data.getStopType());
    }

    @Test
    void shouldParseOkWhenInFullDocStructure() throws IOException, XMLStreamException {
        final String text = "<StopPoints><StopPoint CreationDateTime=\"2013-12-17T14:30:00\" ModificationDateTime=\"202" +
                "1-04-16T09:27:13\" Modification=\"revise\" RevisionNumber=\"2\" Status=\"active\"><AtcoCode>9400ZZMAWWD2</AtcoCode><NaptanCode>" +
                "mantwgdp</NaptanCode><Descriptor><CommonName>Westwood (Manchester Metrolink)</CommonName><ShortCommonName>Westwood</Shor" +
                "tCommonName><Landmark>Richmond Academy</Landmark><Street>Middleton Road</Street><Crossing>Winterbottom Street</Crossing>" +
                "<Indicator>To Manchester</Indicator></Descriptor><Place><NptgLocalityRef>E0029527</NptgLocalityRef><LocalityCentre>1</Lo" +
                "calityCentre><Location><Translation><GridType>UKOS</GridType><Easting>391760</Easting><Northing>405082</Northing><Longit" +
                "ude>-2.12581352411</Longitude><Latitude>53.54231581884</Latitude></Translation></Location></Place><StopClassification><S" +
                "topType>PLT</StopType><OffStreet><Metro><Platform /></Metro></OffStreet></StopClassification><StopAreas><StopAreaRef Cre" +
                "ationDateTime=\"2013-12-17T14:31:00\" ModificationDateTime=\"2013-12-17T14:31:00\" Modification=\"new\" RevisionNumber=\"0\" Sta" +
                "tus=\"active\">940GZZMAWWD</StopAreaRef></StopAreas><AdministrativeAreaRef>147</AdministrativeAreaRef><PlusbusZones><Plusb" +
                "usZoneRef CreationDateTime=\"2013-12-17T14:30:00\" ModificationDateTime=\"2013-12-17T14:31:00\" Modification=\"revise\" Revisi" +
                "onNumber=\"1\" Status=\"active\">MNCRPIC</PlusbusZoneRef></PlusbusZones></StopPoint></StopPoints>";

        String fullBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<NaPTAN " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns=\"http://www.naptan.org.uk/\" "
                        //"CreationDateTime=\"2022-01-21T17:59:59\" ModificationDateTime=\"2022-01-21T17:59:59\" " +
                        //"Modification=\"new\" RevisionNumber=\"0\" FileName=\"multiplestops.xml\" SchemaVersion=\"2.4\" " +
                        //"xsi:schemaLocation=\"http://www.naptan.org.uk/ http://www.naptan.org.uk/schema/2.4/NaPTAN.xsd\"
                        +">"
                        + text + "</NaPTAN>";

        NaptanStopData data = (NaptanStopData) super.parseFirstOnly(fullBody);

        assertNotNull(data);

        assertIdEquals("9400ZZMAWWD2", data.getAtcoCode(), data.toString());
        assertFalse(data.hasRailInfo());
    }

    @Test
    void shouldParseForBusStationMultiple() throws XMLStreamException, IOException {
        String text = "<NaPTAN><StopPoints>" +
                "<StopPoint CreationDateTime=\"1969-12-31T00:00:00\" ModificationDateTime=\"1969-12-31" +
                "T23:00:00\" Modification=\"new\" RevisionNumber=\"0\" Status=\"active\"><AtcoCode>acto1111</AtcoCode><NaptanCode>mergjtpm</N" +
                "aptanCode><Descriptor><CommonName xml:lang=\"en\">St Helens Bus Station</CommonName><ShortCommonName xml:lang=\"en\">St Hele" +
                "ns Bus St 2</ShortCommonName><Landmark xml:lang=\"en\">Bus Station</Landmark><Street xml:lang=\"en\">St Helens Bus Station</" +
                "Street><Indicator xml:lang=\"en\">Stand 2</Indicator></Descriptor><Place><NptgLocalityRef>E0057858</NptgLocalityRef><Subur" +
                "b xml:lang=\"en\">St Helens</Suburb><Town xml:lang=\"en\">St Helens</Town><LocalityCentre>true</LocalityCentre><Location><Gr" +
                "idType>UKOS</GridType><Easting>351424</Easting><Northing>395440</Northing></Location></Place><StopClassification><StopTy" +
                "pe>BCS</StopType><OffStreet><BusAndCoach><Bay /></BusAndCoach></OffStreet></StopClassification><StopAreas><StopAreaRef M" +
                "odification=\"new\" Status=\"active\" CreationDateTime=\"2021-12-06T13:55:45.1995824Z\" ModificationDateTime=\"2021-12-06T13:55" +
                ":45.1995824Z\">280G00000001</StopAreaRef></StopAreas><AdministrativeAreaRef>280</AdministrativeAreaRef></StopPoint>" +
                "<StopPoint CreationDateTime=\"1969-12-31T00:00:00\" ModificationDateTime=\"1969-12-31" +
                "T23:00:00\" Modification=\"new\" RevisionNumber=\"0\" Status=\"active\"><AtcoCode>acto2222</AtcoCode><NaptanCode>mergjtpm</N" +
                "aptanCode><Descriptor><CommonName xml:lang=\"en\">St Helens Bus Station</CommonName><ShortCommonName xml:lang=\"en\">St Hele" +
                "ns Bus St 2</ShortCommonName><Landmark xml:lang=\"en\">Bus Station</Landmark><Street xml:lang=\"en\">St Helens Bus Station</" +
                "Street><Indicator xml:lang=\"en\">Stand 2</Indicator></Descriptor><Place><NptgLocalityRef>E0057858</NptgLocalityRef><Subur" +
                "b xml:lang=\"en\">St Helens</Suburb><Town xml:lang=\"en\">St Helens</Town><LocalityCentre>true</LocalityCentre><Location><Gr" +
                "idType>UKOS</GridType><Easting>351424</Easting><Northing>395440</Northing></Location></Place><StopClassification><StopTy" +
                "pe>BCS</StopType><OffStreet><BusAndCoach><Bay /></BusAndCoach></OffStreet></StopClassification><StopAreas><StopAreaRef M" +
                "odification=\"new\" Status=\"active\" CreationDateTime=\"2021-12-06T13:55:45.1995824Z\" ModificationDateTime=\"2021-12-06T13:55" +
                ":45.1995824Z\">280G00000001</StopAreaRef></StopAreas><AdministrativeAreaRef>280</AdministrativeAreaRef></StopPoint>" +
                "</StopPoints></NaPTAN>";

        List<NaptanXMLData> items = super.parseAll(text);

        assertEquals(items.size(), 2);

        NaptanStopData dataA = (NaptanStopData) items.get(0);
        assertNotNull(dataA);
        assertIdEquals("acto1111", dataA.getAtcoCode());

        NaptanStopData dataB = (NaptanStopData) items.get(1);
        assertNotNull(dataB);
        assertIdEquals("acto2222", dataB.getAtcoCode());
    }

    @Test
    void shouldParseForBusStationWithIndicator() throws XMLStreamException, IOException {
        String text = "<NaPTAN><StopPoints><StopPoint CreationDateTime=\"1969-12-31T00:00:00\" ModificationDateTime=\"1969-12-31" +
                "T23:00:00\" Modification=\"new\" RevisionNumber=\"0\" Status=\"active\"><AtcoCode>2800S16001B</AtcoCode><NaptanCode>mergjtpm</N" +
                "aptanCode><Descriptor><CommonName xml:lang=\"en\">St Helens Bus Station</CommonName><ShortCommonName xml:lang=\"en\">St Hele" +
                "ns Bus St 2</ShortCommonName><Landmark xml:lang=\"en\">Bus Station</Landmark><Street xml:lang=\"en\">St Helens Bus Station</" +
                "Street><Indicator xml:lang=\"en\">Stand 2</Indicator></Descriptor><Place><NptgLocalityRef>E0057858</NptgLocalityRef><Subur" +
                "b xml:lang=\"en\">St Helens</Suburb><Town xml:lang=\"en\">St Helens</Town><LocalityCentre>true</LocalityCentre><Location><Gr" +
                "idType>UKOS</GridType><Easting>351424</Easting><Northing>395440</Northing></Location></Place><StopClassification><StopTy" +
                "pe>BCS</StopType><OffStreet><BusAndCoach><Bay /></BusAndCoach></OffStreet></StopClassification><StopAreas><StopAreaRef M" +
                "odification=\"new\" Status=\"active\" CreationDateTime=\"2021-12-06T13:55:45.1995824Z\" ModificationDateTime=\"2021-12-06T13:55" +
                ":45.1995824Z\">280G00000001</StopAreaRef></StopAreas><AdministrativeAreaRef>280</AdministrativeAreaRef>" +
                "</StopPoint></StopPoints></NaPTAN>";

        NaptanStopData data = (NaptanStopData) super.parseFirstOnly(text);

        assertNotNull(data);

        assertIdEquals("2800S16001B", data.getAtcoCode());
        assertEquals("mergjtpm", data.getNaptanCode());
        assertEquals("St Helens Bus Station", data.getCommonName());
        assertEquals("St Helens", data.getSuburb());
        assertEquals("St Helens", data.getTown());
        assertEquals("Stand 2", data.getIndicator());
        assertEquals(1, data.stopAreasRefs().size());
        assertEquals("280G00000001", data.stopAreasRefs().get(0).getId());
        assertEquals(busCoachTrolleyStationBay, data.getStopType());
        assertFalse(data.hasRailInfo());
    }

    @Test
    void shouldParseAdditionalRailInfoIfPresent() throws XMLStreamException, IOException {
        String text = "<NaPTAN><StopPoints><StopPoint CreationDateTime=\"2006-09-08T14:30:00\" ModificationDateTime=\"2007-09-26T13:00:00\" Modification=\"revise\" RevisionNumber=\"1\" Sta" +
                "tus=\"active\"><AtcoCode>9100ABDARE</AtcoCode><Descriptor><CommonName>Aberdare Rail Station</CommonName><Street>-</Street>" +
                "</Descriptor><Place><NptgLocalityRef>E0054662</NptgLocalityRef><LocalityCentre>1</LocalityCentre><Location><Translation>" +
                "<GridType>UKOS</GridType><Easting>300400</Easting><Northing>202800</Northing><Longitude>-3.44308344608</Longitude><Latit" +
                "ude>51.71505790608</Latitude></Translation></Location></Place><StopClassification><StopType>RLY</StopType><OffStreet>" +
                "<Rail><AccessArea />" +
                "<AnnotatedRailRef CreationDateTime=\"2003-11-04T00:00:00\" ModificationDateTime=\"2006-09-18T18:24:34\" " +
                "Modification=\"revise\" RevisionNumber=\"2\"><TiplocRef>ABDARE</TiplocRef><CrsRef>ABA</CrsRef>" +
                "<StationName>Aberdare Rail Station</StationName>" +
                "<Location><GridType>UKOS</GridType><Easting>300400</Easting><Northing>202800</Northing></Location></AnnotatedRailRef></Rail>" +
                "</OffStreet></StopClassification><StopAreas><StopAreaRef CreationDateTime=\"2006-12-08T00:00:00\" Modif" +
                "icationDateTime=\"2006-12-08T00:00:00\" Modification=\"new\" RevisionNumber=\"0\" Status=\"active\">910GABDARE</StopAreaRef></St" +
                "opAreas><AdministrativeAreaRef>110</AdministrativeAreaRef><PlusbusZones><PlusbusZoneRef CreationDateTime=\"2006-09-08T14:" +
                "30:00\" ModificationDateTime=\"2007-09-26T13:00:00\" Modification=\"revise\" RevisionNumber=\"1\" Status=\"active\">ABDARE</Plusb" +
                "usZoneRef></PlusbusZones></StopPoint></StopPoints></NaPTAN>";

        NaptanStopData data = (NaptanStopData) super.parseFirstOnly(text);

        assertNotNull(data);

        assertTrue(data.hasRailInfo());

        assertEquals("ABDARE", data.getRailInfo().getTiploc());
        assertEquals(1, data.stopAreasRefs().size());
        assertEquals("910GABDARE", data.stopAreasRefs().get(0).getId());
    }

    @Test
    void shouldParseMetrolinkTramStationInfo() throws XMLStreamException, IOException {
        String text = "<NaPTAN><StopPoints><StopPoint CreationDateTime=\"2006-10-24T00:00:00\" ModificationDateTime=\"2021-04-16T09:27:13\" " +
                "Modification=\"revise\" RevisionNumber=\"7\" Status=\"active\">" +
                "<AtcoCode>9400ZZMAALT</AtcoCode><NaptanCode>mantmtmg</NaptanCode><Descriptor>" +
                "<CommonName>Altrincham (Manchester Metrolink)</CommonName><ShortCommonName>Altrincham</ShortCommonName>" +
                "<Landmark>Altrincham Interchange</Landmark><Street>Off Stamford New Road</Street></Descriptor>" +
                "<Place><NptgLocalityRef>E0028261</NptgLocalityRef><LocalityCentre>1</LocalityCentre><Location><Translation>" +
                "<GridType>UKOS</GridType><Easting>376979</Easting><Northing>387883</Northing><Longitude>-2.34759192033</Longitude>" +
                "<Latitude>53.38728306533</Latitude></Translation></Location></Place>" +
                "<StopClassification><StopType>MET</StopType><OffStreet><Metro><AccessArea /></Metro></OffStreet></StopClassification>" +
                "<StopAreas><StopAreaRef CreationDateTime=\"2010-03-01T13:56:00\" ModificationDateTime=\"2010-03-01T13:56:00\" " +
                "Modification=\"new\" RevisionNumber=\"0\" Status=\"active\">940GZZMAALT</StopAreaRef></StopAreas>" +
                "<AdministrativeAreaRef>147</AdministrativeAreaRef>" +
                "<PlusbusZones><PlusbusZoneRef CreationDateTime=\"2006-10-24T00:00:00\" ModificationDateTime=\"2012-01-17T14:02:00\" " +
                "Modification=\"revise\" RevisionNumber=\"5\" Status=\"active\">MNCRPIC</PlusbusZoneRef>" +
                "</PlusbusZones></StopPoint></StopPoints></NaPTAN>";

        NaptanStopData data = (NaptanStopData) super.parseFirstOnly(text);

        assertIdEquals("9400ZZMAALT", data.getAtcoCode());
        assertEquals(StringIdFor.convert(TramStations.Altrincham.getId(), NaptanRecord.class), data.getAtcoCode());
        assertEquals(NaptanStopType.tramMetroUndergroundAccess, data.getStopType());
        assertEquals(1, data.stopAreasRefs().size());
    }

    @Test
    void shouldCorrectlyHandleStopWithTwoStopAreaRefs() throws XMLStreamException, IOException {
        String text = "<NaPTAN><StopPoints>" +
                "<StopPoint CreationDateTime=\"2017-07-07T14:13:22\" ModificationDateTime=\"2021-06-02T11:07:09\" " +
                "Modification=\"revise\" RevisionNumber=\"2\" Status=\"active\"><AtcoCode>1800BNIN0Y1</AtcoCode>" +
                "<NaptanCode>MANWAGJP</NaptanCode><Descriptor><CommonName>Bolton Interchange</CommonName>" +
                "<ShortCommonName>Interchange</ShortCommonName><Indicator>Stand Y</Indicator></Descriptor><Place>" +
                "<NptgLocalityRef>E0057777</NptgLocalityRef><LocalityCentre>1</LocalityCentre><Location>" +
                "<Translation><GridType>UKOS</GridType><Easting>371785</Easting><Northing>408929</Northing>" +
                "<Longitude>-2.427582</Longitude><Latitude>53.57619</Latitude></Translation></Location></Place>" +
                "<StopClassification><StopType>BCS</StopType><OffStreet><BusAndCoach><Bay><TimingStatus>PTP</TimingStatus>" +
                "</Bay></BusAndCoach></OffStreet></StopClassification>" +
                "<StopAreas>" +
                "<StopAreaRef CreationDateTime=\"2017-09-22T14:58:24\" ModificationDateTime=\"2020-08-10T16:46:29\" " +
                "Modification=\"delete\" RevisionNumber=\"3\">1800BNIN</StopAreaRef>" +
                "<StopAreaRef CreationDateTime=\"2020-07-23T15:19:47\" ModificationDateTime=\"2020-08-11T11:21:43\" " +
                "Modification=\"revise\" RevisionNumber=\"2\">180GBNIN</StopAreaRef>" +
                "</StopAreas>" +
                "<AdministrativeAreaRef>083</AdministrativeAreaRef><Public>false</Public>" +
                "</StopPoint></StopPoints></NaPTAN>";

        NaptanStopData data = (NaptanStopData) super.parseFirstOnly(text);

        assertEquals(2, data.stopAreasRefs().size());

        final NaptanXMLStopAreaRef firstRef = data.stopAreasRefs().get(0);
        final NaptanXMLStopAreaRef secondRef = data.stopAreasRefs().get(1);

        assertEquals("1800BNIN", firstRef.getId());
        assertEquals("delete", firstRef.getModification());
        assertFalse(firstRef.isActive());

        assertEquals( "180GBNIN", secondRef.getId());
        assertEquals("revise", secondRef.getModification());
        assertTrue(secondRef.isActive());
    }

}
