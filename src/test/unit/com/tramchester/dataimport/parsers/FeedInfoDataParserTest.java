package com.tramchester.dataimport.parsers;


import com.tramchester.domain.FeedInfo;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedInfoDataParserTest {
    String feedInfo = "feed_valid_from,feed_valid_to,feed_version";

    @Test
    public void shouldParserFeedInfo() {
        FeedInfoDataParser feedInfoDataParser = new FeedInfoDataParser();

        FeedInfo info = feedInfoDataParser.parseEntry(this.feedInfo.split(","));
        assertThat(info.getVersion()).isEqualTo("feed_version");
        assertThat(info.validFrom()).isEqualTo("feed_valid_from");
        assertThat(info.validUntil()).isEqualTo("feed_valid_to");
    }
}
