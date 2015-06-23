package com.tramchester.dataimport.parsers;


import com.tramchester.domain.FeedInfo;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedInfoDataParserTest {
    String feedInfo = "feed_publisher_name,feed_publisher_url,feed_timezone,feed_lang,feed_valid_from,feed_valid_to,feed_version";

    @Test
    public void shouldParserFeedInfo() {
        FeedInfoDataParser feedInfoDataParser = new FeedInfoDataParser();

        FeedInfo info = feedInfoDataParser.parseEntry(this.feedInfo.split(","));
        assertThat(info.getPublisherName()).isEqualTo("feed_publisher_name");
        assertThat(info.getPublisherUrl()).isEqualTo("feed_publisher_url");
        assertThat(info.getTimezone()).isEqualTo("feed_timezone");
        assertThat(info.getLang()).isEqualTo("feed_lang");
        assertThat(info.getVersion()).isEqualTo("feed_version");
        assertThat(info.validFrom()).isEqualTo("feed_valid_from");
        assertThat(info.validUntil()).isEqualTo("feed_valid_to");
    }
}
