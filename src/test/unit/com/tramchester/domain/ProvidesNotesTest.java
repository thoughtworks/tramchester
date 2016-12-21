package com.tramchester.domain;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;


public class ProvidesNotesTest {
    private ProvidesNotes provider;

    @Before
    public void beforeEachTestRuns() {
        provider = new ProvidesNotes();
    }

    @Test
    public void shouldAddNotesForSaturdayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.parse("2016-10-29"));
        List<String> result = provider.createNotesFor(queryDate);

        assertThat(result, hasItem(ProvidesNotes.weekend));

    }

    @Test
    public void shouldAddNotesForSundayJourney() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.parse("2016-10-30"));
        List<String> result = provider.createNotesFor(queryDate);

        assertThat(result, hasItem(ProvidesNotes.weekend));
    }

    @Test
    public void shouldNotShowNotesOnOtherDay() {
        TramServiceDate queryDate = new TramServiceDate(LocalDate.parse("2016-10-31"));
        List<String> result = provider.createNotesFor(queryDate);

        assertThat(result, not(hasItem(ProvidesNotes.weekend)));
    }

    @Test
    public void shouldHaveNoteForChristmasServices2016() {
        LocalDate date = new LocalDate(2016, 12, 23);

        List<String> result = provider.createNotesFor(new TramServiceDate(date));
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));

        for(int offset=1; offset<11; offset++) {
            TramServiceDate queryDate = new TramServiceDate(date.plusDays(offset));
            result = provider.createNotesFor(queryDate);
            assertThat(queryDate.toString(), result, hasItem(ProvidesNotes.christmas));
        }

        date = new LocalDate(2017, 1, 3);
        result = provider.createNotesFor(new TramServiceDate(date));
        assertThat(result, not(hasItem(ProvidesNotes.christmas)));
    }
}
