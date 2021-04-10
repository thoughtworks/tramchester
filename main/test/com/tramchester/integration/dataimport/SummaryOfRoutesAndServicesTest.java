package com.tramchester.integration.dataimport;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

import static java.lang.String.format;

class SummaryOfRoutesAndServicesTest {

    private static ComponentContainer componentContainer;

    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
    }

    @Test
    void createsSummaryOqfRoutesAndServicesToAidInDiagnostics() throws FileNotFoundException {
        String fileName = "summaryOfRouteAndServices.txt";
        OutputStream fileStream = new FileOutputStream(fileName);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileStream);
        PrintStream printStream = new PrintStream(bufferedOutputStream);

        transportData.getRoutes().forEach(route -> {
            printStream.printf("Route ID:'%s' Name:'%s'%n", route.getId(), route.getName());
            route.getServices().forEach(svc -> {
                printStream.printf("Service ID:%s  (%s)%n",svc.getId(), route.getName());
                svc.summariseDates(printStream);

//                svc.getTripsFor(route).forEach(trip ->{
//                    StopCalls stops = trip.getStops();
//                    stops.getLegs().forEach(leg -> printStream.println(format("Trip: %s From: %s To: %s %s %s",
//                            trip.getId(), leg.getFirstStation().getName(),
//                            leg.getSecondStation().getName(),
//                            trip.earliestDepartTime().toPattern(), trip.latestDepartTime().toPattern())));
//                });
                printStream.println();

            });
            printStream.println();
            printStream.println();
        });

        printStream.close();
    }


}
