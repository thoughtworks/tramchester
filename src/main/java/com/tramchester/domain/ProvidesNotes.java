package com.tramchester.domain;

import com.tramchester.repository.ProvidesFeedInfo;

import java.util.LinkedList;
import java.util.List;

public class ProvidesNotes {
    private ProvidesFeedInfo providesFeedInfo;

    public static final String website = "Please check <a href=\"http://www.metrolink.co.uk/pages/pni.aspx\">TFGM</a> for details.";

    public static String weekend = "At the weekend your journey may be affected by improvement works." + website;

    public static String christmas = "There are changes to Metrolink services during Christmas and New Year." + website;

    public static String noData = "The current data available from TFGM does not contain data for this date.";

    public ProvidesNotes(ProvidesFeedInfo providesFeedInfo) {
        this.providesFeedInfo = providesFeedInfo;
    }

    public List<String> createNotesFor(TramServiceDate queryDate) {
        List<String> notes = new LinkedList<>();

        if (queryDate.isWeekend()) {
            notes.add(weekend);
        }

        if (queryDate.isChristmasPeriod()) {
            notes.add(christmas);
        }

        FeedInfo feedInfo = providesFeedInfo.getFeedInfo();
        if (!queryDate.within(feedInfo.validFrom(), feedInfo.validUntil())) {
            notes.add(noData);
        }

        return notes;
    }
}
