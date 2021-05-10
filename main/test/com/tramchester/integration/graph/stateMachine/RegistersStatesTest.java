package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.Set;

class RegistersStatesTest {
    private static ComponentContainer componentContainer;
    private RegistersStates registersStates;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachOfTheTestsRun() {
        componentContainer.get(TraversalStateFactory.class); // trigger population
        registersStates = componentContainer.get(RegistersStates.class);
    }

    @Test
    void shouldCreateStateTransitionDiagram() throws FileNotFoundException {
        Path filePath = Path.of("stateTransitions.dot");
        OutputStream fileStream = new FileOutputStream(filePath.toFile());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileStream);
        PrintStream printStream = new PrintStream(bufferedOutputStream);

        printStream.print("digraph G {\n");
        Set<RegistersStates.FromTo> transitions = registersStates.getTransitions();
        transitions.forEach(transition -> printStream.format("\"%s\"->\"%s\";\n",
                transition.getFrom().getSimpleName(), transition.getTo().getSimpleName()));
        printStream.print("}");

        printStream.close();
    }

}