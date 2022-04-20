package com.tramchester.livedata.openLdb;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.thalesgroup.rtti._2013_11_28.token.types.AccessToken;
import com.thalesgroup.rtti._2017_10_01.ldb.GetBoardRequestParams;
import com.thalesgroup.rtti._2017_10_01.ldb.LDBServiceSoap;
import com.thalesgroup.rtti._2017_10_01.ldb.Ldb;
import com.thalesgroup.rtti._2017_10_01.ldb.StationBoardResponseType;
import com.thalesgroup.rtti._2017_10_01.ldb.types.StationBoard;
import com.tramchester.config.OpenLdbConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import static java.lang.String.format;

@LazySingleton
public class TrainDeparturesDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(TrainDeparturesDataFetcher.class);

    private final AccessToken accessToken;
    private final CRSRepository crsRepository;
    private LDBServiceSoap soapService;

    @Inject
    public TrainDeparturesDataFetcher(TramchesterConfig config, CRSRepository crsRepository) {
        this.crsRepository = crsRepository;
        final OpenLdbConfig openLdbConfig = config.getOpenldbwsConfig();

        accessToken = new AccessToken();
        accessToken.setTokenValue(openLdbConfig.getAccessToken());
    }

    @PostConstruct
    public void start() {
        Ldb soap = new Ldb();
        soapService = soap.getLDBServiceSoap12();
    }

    public StationBoard getFor(Station station) {
        if (!station.getTransportModes().contains(TransportMode.Train)) {
            logger.warn("Station is not a train station");
            return null;
        }
        if (!crsRepository.hasStation(station)) {
            logger.error("Not CRS Code found for " + station.getId());
            return null;
        }
        String crs = crsRepository.getCRSFor(station);
        logger.info("Get train departures for " + station.getId() + " with CRS " + crs);

        GetBoardRequestParams params = new GetBoardRequestParams();
        params.setCrs(crs);

        StationBoardResponseType departureBoard = soapService.getDepartureBoard(params, accessToken);

        final StationBoard stationBoardResult = departureBoard.getGetStationBoardResult();
        logger.info(format("Got departure board %s at %s for %s", stationBoardResult.getLocationName(),
                stationBoardResult.getGeneratedAt(), crs));

        return stationBoardResult;
    }
}
