package com.tramchester.domain;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramTime;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Journey implements Iterable<TransportStage>, CallsAtPlatforms {

    private final List<Location> path;
    private final List<TransportStage> stages;
    private final TramTime queryTime;

    public Journey(List<TransportStage> stages, TramTime queryTime, List<Location> path) {
        this.stages = stages;
        this.queryTime = queryTime;
        this.path = path;
    }
    
    public Iterator<TransportStage> iterator() {
        return stages.iterator();
    }

    public List<TransportStage> getStages() {
        return stages;
    }

    @Override
    public List<HasId> getCallingPlatformIds() {
       return stages.stream().map(TransportStage::getBoardingPlatform).
               filter(Optional::isPresent).
               map(Optional::get).
               collect(Collectors.toList());
    }

    public TramTime getQueryTime() {
        return queryTime;
    }

    @Override
    public String toString() {
        return "Journey{" +
                "path=" + HasId.asIds(path) +
                ", stages=" + stages +
                ", queryTime=" + queryTime +
                '}';
    }

    public List<Location> getPath() {
        return path;
    }
}
