package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.livedata.repository.ProvidesNotes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.domain.presentation.Note.NoteType.*;

@LazySingleton
public class ProvidesTramNotes implements ProvidesNotes {
    private static final Logger logger = LoggerFactory.getLogger(ProvidesTramNotes.class);

    private static final String EMPTY = "<no message>";
    public static final String website = "Please check <a href=\"http://www.metrolink.co.uk/pages/pni.aspx\">TFGM</a> for details.";
    public static final String weekend = "At the weekend your journey may be affected by improvement works." + website;
    public static final String christmas = "There are changes to Metrolink services during Christmas and New Year." + website;
    public static final String christmas2021 = "There are changes to services between 24th Dec and 3rd January. " +
            "Please check <a = href=\"https://tfgm.com/christmas-services\">TFGM</a> for details.";
    public static final String summer2022 = "From Saturday 16 July until Friday 21 October, engineering work will take " +
            "place on the Eccles line, a <a = href=\"https://tfgm.com/eccles-line\">replacement bus service</a> will be in operation. ";
    private static final int MESSAGE_LIFETIME = 5;

    private final PlatformMessageSource platformMessageSource;
    private final DTOFactory stationDTOFactory;

    @Inject
    public ProvidesTramNotes(PlatformMessageSource platformMessageSource, DTOFactory stationDTOFactory) {
        this.platformMessageSource = platformMessageSource;
        this.stationDTOFactory = stationDTOFactory;
    }

    @PostConstruct
    void start() {
        logger.info("starting");
        if (!platformMessageSource.isEnabled()) {
            logger.warn("Disabled for live data since PlatformMessageSource is disabled");
        }
        logger.info("started");
    }

    /***
     * From JourneyDTO prep
     */
    @Override
    public List<Note> createNotesForJourney(Journey journey, TramDate queryDate) {
        if (!journey.getTransportModes().contains(TransportMode.Tram)) {
            logger.info("Not a tram journey, providing no notes");
            return Collections.emptyList();
        }

        List<Note> notes = new LinkedList<>(createNotesForADate(queryDate));

        if (platformMessageSource.isEnabled()) {
            notes.addAll(liveNotesForJourney(journey, queryDate));
        }

        return notes;
    }

    /***
     * From DeparturesResource
     */
    @Override
    public List<Note> createNotesForStations(List<Station> stations, TramDate queryDate, TramTime time) {
        if (!platformMessageSource.isEnabled()) {
            logger.error("Attempted to get notes for departures when live data disabled");
            return Collections.emptyList();
        }

        List<Note> notes = new LinkedList<>();
        notes.addAll(createNotesForADate(queryDate));
        notes.addAll(createLiveNotesForStations(stations, queryDate, time));
        return notes;
    }

    private List<Note> createLiveNotesForStations(List<Station> stations, TramDate date, TramTime time) {
        List<Note> notes = new ArrayList<>();

        stations.forEach(station -> platformMessageSource.messagesFor(station, date, time).forEach(info ->
                addRelevantNote(notes, info)));

        return notes;
    }

    private List<Note> createNotesForADate(TramDate queryDate) {
        ArrayList<Note> notes = new ArrayList<>();
        if (queryDate.isWeekend()) {
            notes.add(new Note(weekend, Weekend));
        }
        if (queryDate.isChristmasPeriod()) {
            int year = queryDate.getYear();
            if (year==2021 || year==2022) {
                notes.add(new Note(christmas2021, Christmas));
            } else {
                notes.add(new Note(christmas, Christmas));
            }
        }
        return notes;
    }

    private List<Note> liveNotesForJourney(Journey journey, TramDate queryDate) {
        // Map: Note -> Location
        List<Note> notes = new ArrayList<>();

        // find all the platforms involved in a journey, so board, depart and changes
        // add messages for those platforms
        journey.getCallingPlatformIds().forEach(platform -> addLiveNotesForPlatform(notes, platform, queryDate,
                journey.getQueryTime()));

        return notes;
    }

    private void addLiveNotesForPlatform(List<Note> notes, IdFor<Platform> platformId, TramDate queryDate, TramTime queryTime) {
        Optional<PlatformMessage> maybe = platformMessageSource.messagesFor(platformId, queryDate, queryTime);
        if (maybe.isEmpty()) {
            logger.warn("No messages found for " + platformId + " at " + queryDate +  " " + queryTime);
            return;
        }
        PlatformMessage info = maybe.get();
        LocalDateTime lastUpdate = info.getLastUpdate();
        TramDate lastUpdateDate = TramDate.of(lastUpdate.toLocalDate());
        if (!lastUpdateDate.isEqual(queryDate)) {
            // message is not for journey time, perhaps journey is a future date or live data is stale
            logger.info("No messages available for " + queryDate + " last up date was " + lastUpdate);
            return;
        }
        TramTime updateTime = TramTime.ofHourMins(lastUpdate.toLocalTime());

        // 1 minutes here as time sync on live api has been out by 1 min
        TimeRange range = TimeRange.of(updateTime, Duration.ofMinutes(1), Duration.ofMinutes(MESSAGE_LIFETIME));
        //if (!queryTime.between(updateTime.minusMinutes(1), updateTime.plusMinutes(MESSAGE_LIFETIME))) {
        if (!range.contains(queryTime)) {
            logger.info("No data available for " + queryTime + " as not within " + range);
            return;
        }
        addRelevantNote(notes, info);
    }

    private void addRelevantNote(List<Note> notes, PlatformMessage info) {

        String message = info.getMessage();
        if (usefulNote(message)) {
            logger.info("Added message from " + info);
            notes.add(stationDTOFactory.createStationNote(Live, message, info.getStation()));
        } else {
            logger.info("Filtered message from " + info);
        }
    }

    private boolean usefulNote(String text) {
        return ! (text.isEmpty() || EMPTY.equals(text));
    }
}
