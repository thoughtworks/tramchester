package com.tramchester.unit.resource;


import com.tramchester.resources.RouteCodeToClassMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RouteCodeToClassMapperTest {

    @Test
    public void testShouldMapToCorrectClassOldRouteStyle() {
        RouteCodeToClassMapper mapper = new RouteCodeToClassMapper();

        assertEquals("RouteClassD", mapper.map("MET:MET1:I"));
        assertEquals("RouteClassA", mapper.map("MET:MET2:I"));
        assertEquals("RouteClassG", mapper.map("MET:MET3:I"));
        assertEquals("RouteClassE", mapper.map("MET:MET4:I"));
        assertEquals("RouteClassC", mapper.map("MET:MET5:I"));
        assertEquals("RouteClassF", mapper.map("MET:MET6:I"));
        assertEquals("RouteClassH", mapper.map("MET:MET7:I"));

    }

    @Test
    public void testShouldMapToCorrectClassNewRouteStyle() {
        RouteCodeToClassMapper mapper = new RouteCodeToClassMapper();

        assertEquals("RouteClassA", mapper.map("MET:   A:I"));
        assertEquals("RouteClassB", mapper.map("MET:   B:I"));
        assertEquals("RouteClassC", mapper.map("MET:   C:I"));
        assertEquals("RouteClassD", mapper.map("MET:   D:I"));
        assertEquals("RouteClassE", mapper.map("MET:   E:I"));
        assertEquals("RouteClassF", mapper.map("MET:   F:I"));
        assertEquals("RouteClassG", mapper.map("MET:   G:I"));
        assertEquals("RouteClassH", mapper.map("MET:   H:I"));
        assertEquals("RouteClassI", mapper.map("MET:   I:I"));
        assertEquals("RouteClassJ", mapper.map("MET:   J:I"));
    }
}
