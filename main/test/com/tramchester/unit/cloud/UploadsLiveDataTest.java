package com.tramchester.unit.cloud;

import com.tramchester.cloud.ClientForS3;
import com.tramchester.cloud.UploadsLiveData;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.testSupport.Stations;
import com.tramchester.unit.repository.LiveDataRepositoryTest;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;


public class UploadsLiveDataTest extends EasyMockSupport {

    private ClientForS3 s3facade;
    private UploadsLiveData uploadsLiveData;
    private List<StationDepartureInfo> liveData;
    private String environment;

    @BeforeEach
    public void beforeEachTestRuns() {
        environment = System.getenv("PLACE");
        environment = environment==null ? "test" : environment.toLowerCase();
        LocalDateTime lastUpdateTime = LocalDateTime.parse("2018-11-15T15:06:32");

        s3facade = createStrictMock(ClientForS3.class);
        uploadsLiveData = new UploadsLiveData(s3facade);
        liveData = new LinkedList<>();
        LiveDataRepositoryTest.addStationInfoWithDueTram(liveData, lastUpdateTime, "displayId", "platforId", "messageTxt", Stations.NavigationRoad);
    }

    @Test
    public void shouldConvertToJsonStringAndThenUploadIfNotDuplicate() {
        String expectedJSON = "[{\"lineName\":\"lineName\",\"stationPlatform\":\"platforId\",\"message\":\"messageTxt\"," +
                "\"dueTrams\":[{\"from\":\"Navigation Road\",\"carriages\":\"Single\",\"status\":\"Due\",\"destination\":\"Bury\",\"when\":\"15:48\",\"wait\":42}]," +
                "\"lastUpdate\":\"2018-11-15T15:06:32\",\"displayId\":\"displayId\",\"location\":\"Navigation Road\"}]";

        EasyMock.expect(s3facade.isStarted()).andReturn(true);
        EasyMock.expect(s3facade.keyExists("20181115",environment+"/20181115/15:06:32")).andReturn(false);
        EasyMock.expect(s3facade.upload(environment+"/20181115/15:06:32", expectedJSON)).andReturn(true);

        replayAll();
        boolean result = uploadsLiveData.seenUpdate(liveData);
        verifyAll();

        Assertions.assertTrue(result);
    }

    @Test
    public void shouldNotUploadIfKeyExists() {
        EasyMock.expect(s3facade.isStarted()).andReturn(true);
        EasyMock.expect(s3facade.keyExists("20181115", environment+"/20181115/15:06:32")).andReturn(true);

        replayAll();
        boolean result = uploadsLiveData.seenUpdate(liveData);
        verifyAll();

        Assertions.assertTrue(result);
    }

    @Test
    public void shouldNotUploadIfNotStarted() {
        EasyMock.expect(s3facade.isStarted()).andReturn(false);

        replayAll();
        boolean result = uploadsLiveData.seenUpdate(liveData);
        verifyAll();

        Assertions.assertFalse(result);
    }
}
