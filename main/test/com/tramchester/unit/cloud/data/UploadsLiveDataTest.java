package com.tramchester.unit.cloud.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.cloud.data.S3Keys;
import com.tramchester.cloud.data.StationDepartureMapper;
import com.tramchester.cloud.data.UploadsLiveData;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.unit.repository.LiveDataUpdaterTest;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


class UploadsLiveDataTest extends EasyMockSupport {

    private ClientForS3 clientForS3;
    private UploadsLiveData uploadsLiveData;
    private List<StationDepartureInfo> liveData;
    private StationDepartureMapper mapper;
    private S3Keys s3Keys;
    private LocalDateTime lastUpdateTime;

    @BeforeEach
    void beforeEachTestRuns() {
        lastUpdateTime = LocalDateTime.parse("2018-11-15T15:06:32");

        clientForS3 = createStrictMock(ClientForS3.class);
        mapper = createStrictMock(StationDepartureMapper.class);

        s3Keys = createMock(S3Keys.class);

        uploadsLiveData = new UploadsLiveData(clientForS3, mapper, s3Keys);

        liveData = new LinkedList<>();
        liveData.add(LiveDataUpdaterTest.createDepartureInfoWithDueTram(lastUpdateTime, "displayId",
                "platforId", "messageTxt", TramStations.of(TramStations.NavigationRoad)));

    }

    @Test
    void shouldConvertToJsonStringAndThenUploadIfNotDuplicate() throws JsonProcessingException {

        List<StationDepartureInfoDTO> dtos = new ArrayList<>();
        dtos.add(new StationDepartureInfoDTO(liveData.get(0)));

        EasyMock.expect(s3Keys.createPrefix(lastUpdateTime.toLocalDate())).andReturn("prefix");
        EasyMock.expect(s3Keys.create(lastUpdateTime)).andReturn("key");

        EasyMock.expect(clientForS3.isStarted()).andReturn(true);
        EasyMock.expect(clientForS3.keyExists("prefix","key")).andReturn(false);
        EasyMock.expect(mapper.map(dtos)).andReturn("someJson");

        EasyMock.expect(clientForS3.upload("key", "someJson")).andReturn(true);

        replayAll();
        boolean result = uploadsLiveData.seenUpdate(liveData);
        verifyAll();

        Assertions.assertTrue(result);
    }

    @Test
    void shouldNotUploadIfKeyExists() {

        EasyMock.expect(s3Keys.createPrefix(lastUpdateTime.toLocalDate())).andReturn("prefix");
        EasyMock.expect(s3Keys.create(lastUpdateTime)).andReturn("key");

        EasyMock.expect(clientForS3.isStarted()).andReturn(true);
        EasyMock.expect(clientForS3.keyExists("prefix", "key")).andReturn(true);

        replayAll();
        boolean result = uploadsLiveData.seenUpdate(liveData);
        verifyAll();

        Assertions.assertTrue(result);
    }

    @Test
    void shouldNotUploadIfNotStarted() {
        EasyMock.expect(clientForS3.isStarted()).andReturn(false);

        replayAll();
        boolean result = uploadsLiveData.seenUpdate(liveData);
        verifyAll();

        Assertions.assertFalse(result);
    }
}
