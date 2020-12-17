package com.tramchester.unit.dataimport.parsers;


import com.tramchester.domain.FeedInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FeedInfoDataParserTest extends ParserTestHelper<FeedInfo> {
    private static final String feedInfo = "feed_publisher_name,feed_publisher_url,feed_timezone,feed_lang,20160530,20160615,feed_version";

    @BeforeEach
    void beforeEach() {
        super.before(FeedInfo.class, "feed_publisher_name,feed_publisher_url,feed_timezone,feed_lang,feed_valid_from,feed_valid_to,feed_version");
    }

    @Test
    void shouldParserFeedInfo() {

        FeedInfo info = parse(feedInfo);
        assertThat(info.getPublisherName()).isEqualTo("feed_publisher_name");
        assertThat(info.getPublisherUrl()).isEqualTo("feed_publisher_url");
        assertThat(info.getTimezone()).isEqualTo("feed_timezone");
        assertThat(info.getLang()).isEqualTo("feed_lang");
        assertThat(info.getVersion()).isEqualTo("feed_version");
        assertThat(info.validFrom()).isEqualTo(LocalDate.of(2016,5,30));
        assertThat(info.validUntil()).isEqualTo(LocalDate.of(2016,6,15));
    }
}
