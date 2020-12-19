package com.tramchester.domain;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Journey implements Iterable<TransportStage<?,?>>, CallsAtPlatforms {

    private final List<Location<?>> path;
    private final List<TransportStage<?,?>> stages;
    private final TramTime queryTime;

    public Journey(List<TransportStage<?,?>> stages, TramTime queryTime, List<Location<?>> path) {
        this.stages = stages;
        this.queryTime = queryTime;
        this.path = path;
    }
    
    public @NotNull Iterator<TransportStage<?,?>> iterator() {
        return stages.iterator();
    }

    public List<TransportStage<?,?>> getStages() {
        return stages;
    }

    @Override
    public IdSet<Platform> getCallingPlatformIds() {
       return stages.stream().filter(TransportStage::hasBoardingPlatform).
               map(TransportStage::getBoardingPlatform).
               collect(IdSet.collector());
    }

    public TramTime getQueryTime() {
        return queryTime;
    }

    @Override
    public String toString() {
        return "Journey{" +
                "path=" + HasId.dtoAsIds(path) +
                ", stages=" + stages +
                ", queryTime=" + queryTime +
                '}';
    }

    public List<Location<?>> getPath() {
        return path;
    }

    public Set<TransportMode> getTransportModes() {
        return stages.stream().map(TransportStage::getMode).collect(Collectors.toSet());
    }
}
