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

    private final TramTime queryTime;
    private final TramTime arrivalTime;
    private final TramTime departTime;
    private final int requestedNumberChanges;
    private final List<TransportStage<?,?>> stages;
    private final List<Location<?>> path;

    public Journey(TramTime departTime, TramTime queryTime, TramTime arrivalTime, List<TransportStage<?, ?>> stages,
                   List<Location<?>> path, int requestedNumberChanges) {
        this.stages = stages;
        this.queryTime = queryTime;
        this.path = path;
        this.departTime = departTime;
        this.arrivalTime = arrivalTime;
        this.requestedNumberChanges = requestedNumberChanges;
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
                "queryTime=" + queryTime +
                ", arrivalTime=" + arrivalTime +
                ", departTime=" + departTime +
                ", requestedNumberChanges=" + requestedNumberChanges +
                ", stages=" + stages +
                ", path=" + HasId.dtoAsIds(path) +
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

    public int getRequestedNumberChanges() {
        return requestedNumberChanges;
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
