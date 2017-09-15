package com.tramchester.integration.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.integration.IntegrationClient;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;

public class AreaResourceTest {

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    ObjectMapper mapper = new ObjectMapper();

    @Before
    public void beforeEachTestRuns() {
        mapper.registerModule(new JodaModule());
    }

    @Test
    public void shouldGetAllAreas() {
        List<AreaDTO> results = getAll();

        assertTrue(results.size()>0);
        AreaDTO area = new AreaDTO("Altrincham");
        assertTrue(results.contains(area));
    }


    private List<AreaDTO> getAll() {
        Response result = IntegrationClient.getResponse(testRule, "areas", Optional.empty());
        return result.readEntity(new GenericType<ArrayList<AreaDTO>>(){});
    }
}
