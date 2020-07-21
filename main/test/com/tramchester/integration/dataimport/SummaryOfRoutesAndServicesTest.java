package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

import static java.lang.String.format;

class SummaryOfRoutesAndServicesTest {

    private static Dependencies dependencies;

    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = dependencies.get(TransportData.class);
    }

    @Test
    void createsSummaryOfRoutesAndServicesToAidInDiagnostics() throws FileNotFoundException {
        String fileName = "summaryOfRouteAndServices.txt";
        OutputStream fileStream = new FileOutputStream(fileName);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileStream);
        PrintStream printStream = new PrintStream(bufferedOutputStream);

        transportData.getRoutes().forEach(route -> {
            printStream.println(format("Route ID:'%s' Name:'%s'", route.getId(), route.getName()));
            route.getServices().forEach(svc -> {
                printStream.println(format("Service ID:%s  (%s)",svc.getId(), route.getName()));
                svc.summariseDates(printStream);

                svc.getTripsFor(route).forEach(trip ->{
                    StopCalls stops = trip.getStops();
                    stops.getLegs().forEach(leg -> {
                        printStream.println(format("Trip: %s From: %s To: %s %s %s",
                                trip.getId(), leg.getFirstStation().getName(),
                                leg.getSecondStation().getName(),
                                trip.earliestDepartTime().toPattern(), trip.latestDepartTime().toPattern()));
                    });
                });
                printStream.println();

            });
            printStream.println();
            printStream.println();
        });

        printStream.close();
    }


}
