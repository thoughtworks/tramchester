package com.tramchester.acceptance;

import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class ConnectivityTest {

    @Test
    public void checkCanReachServer() {
        Optional<String> maybeUrl = Optional.ofNullable(System.getenv("SERVER_URL"));

        if (!maybeUrl.isPresent()) {
            return;
        }

        URI uri = URI.create(maybeUrl.get());
        String host = uri.getHost();

        boolean https = isReachable(host, 443);
        assertTrue(https);
        
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
