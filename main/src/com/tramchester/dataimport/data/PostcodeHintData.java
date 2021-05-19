package com.tramchester.dataimport.data;

import com.tramchester.geo.BoundingBox;

import java.nio.file.Path;

public class PostcodeHintData {
    private String code;
    private long minEasting;
    private long minNorthing;
    private long maxEasting;
    private long maxNorthing;

    public PostcodeHintData() {
        // for deserialization
    }

    public PostcodeHintData(String code, BoundingBox boundingBox)
    {
        this.code = code;
        this.minEasting = boundingBox.getMinEastings();
        this.minNorthing = boundingBox.getMinNorthings();
        this.maxEasting = boundingBox.getMaxEasting();
        this.maxNorthing = boundingBox.getMaxNorthings();

    }

    public String getCode() {
        return code;
    }

    public long getMinEasting() {
        return minEasting;
    }

    public long getMinNorthing() {
        return minNorthing;
    }

    public long getMaxEasting() {
        return maxEasting;
    }

    public long getMaxNorthing() {
        return maxNorthing;
    }
}
