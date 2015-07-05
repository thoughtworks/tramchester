package com.tramchester.cloud;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;


public class StubbedAWSServer  {
    private String metadata;
    private String calledUrl;
    private String postedData;
    private Server server;
    private Handler handler;

    public StubbedAWSServer() {
        handler = new Handler();
    }

    public void run(String metadata) throws Exception {
        this.metadata = metadata;
        server = new Server(8080);
        server.setHandler(handler);
        server.start();
    }

    public void run() throws Exception {
        run("unused");
    }

    public String getCalledUrl() {
        return calledUrl;
    }

    public String getPostedData() {
        return postedData;
    }

    public void stopServer() throws Exception {
        server.stop();
        while (server.isRunning()) {
            Thread.sleep(30);
        }
        server.destroy();
    }

    private class Handler extends AbstractHandler{
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            calledUrl = request.getRequestURL().toString();
            if (request.getMethod().equals(HttpGet.METHOD_NAME)) {
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(metadata);
                baseRequest.setHandled(true);
            } else if (request.getMethod().equals(HttpPost.METHOD_NAME)) {
                response.setStatus(HttpServletResponse.SC_OK);
                BufferedReader reader = request.getReader();
                StringBuffer incoming = new StringBuffer();
                reader.lines().forEach(line -> incoming.append(line));
                postedData = incoming.toString();
            }
        }
    }

}
