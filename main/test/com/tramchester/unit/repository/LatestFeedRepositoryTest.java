package com.tramchester.unit.repository;

import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.domain.FeedInfo;
import com.tramchester.healthchecks.ProvidesNow;
import com.tramchester.repository.LatestFeedInfoRepository;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;

import static junit.framework.TestCase.assertEquals;

public class LatestFeedRepositoryTest extends EasyMockSupport {
    String rawInfo = "pub_name,url,timezone,feed_lang,start_date,end_date,version"
            + System.lineSeparator()
            + "some name,http://theUrl.com,UK,GB,20160530,20160615,1";

    private ByteArrayInputStream stream;
    FetchDataFromUrl fetchData;
    ProvidesNow providesNow;

    @Before
    public void beforeEachTestRuns() {
        stream = new ByteArrayInputStream(rawInfo.getBytes());
        fetchData = createMock(FetchDataFromUrl.class);
        providesNow = createMock(ProvidesNow.class);
    }

    @Test
    public void shouldFetchIfNotFetchedYet() throws IOException {

        EasyMock.expect(providesNow.getDate()).andReturn(LocalDate.of(2019,11,15));
        EasyMock.expect(fetchData.streamForSingleFile("gtdf-out/feed_info.txt")).andReturn(stream);

        LatestFeedInfoRepository latestFeedRepository = new LatestFeedInfoRepository(fetchData, providesNow);

        replayAll();
        FeedInfo feedinfo = latestFeedRepository.getFeedinfo();
        assertEquals("some name", feedinfo.getPublisherName());
        assertEquals(LocalDate.of(2016, 5,30),feedinfo.validFrom());
        assertEquals(LocalDate.of(2016, 6,15),feedinfo.validUntil());
        verifyAll();

    }

    @Test
    public void shouldNotFetchIfSameQueryDateDate() throws IOException {

        EasyMock.expect(providesNow.getDate()).andReturn(LocalDate.of(2019,11,15));
        EasyMock.expect(providesNow.getDate()).andReturn(LocalDate.of(2019,11,15));

        EasyMock.expect(fetchData.streamForSingleFile("gtdf-out/feed_info.txt")).andReturn(stream);

        LatestFeedInfoRepository latestFeedRepository = new LatestFeedInfoRepository(fetchData, providesNow);

        replayAll();
        latestFeedRepository.getFeedinfo();
        latestFeedRepository.getFeedinfo();
        verifyAll();

    }

    @Test
    public void shouldFetchIfLaterQueryDateDate() throws IOException {

        EasyMock.expect(providesNow.getDate()).andReturn(LocalDate.of(2019,11,15));
        EasyMock.expect(providesNow.getDate()).andReturn(LocalDate.of(2019,11,16));
        EasyMock.expect(providesNow.getDate()).andReturn(LocalDate.of(2019,11,17));
        EasyMock.expect(providesNow.getDate()).andReturn(LocalDate.of(2019,11,17));

        EasyMock.expect(fetchData.streamForSingleFile("gtdf-out/feed_info.txt")).andReturn(stream);
        EasyMock.expect(fetchData.streamForSingleFile("gtdf-out/feed_info.txt")).andReturn(stream);
        EasyMock.expect(fetchData.streamForSingleFile("gtdf-out/feed_info.txt")).andReturn(stream);

        LatestFeedInfoRepository latestFeedRepository = new LatestFeedInfoRepository(fetchData, providesNow);

        replayAll();
        latestFeedRepository.getFeedinfo();
        latestFeedRepository.getFeedinfo();
        latestFeedRepository.getFeedinfo();
        latestFeedRepository.getFeedinfo();
        verifyAll();

    }

}
