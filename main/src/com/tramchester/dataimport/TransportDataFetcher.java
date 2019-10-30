package com.tramchester.dataimport;

import java.io.IOException;

public interface TransportDataFetcher {
    void fetchData(Unzipper unzipper) throws IOException;
}
