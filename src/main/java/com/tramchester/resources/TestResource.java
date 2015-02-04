package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;
import com.tramchester.dataimport.parsers.StopParser;
import com.tramchester.domain.Stop;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {


    @GET
    @Timed
    public Response get() {


        try {
            Reader reader = new FileReader("data/tram/stops.txt");
            CSVReader<Stop> csvPersonReader = new CSVReaderBuilder<Stop>(reader).entryParser(new StopParser()).strategy(CSVStrategy.UK_DEFAULT).build();
            List<Stop> persons = csvPersonReader.readAll();
            return Response.ok(persons).build();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Response.ok("ssssss").build();
    }
}


