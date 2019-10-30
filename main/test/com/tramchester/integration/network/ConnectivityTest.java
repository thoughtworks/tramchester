package com.tramchester.integration.network;

import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class ConnectivityTest {

    // Here to assist in diagnosing connectivity issues on the CI machines

    @Test
    public void checkCanReachDevServer() {
        Optional<String> maybeUrl = Optional.ofNullable(System.getenv("SERVER_URL"));

        if (!maybeUrl.isPresent()) {
            return;
        }

        URI uri = URI.create(maybeUrl.get());
        String host = uri.getHost();

        boolean reachable = isReachable(host, 443);
        assertTrue(reachable);
    }

    @Test
    public void shouldReachWellKnownServer() {
        boolean reachable = isReachable("google.co.uk", 443);
        assertTrue(reachable);
    }

    private boolean isReachable(String host, int port) {
        boolean opened = false;
        try {
            Socket socket = new Socket(host, port);
            opened = true;
            socket.close();
        } catch (IOException e) {
            // can't open socket
        }
        return opened;
    }
}
