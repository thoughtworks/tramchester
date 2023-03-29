package com.tramchester.livedata.tfgm;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Platform;
import com.tramchester.domain.factory.TransportEntityFactoryForTFGM;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.StationByName;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.*;
import java.util.*;

import static com.tramchester.livedata.domain.liveUpdates.LineDirection.Both;
import static com.tramchester.livedata.domain.liveUpdates.LineDirection.Unknown;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;



// TODO Split parse and create concerns here

@LazySingleton
public class LiveDataParser {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataParser.class);

    private static final String DIRECTION_BOTH = "Incoming/Outgoing";
    private static final String TERMINATES_HERE = "Terminates Here";
    private static final String NOT_IN_SERVICE = "Not in Service";
    private static final List<String> NotADestination = Arrays.asList("See Tram Front", NOT_IN_SERVICE);

    private final TimeZone timeZone = TimeZone.getTimeZone(TramchesterConfig.TimeZoneId);

    private final StationByName stationByName;
    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final AgencyRepository agencyRepository;
    private final Map<String, String> destinationNameMappings;

    // live data api has limit in number of results
    private static final int MAX_DUE_TRAMS = 4;
    private Agency agency;

    public enum LiveDataNamesMapping {
        Firswood("Firswood", "Firswood Station"),
        Ashton("Ashton","Ashton-Under-Lyne"),
        DeansgateAliasA("Deansgate - Castlefield","Deansgate-Castlefield"),
        DeansgateAliasB("Deansgate Castlefield","Deansgate-Castlefield"),
        BessesOThBarns("Besses O’ Th’ Barn","Besses o'th'barn"),
        NewtonHeathAndMoston("Newton Heath and Moston","Newton Heath & Moston"),
        StWerburgsRoad("St Werburgh’s Road","St Werburgh's Road"),
        Rochdale("Rochdale Stn", "Rochdale Railway Station"),
        TraffordCentre("Trafford Centre", "The Trafford Centre"),
        RochdaleCentre("Rochdale Ctr", "Rochdale Town Centre");

        private final String from;
        private final String too;

        LiveDataNamesMapping(String from, String too) {
            this.from = from;
            this.too = too;
        }

        public String getToo() {
            return too;
        }
    }

    @Inject
    public LiveDataParser(StationByName stationByName, StationRepository stationRepository,
                          PlatformRepository platformRepository, AgencyRepository agencyRepository) {
        this.stationByName = stationByName;
        this.platformRepository = platformRepository;
        this.stationRepository = stationRepository;
        this.agencyRepository = agencyRepository;

        destinationNameMappings = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        List<LiveDataNamesMapping> referenceData = Arrays.asList(LiveDataNamesMapping.values());
        referenceData.forEach(item -> destinationNameMappings.put(item.from, item.too));
        agency = agencyRepository.get(MutableAgency.METL);
        logger.info("started");
    }

    public List<TramStationDepartureInfo> parse(String rawJson) {
        List<TramStationDepartureInfo> result = new LinkedList<>();

        JsonObject parsed = Jsoner.deserialize(rawJson, new JsonObject());
        if (parsed.containsKey("value")) {
            JsonArray infoList = (JsonArray ) parsed.get("value");

            if (infoList!=null) {
                for (Object anInfoList : infoList) {
                    Optional<TramStationDepartureInfo> item = parseItem((JsonObject) anInfoList);
                    item.ifPresent(result::add);
                }
            }
        } else {
            logger.error("Unable to deserialise received json: "+rawJson);
        }

        return result;
    }

    private Optional<TramStationDepartureInfo> parseItem(JsonObject jsonObject) {
        logger.debug(format("Parsing JSON '%s'", jsonObject));

        final BigDecimal displayId = (BigDecimal) jsonObject.get("Id");
        final String rawLine = (String) jsonObject.get("Line");
        final String atcoCode = (String) jsonObject.get("AtcoCode");
        final String message = (String) jsonObject.get("MessageBoard");
        final String dateString = (String) jsonObject.get("LastUpdated");
        final String rawDirection = (String)jsonObject.get("Direction");
        LocalDateTime updateTime = getStationUpdateTime(dateString);

        LineDirection direction = getDirection(rawDirection);
        if (direction == Unknown) {
            logger.warn("Display '" + displayId +"' Unable to map direction code name "+ rawDirection +
                    " for JSON " + jsonObject);
        }

        Lines line = getLine(rawLine);
        if (line == Lines.UnknownLine) {
            logger.warn("Display '" + displayId +"' Unable to map line name "+ rawLine +
                    " for JSON " + jsonObject);
        }

        Optional<Station> maybeStation = getStationByAtcoCode(atcoCode);
        if (maybeStation.isEmpty()) {
            logger.warn("Display '" + displayId + "' Unable to map atco code to station '"+ atcoCode + "' for JSON " + jsonObject);
            return Optional.empty();
        }
        Station station = maybeStation.get();

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo(displayId.toString(), line, direction,
                station, message, updateTime);

        IdFor<Platform> platformId = PlatformId.createId(station.getId(), atcoCode);
        if (platformRepository.hasPlatformId(platformId)) {
            Platform platform = platformRepository.getPlatformById(platformId);
            departureInfo.setStationPlatform(platform);
            if (!station.hasPlatform(platformId)) {
                // NOTE: some single platform stations (i.e. navigation road) appear to have
                // two platforms in the live data feed...but not in the station reference data
                logger.info(format("Display '%s' Platform '%s' not in timetable data for station %s and Json %s",
                        displayId, atcoCode, station.getId(), jsonObject));

            }
        } else {
            logger.warn("Did not find platform for '" + atcoCode + "' and Json " + jsonObject);
        }

        parseDueTrams(jsonObject, departureInfo);

        logger.debug("Parsed live data to " + departureInfo);
        return Optional.of(departureInfo);
    }

    private Lines getLine(String text) {
        Lines[] valid = Lines.values();
        for (Lines line : valid) {
            if (line.getName().equals(text)) {
                return line;
            }
        }
        return Lines.UnknownLine;
    }

    private LineDirection getDirection(String text) {
        if (DIRECTION_BOTH.equals(text)) {
            return Both;
        }
        try {
            return LineDirection.valueOf(text);
        }
        catch (IllegalArgumentException unexpectedValueInTheApi) {
            logger.warn("Unable to parse direction " + text);
        }
        return Unknown;
    }

    private LocalDateTime getStationUpdateTime(String dateString) {
        Instant instanceOfUpdate = Instant.from(ISO_INSTANT.parse(dateString));

        ZonedDateTime zonedDateTime = instanceOfUpdate.atZone(TramchesterConfig.TimeZoneId);
        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();

        // WORKAROUND - feed always contains 'Z' at end of date/time even though feed actually switches to BST
        boolean dst = timeZone.inDaylightTime(Date.from(instanceOfUpdate));
        if (dst) {
            int seconds_offset = timeZone.getDSTSavings() / 1000;
            localDateTime = localDateTime.minusSeconds(seconds_offset);
        }

        return localDateTime;
    }

    private void parseDueTrams(JsonObject jsonObject, TramStationDepartureInfo departureInfo) {
        for (int i = 0; i < MAX_DUE_TRAMS; i++) {
            final int index = i;
            String destinationName = getNumberedField(jsonObject, "Dest", index);
            if (destinationName.isEmpty()) {
                // likely not present in json
                logger.debug("Skipping destination '" + destinationName + "' for " + jsonObject + " and index " + i);
            } else if (NotADestination.contains(destinationName)) {
                logger.info("Display '" + departureInfo.getDisplayId() + "' Skipping destination '" + destinationName + "' for " + jsonObject.toJson() + " and index " + i);
            } else {
                Optional<Station> maybeDestStation;
                if (TERMINATES_HERE.equals(destinationName)) {
                    // replace "terminates here" with the station where this message is displayed
                    maybeDestStation = Optional.of(departureInfo.getStation());
                } else {
                    // try to look up destination station based on the destination text....
                    maybeDestStation = getTramDestination(destinationName);
                }

                maybeDestStation.ifPresentOrElse(station -> {
                            String status = getNumberedField(jsonObject, "Status", index);
                            String waitString = getNumberedField(jsonObject, "Wait", index);
                            int waitInMinutes = Integer.parseInt(waitString);
                            String carriages = getNumberedField(jsonObject, "Carriages", index);
                            LocalTime lastUpdate = departureInfo.getLastUpdate().toLocalTime();
                            LocalDate date = departureInfo.getLastUpdate().toLocalDate();
                            Station displayLocation = departureInfo.getStation();

                            TramTime when = TramTime.ofHourMins(lastUpdate.plusMinutes(waitInMinutes));

                            UpcomingDeparture dueTram = new UpcomingDeparture(date, displayLocation, station, status,
                                    when, carriages, agency, TransportMode.Tram);
                            if (departureInfo.hasStationPlatform()) {
                                dueTram.setPlatform(departureInfo.getStationPlatform());
                            }
                            departureInfo.addDueTram(dueTram);

                        },

                        () -> logger.warn("Display id '" + departureInfo.getDisplayId() +
                                "' Unable to match due tram destination '" + destinationName +
                                "' index: " + index +" json '"+jsonObject+"'"));
            }
        }
    }

    private Optional<Station> getStationByAtcoCode(String atcoCode) {
        IdFor<Station> stationId = TransportEntityFactoryForTFGM.getStationIdFor(atcoCode);
        if (stationRepository.hasStationId(stationId)) {
            return Optional.of(stationRepository.getStationById(stationId));
        } else {
            return Optional.empty();
        }
    }

    private Optional<Station> getTramDestination(String name) {
        if (name.isEmpty())
        {
            logger.warn("Got empty name");
            return Optional.empty();
        }
        if (NotADestination.contains(name)) {
            logger.info(format("Not a destination: '%s'", name));
            return Optional.empty();
        }

        String destinationName = mapLiveAPIToTimetableDataNames(name);
        return stationByName.getTramStationByName(destinationName);
    }

    private String mapLiveAPIToTimetableDataNames(String destinationName) {
        destinationName = destinationName.replace("Via", "via");

        // assume station name is valid.....
        int viaIndex = destinationName.indexOf(" via");
        if (viaIndex > 0) {
            destinationName = destinationName.substring(0, viaIndex);
        }

        if (destinationNameMappings.containsKey(destinationName)) {
            return destinationNameMappings.get(destinationName);
        }

        return destinationName;
    }

    private String getNumberedField(JsonObject jsonObject, String name, final int i) {
        String destKey = format("%s%d", name, i);
        return (String) jsonObject.get(destKey);
    }
}
