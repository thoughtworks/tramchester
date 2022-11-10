package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import com.tramchester.testSupport.testTags.PiccGardens2022;
import com.tramchester.testSupport.testTags.VictoriaNov2022;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tramchester.domain.time.TramTime.of;
import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class StationAvailabilityRepositoryTest {
    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig config;

    private StationAvailabilityRepository availabilityRepository;
    private StationRepository stationRepository;
    private TramDate when;
    private ClosedStationsRepository closedStationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        availabilityRepository = componentContainer.get(StationAvailabilityRepository.class);
        closedStationRepository = componentContainer.get(ClosedStationsRepository.class);

        when = TestEnv.testTramDay();

    }

    @Test
    void shouldBeAvailableAtExpectedHours() {

        Station stPeters = StPetersSquare.from(stationRepository);

        boolean duringTheDay = availabilityRepository.isAvailable(stPeters, when, TimeRange.of(of(8,45), of(10,45)));

        assertTrue(duringTheDay);

        boolean lateAtNight = availabilityRepository.isAvailable(stPeters, when, TimeRange.of(of(3,5), of(3,15)));

        assertFalse(lateAtNight);
    }

    @DataExpiryCategory
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesOverMidnight() {

        // earier to diagnose using end of line station
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRange = TimeRange.of(TramTime.of(22, 50), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRange);
        assertFalse(results.isEmpty(), "for " + timeRange + " missing routes from " + altrincham);

        TimeRange timeRangeCrossMidnight = TimeRange.of(TramTime.of(23, 59), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> overMidnightResults = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRangeCrossMidnight);
        assertFalse(overMidnightResults.isEmpty(), "for " + timeRangeCrossMidnight + " missing routes over mid-night from " + altrincham);

        TimeRange timeRangeATMidnight = TimeRange.of(TramTime.of(0, 0), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> atMidnightResults = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRangeATMidnight);
        assertFalse(atMidnightResults.isEmpty(), "for " + timeRangeATMidnight + " missing routes over mid-night from " + altrincham);
    }

    @PiccGardens2022
    @DataExpiryCategory
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRanges() {

        // earier to diagnose using end of line station
        Station altrincham = Altrincham.from(stationRepository);

        TimeRange timeRange = TimeRange.of(TramTime.of(12, 50), Duration.ofHours(4), Duration.ofHours(4));

        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRange);

        // 2 -> 1, no piccadilly route
        assertEquals(1, results.size(),
                timeRange + " missing routes from " + altrincham.getId() + " got " + results);
    }

    @Test
    void shouldReproIssueWithAshtonAndTraffordCenterNov2022() {

        Station ashton = Ashton.from(stationRepository);
        Station traffordCentre = TraffordCentre.from(stationRepository);

        TramDate testDate = TramDate.of(2022, 11, 14);
        TimeRange timeRange = TimeRange.of(of(8, 5), of(10, 9));

        assertTrue(availabilityRepository.isAvailable(ashton, testDate, timeRange));
        assertTrue(availabilityRepository.isAvailable(traffordCentre, testDate, timeRange));
    }

    @DataExpiryCategory
    @Test
    void shouldHaveServicesAvailableAtExpectedLateTimeRangeNDaysAhead() {
        TramTime latestHour = TramTime.of(23,0);

        Duration maxwait = Duration.ofMinutes(config.getMaxWait());

        getUpcomingDates().filter(this::isValidDateToCheck).forEach(date -> {

            TimeRange lateRange = TimeRange.of(latestHour, maxwait, maxwait);
            Set<Station> notAvailableLate = stationRepository.getStations().stream().
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                    filter(station -> !availabilityRepository.isAvailable(station, date, lateRange)).
                    collect(Collectors.toSet());

            assertTrue(notAvailableLate.isEmpty(), "Not available " + date + " " + lateRange + " " + HasId.asIds(notAvailableLate));

        });
    }

    @VictoriaNov2022
    @DataExpiryCategory
    @Test
    void shouldHaveServicesAvailableAtExpectedEarlyTimeRangeNDaysAhead() {
        TramTime earlistHour = TramTime.of(7,0);

        Duration maxwait = Duration.ofMinutes(config.getMaxWait());

        getUpcomingDates().filter(this::isValidDateToCheck).forEach(date -> {

            TimeRange earlyRange = TimeRange.of(earlistHour, maxwait, maxwait);
            Set<Station> notAvailableEarly = stationRepository.getStations().stream().
                    filter(station -> workaroundDataIssueExchangeSquare(station, date)).
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                    filter(station -> !availabilityRepository.isAvailable(station, date, earlyRange)).
                    collect(Collectors.toSet());

            assertTrue(notAvailableEarly.isEmpty(), "Not available " + date + " " + earlyRange + " " + HasId.asIds(notAvailableEarly));
        });
    }

    @NotNull
    private Stream<TramDate> getUpcomingDates() {
        return IntStream.range(0, DAYS_AHEAD).boxed().
                map(when::plusDays).sorted();
    }

    private boolean isValidDateToCheck(TramDate date) {
        TramServiceDate tramServiceDate = new TramServiceDate(date);
        return !tramServiceDate.isChristmasPeriod();
    }

    private boolean workaroundDataIssueExchangeSquare(Station station, TramDate date) {
        if (station.getId().equals(ExchangeSquare.getId())) {
            return date.getDayOfWeek()!= DayOfWeek.SUNDAY;
        }
        return true;
    }

}
