package com.tramchester.integration.dataimport;

import com.tramchester.Dependencies;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.time.DaysOfWeek;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TransportDataFromFiles;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.junit.Assert.fail;

public class SummaryOfRoutesAndServicesTest {

    private static Dependencies dependencies;

    private TransportDataFromFiles transportData;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        transportData = dependencies.get(TransportDataFromFiles.class);
    }

    @Test
    public void createsSummaryOfRoutesAndServicesToAidInDiagnostics() throws FileNotFoundException {
        String fileName = "summaryOfRouteAndServices.txt";
        OutputStream fileStream = new FileOutputStream(fileName);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileStream);
        PrintStream printStream = new PrintStream(bufferedOutputStream);

        transportData.getRoutes().forEach(route -> {
            printStream.println(format("Route ID:'%s' Name:'%s'", route.getId(), route.getName()));
            route.getServices().forEach(svc -> {
                printStream.println(format("Service ID:%s  (%s)",svc.getId(), route.getName()));
                svc.summariseDates(printStream);

                svc.getTrips().forEach(trip ->{
                    StopCalls stops = trip.getStops();
                    String from = stops.get(0).getStation().getName();
                    String to = stops.get(stops.size()-1).getStation().getName();
                    printStream.println(format("Trip: %s From: %s To: %s %s %s",
                            trip.getId(), from, to, trip.earliestDepartTime().toPattern(), trip.latestDepartTime().toPattern()));
                });
                printStream.println();

            });
            printStream.println();
            printStream.println();
        });

        printStream.close();
    }


}
