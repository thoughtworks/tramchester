package com.tramchester.unit.domain;

import com.google.common.collect.Sets;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.*;

class UpdateRecentJourneysTest {

    //private final IdFor<Station> altyId = TramStations.Altrincham.getId();
    private final UpdateRecentJourneys updater = new UpdateRecentJourneys(TestEnv.GET());
    private final ProvidesNow providesNow = new ProvidesLocalNow();

    @Test
    void shouldUpdateRecentFrom() {
        RecentJourneys recentJourneys = new RecentJourneys();
        recentJourneys.setTimestamps(createTimestamps("id1","id2"));

        RecentJourneys updated = updater.createNewJourneys(recentJourneys, providesNow, Altrincham.fake());

        Set<Timestamped> from = updated.getRecentIds();
        Assertions.assertEquals(3, from.size());
        Assertions.assertTrue(from.containsAll(createTimestamps("id1","id2", Altrincham.getRawId())));
    }

    @Test
    void shouldNotAddIfPresent() {
        RecentJourneys recentJourneys = new RecentJourneys();
        recentJourneys.setTimestamps(createTimestamps("id1", Altrincham.getRawId(),"id3"));

        RecentJourneys updated = updater.createNewJourneys(recentJourneys, providesNow, Altrincham.fake());
        Set<Timestamped> from = updated.getRecentIds();
        Assertions.assertEquals(3, from.size());
        Assertions.assertTrue(from.containsAll(createTimestamps("id1", Altrincham.getRawId(),"id3")));
    }

    @Test
    void shouldRemoveOldestWhenLimitReached() throws InterruptedException {
        RecentJourneys recentJourneys = new RecentJourneys();
        recentJourneys.setTimestamps(Sets.newHashSet());
        RecentJourneys updated = updater.createNewJourneys(recentJourneys, providesNow, Altrincham.fake());
        Thread.sleep(2);
        updated = updateWithPause(updated, NavigationRoad.fake());
        updated = updateWithPause(updated, TraffordBar.fake());
        updated = updateWithPause(updated, StPetersSquare.fake());

        Set<Timestamped> from = updated.getRecentIds();
        Assertions.assertEquals(4, from.size());
        Set<Timestamped> initialExpected = createTimestamps(NavigationRoad.getRawId(), TraffordBar.getRawId(), NavigationRoad.getRawId());
        Assertions.assertTrue(from.containsAll(initialExpected));

        updated = updateWithPause(updated, Piccadilly.fake());
        from = updated.getRecentIds();
        Assertions.assertEquals(5, from.size());
        Assertions.assertTrue(from.containsAll(createTimestamps(StPetersSquare.getRawId(), TraffordBar.getRawId(), Piccadilly.getRawId())));
    }

    private RecentJourneys updateWithPause(RecentJourneys updated, Location<?> location) throws InterruptedException {
        updated = updater.createNewJourneys(updated, providesNow, location);
        Thread.sleep(2);
        return updated;
    }

    private Set<Timestamped> createTimestamps(String... ids) {
        Set<Timestamped> set = new HashSet<>();
        int count = 0;
        for (String id : ids) {
            IdForDTO idForDTO = new IdForDTO(id);
            set.add(new Timestamped(idForDTO, providesNow.getDateTime().plusSeconds(count++)));
        }
        return set;
    }
}
