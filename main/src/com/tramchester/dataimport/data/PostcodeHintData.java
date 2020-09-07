package com.tramchester.dataimport.data;

public class PostcodeHintData {
    private final String file;
    private final int minEasting;
    private final int minNorthing;
    private final int maxEasting;
    private final int maxNorthing;

    public PostcodeHintData(String file, int minEasting, int minNorthing, int maxEasting, int maxNorthing) {

        this.file = file;
        this.minEasting = minEasting;
        this.minNorthing = minNorthing;
        this.maxEasting = maxEasting;
        this.maxNorthing = maxNorthing;
    }

    public String getFile() {
        return file;
    }

    public int getMinEasting() {
        return minEasting;
    }

    public int getMinNorthing() {
        return minNorthing;
    }

    public int getMaxEasting() {
        return maxEasting;
    }

    public int getMaxNorthing() {
        return maxNorthing;
    }
}
