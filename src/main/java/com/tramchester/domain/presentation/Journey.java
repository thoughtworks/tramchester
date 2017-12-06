package com.tramchester.domain.presentation;


import java.util.List;

public class Journey {
    private List<TransportStage> allStages;

    public Journey(List<TransportStage> allStages) {
        this.allStages = allStages;
    }

    public List<TransportStage> getStages() {
        return allStages;
    }

    @Override
    public String toString() {
        return  "Journey{" +
                "stages= [" +allStages.size() +"] "+ allStages +
                '}';
    }

}
