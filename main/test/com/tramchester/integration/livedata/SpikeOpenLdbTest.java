package com.tramchester.integration.livedata;

import com.thalesgroup.rtti._2013_11_28.token.types.AccessToken;
import com.thalesgroup.rtti._2017_10_01.ldb.GetBoardRequestParams;
import com.thalesgroup.rtti._2017_10_01.ldb.LDBServiceSoap;
import com.thalesgroup.rtti._2017_10_01.ldb.Ldb;
import com.thalesgroup.rtti._2017_10_01.ldb.StationBoardResponseType;
import com.thalesgroup.rtti._2017_10_01.ldb.types.ServiceItem;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.OpenLdbConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

class SpikeOpenLdbTest {

    private static GuiceContainerDependencies componentContainer;
    private static TramchesterConfig config;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(true), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        config = componentContainer.get(TramchesterConfig.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldTestSomething() {
        final OpenLdbConfig openLdbConfig = config.getOpenldbwsConfig();

        var accessToken = new AccessToken();
        accessToken.setTokenValue(openLdbConfig.getAccessToken());
        //accessToken.setTokenValue("402dee5a-7e76-422b-9aa1-22da6f3be0a7");

        Ldb soap = new Ldb();
        LDBServiceSoap soapService = soap.getLDBServiceSoap12();

        GetBoardRequestParams params = new GetBoardRequestParams();
        params.setCrs("EUS");

        StationBoardResponseType departureBoard = soapService.getDepartureBoard(params, accessToken);

        List<ServiceItem> service = departureBoard.getGetStationBoardResult().getTrainServices().getService();
    }
}
