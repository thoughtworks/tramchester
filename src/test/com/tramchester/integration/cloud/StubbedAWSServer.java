package com.tramchester.integration.cloud;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
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
    private String contentHeader;

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

    public String getPutData() {
        return postedData;
    }

    public void stopServer() throws Exception {
        server.stop();
        while (server.isRunning()) {
            Thread.sleep(30);
        }
        server.destroy();
    }

    public String getContentHeader() {
        return contentHeader;
    }

    private class Handler extends AbstractHandler{
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            calledUrl = request.getRequestURL().toString();
            contentHeader = request.getHeader("Content-Type");
            if (request.getMethod().equals(HttpGet.METHOD_NAME)) {
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(metadata);
                baseRequest.setHandled(true);
            } else if (request.getMethod().equals(HttpPut.METHOD_NAME)) {
                response.setStatus(HttpServletResponse.SC_OK);
                BufferedReader reader = request.getReader();
                StringBuffer incoming = new StringBuffer();
                reader.lines().forEach(line -> incoming.append(line));
                postedData = incoming.toString();
            }
        }
    }

}
