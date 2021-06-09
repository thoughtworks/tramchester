package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Journey implements Iterable<TransportStage<?,?>>, CallsAtPlatforms {

    private final List<Location<?>> path;
    private final List<TransportStage<?,?>> stages;
    private final TramTime queryTime;
    private final TramTime departTime;
    private final TramTime arrivalTime;

    public Journey(List<TransportStage<?, ?>> stages, TramTime queryTime, List<Location<?>> path, TramTime departTime,
                   TramTime arrivalTime) {
        this.stages = stages;
        this.queryTime = queryTime;
        this.path = path;
        this.departTime = departTime;
        this.arrivalTime = arrivalTime;
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
                ", arrivalTime=" + arrivalTime +
                ", departTime=" + departTime +
                '}';
    }

    public List<Location<?>> getPath() {
        return path;
    }

    public Set<TransportMode> getTransportModes() {
        return stages.stream().map(TransportStage::getMode).collect(Collectors.toSet());
    }

    public TramTime getArrivalTime() {
        return arrivalTime;
    }

    public TramTime getDepartTime() {
        return departTime;
    }


    public boolean isDirect() {
        int size = stages.size();

        if (size == 1) {
            return true;
        }
        if (firstStageIsWalk() && size == 2) {
            return true;
        }
        return false;
    }

    public boolean firstStageIsWalk() {
        return stages.get(0).getMode()==TransportMode.Walk;
    }

    public Location<?> getBeginning() {
        if (firstStageIsWalk()) {
            // TODO Check if this workaround still needed or used?
            if (stages.size()>1) {
                return stages.get(1).getFirstStation();
            }
        }
        return stages.get(0).getFirstStation();
    }

    public List<Location<?>> getChangeStations() {
        List<Location<?>> result = new ArrayList<>();

        if (isDirect()) {
            return result;
        }

        for(int index = 1; index< stages.size(); index++) {
            result.add(stages.get(index).getFirstStation());
        }

        return result;
    }

}
