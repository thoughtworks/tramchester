package com.tramchester.unit.cloud;

import com.tramchester.cloud.ClientForS3;
import com.tramchester.cloud.UploadsLiveData;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.unit.repository.LiveDataRepositoryTest;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;


public class UploadsLiveDataTest extends EasyMockSupport {

    private ClientForS3 s3facade;
    private UploadsLiveData uploadsLiveData;
    private List<StationDepartureInfo> liveData;

    @Before
    public void beforeEachTestRuns() {
        LocalDateTime lastUpdateTime = LocalDateTime.parse("2018-11-15T15:06:32");

        s3facade = createStrictMock(ClientForS3.class);
        uploadsLiveData = new UploadsLiveData(s3facade);
        liveData = new LinkedList<>();
        LiveDataRepositoryTest.addStationInfo(liveData, lastUpdateTime, "displayId", "platforId", "messageTxt", "location");
    }

    @Test
    public void shouldConvertToJsonStringAndThenUploadIfNotDuplicate() {
        String expectedJSON = "[{\"lineName\":\"lineName\",\"stationPlatform\":\"platforId\",\"message\":\"messageTxt\"," +
                "\"dueTrams\":[{\"wait\":42,\"carriages\":\"Single\",\"status\":\"Due\",\"destination\":\"dest\",\"when\":\"15:48\"}]," +
                "\"lastUpdate\":\"2018-11-15T15:06:32\",\"displayId\":\"displayId\",\"location\":\"location\"}]";

        EasyMock.expect(s3facade.keyExists("20181115","20181115/15:06:32")).andReturn(false);
        EasyMock.expect(s3facade.upload("20181115/15:06:32", expectedJSON)).andReturn(true);

        replayAll();
        boolean result = uploadsLiveData.seenUpdate(liveData);
        verifyAll();

        assertTrue(result);
    }

    @Test
    public void shouldNotUploadIfKeyExists() {
        EasyMock.expect(s3facade.keyExists("20181115", "20181115/15:06:32")).andReturn(true);

        replayAll();
        uploadsLiveData.seenUpdate(liveData);
        verifyAll();

    }
}
