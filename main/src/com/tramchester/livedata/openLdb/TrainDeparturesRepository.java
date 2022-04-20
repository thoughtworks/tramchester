package com.tramchester.livedata.openLdb;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.thalesgroup.rtti._2015_11_27.ldb.types.ArrayOfServiceLocations;
import com.thalesgroup.rtti._2015_11_27.ldb.types.ServiceLocation;
import com.thalesgroup.rtti._2017_10_01.ldb.types.CoachData;
import com.thalesgroup.rtti._2017_10_01.ldb.types.FormationData;
import com.thalesgroup.rtti._2017_10_01.ldb.types.ServiceItem;
import com.thalesgroup.rtti._2017_10_01.ldb.types.StationBoard;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.UpcomingDeparturesSource;
import com.tramchester.repository.AgencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@LazySingleton
public class TrainDeparturesRepository implements UpcomingDeparturesSource {
    private static final Logger logger = LoggerFactory.getLogger(TrainDeparturesRepository.class);

    private final TrainDeparturesDataFetcher dataFetcher;
    private final AgencyRepository agencyRepository;
    private final CRSRepository crsRepository;

    @Inject
    public TrainDeparturesRepository(TrainDeparturesDataFetcher dataFetcher, AgencyRepository agencyRepository, CRSRepository crsRepository) {
        this.dataFetcher = dataFetcher;
        this.agencyRepository = agencyRepository;
        this.crsRepository = crsRepository;
    }

    @Override
    public List<UpcomingDeparture> forStation(Station station) {
        StationBoard board = dataFetcher.getFor(station);
        LocalDateTime generated = getDate(board);
        return board.getTrainServices().getService().stream().
                map(serviceItem -> map(serviceItem, station, generated)).
                collect(Collectors.toList());
    }

    private LocalDateTime getDate(StationBoard board) {
        XMLGregorianCalendar generated = board.getGeneratedAt();

        LocalDate date =  LocalDate.of(generated.getYear(), generated.getMonth(), generated.getDay());
        LocalTime time = LocalTime.of(generated.getHour(), generated.getMinute(), generated.getSecond());

        return LocalDateTime.of(date, time);
    }

    private UpcomingDeparture map(ServiceItem serviceItem, Station displayLocation, LocalDateTime generated) {

        String carridges = carridgesFrom(serviceItem.getFormation());
        Agency agency = agencyFrom(serviceItem.getOperatorCode());
        Station destination = destinationFrom(serviceItem.getDestination());
        String status = getStatus(serviceItem);
        Duration wait = getWait(generated, serviceItem);

        return new UpcomingDeparture(generated.toLocalDate(), displayLocation,
                destination, status, wait, carridges, generated.toLocalTime(), agency, TransportMode.Train);

    }

    private String getStatus(ServiceItem serviceItem) {
        final String etd = serviceItem.getEtd();
        logger.info("Get status from " + etd);
        // todo if not 'On time', then there is a delay?
        return etd;
    }

    private Duration getWait(LocalDateTime generated, ServiceItem serviceItem) {
        final String std = serviceItem.getStd();
        logger.info("Get wait from " + std);
        LocalTime departureTime = LocalTime.parse(std);

        final Duration duration = Duration.between(generated.toLocalTime(), departureTime);
        // TODO
        // Right now don't store seconds in TramTime so need to round
        long minutes = duration.toMinutes();
        long remainder = duration.minusMinutes(minutes).getSeconds();
        if (remainder>30) {
            minutes = minutes + 1;
        }
        Duration rounded = Duration.ofMinutes(minutes);
        logger.info("Wait Duration is " + duration + " and rounded is " + rounded);
        return rounded;
    }

    private Station destinationFrom(ArrayOfServiceLocations destination) {
        List<ServiceLocation> dests = destination.getLocation();
        if (dests.size()>1) {
            logger.warn("Number of destinations was " + dests.size());
        }
        String crs = dests.get(0).getCrs();
        logger.info("Find destination from " + crs);

        if (!crsRepository.hasCrs(crs)) {
            return MutableStation.Unknown(DataSourceID.rail);
        }
        return crsRepository.getFor(crs);
    }

    private Agency agencyFrom(String operatorCode) {
        logger.info("Find agency from " + operatorCode);
        return agencyRepository.get(StringIdFor.createId(operatorCode));
    }

    private String carridgesFrom(FormationData formation) {
        if (formation==null) {
            logger.warn("Formation missing");
            return "Unknown formation";
        }
        if (formation.getCoaches()==null) {
            logger.info("Unknown formation, coaches");
            return "Unknown formation";
        }
        List<CoachData> coaches = formation.getCoaches().getCoach();
        return "Formed by " + coaches.size() + " coaches";
    }


}
