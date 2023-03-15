package com.tramchester.unit.domain.id;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdTest {

    private final IdFor<Station> idA = Station.createId("1234");
    private final IdFor<Station> idAA = Station.createId("1234");
    private final IdFor<Station> idB = Station.createId("0BCD");
    private final IdFor<Station> idC = Station.createId("5678");

    @Test
    void shouldHaveEquality() {

        assertEquals(idA, idA);
        assertEquals(idA, idAA);
        assertEquals(idAA, idA);

        assertNotEquals(idA, idC);
        assertNotEquals(idC, idA);
    }

//    @Test
//    void shouldHaveCompositeIdEquality() {
//
//        CompositeId<Station> compositeIdA = new CompositeId<>(idA, idB);
//        CompositeId<Station> compositeIdB = new CompositeId<>(idB, idA);
//        CompositeId<Station> compositeIdC = new CompositeId<>(idA, idC);
//
//        assertEquals(compositeIdA, compositeIdA);
//        assertEquals(compositeIdA, compositeIdB);
//        assertEquals(compositeIdB, compositeIdA);
//
//        assertNotEquals(compositeIdA, compositeIdC);
//        assertNotEquals(compositeIdC, compositeIdA);
//
//        //assertEquals("[0BCD_1234]", compositeIdA.forDTO());
//        assertEquals(new IdForDTO("[0BCD_1234]"), new IdForDTO(compositeIdA));
//        assertEquals("[0BCD_1234]", compositeIdA.getGraphId());
//
//        assertEquals(compositeIdA.forDTO(), compositeIdB.forDTO());
//        assertEquals(compositeIdA.getGraphId(), compositeIdB.getGraphId());
//    }

    @Test
    void shouldNotBeEqualsIfDifferentDomains() {
        IdFor<Station> stationId = Station.createId("test123");
        IdFor<Route> routeId = Route.createId("test123");

        assertNotEquals(stationId, routeId);
    }


//    @Test
//    void shouldMapToNormalOrCompositeId() {
//        IdFor<Station> id = Station.createId("normalId");
//        assertFalse(CompositeId.isComposite("normalId"));
//        assertEquals("normalId", id.forDTO());
//
//        String text = "[Id1_Id2_Id3]";
//        assertTrue(CompositeId.isComposite(text));
//        IdFor<Station> comp = Station.createId(text);
//        CompositeId<Station> exepected = new CompositeId<>(Station.createId("Id1"),
//                Station.createId("Id2"), Station.createId("Id3"));
//
//        assertEquals(exepected, comp);
//    }

//    @Test
//    void shouldRoundTripParseComposite() {
//        CompositeId<Station> compositeIdA = new CompositeId<>(idA, idB, idC);
//
//        String forDto = compositeIdA.forDTO();
//        CompositeId<Station> result = CompositeId.parse(forDto, Station.class);
//
//        assertEquals(compositeIdA, result);
//    }

}
