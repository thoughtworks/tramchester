package com.tramchester.cloud;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class TestFetchInstanceMetadata {

    @Test
    public void shouldFetchInstanceMetadata() throws Exception {
        URL url = new URL("http://localhost:8080/");
        FetchInstanceMetadata fetcher = new FetchInstanceMetadata(url);

        SimplestServer server = new SimplestServer();
        server.run("someSimpleMetaData");

        String data = fetcher.getUserData();
        assertThat(data).isEqualTo("someSimpleMetaData");
        assertThat(server.calledUrl).isEqualTo("http://localhost:8080/latest/user-data");
        server.stop();

    }

    public class SimplestServer extends AbstractHandler {
        private String metadata;
        private String calledUrl;

        public void run(String metadata) throws Exception
        {
            this.metadata = metadata;
            Server server = new Server(8080);
            server.setHandler(this);
            server.start();
        }


        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            calledUrl = request.getRequestURL().toString();
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(metadata);
            baseRequest.setHandled(true);
        }
    }
}
