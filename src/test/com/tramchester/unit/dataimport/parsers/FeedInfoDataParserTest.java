package com.tramchester.unit.dataimport.parsers;


import com.tramchester.dataimport.parsers.FeedInfoDataParser;
import com.tramchester.domain.FeedInfo;
import org.joda.time.LocalDate;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedInfoDataParserTest {
    String feedInfo = "feed_publisher_name,feed_publisher_url,feed_timezone,feed_lang,20160530,20160615,feed_version";

    @Test
    public void shouldParserFeedInfo() {
        FeedInfoDataParser feedInfoDataParser = new FeedInfoDataParser();

        FeedInfo info = feedInfoDataParser.parseEntry(this.feedInfo.split(","));
        assertThat(info.getPublisherName()).isEqualTo("feed_publisher_name");
        assertThat(info.getPublisherUrl()).isEqualTo("feed_publisher_url");
        assertThat(info.getTimezone()).isEqualTo("feed_timezone");
        assertThat(info.getLang()).isEqualTo("feed_lang");
        assertThat(info.getVersion()).isEqualTo("feed_version");
        assertThat(info.validFrom()).isEqualTo(new LocalDate(2016,5,30));
        assertThat(info.validUntil()).isEqualTo(new LocalDate(2016,6,15));
    }
}
