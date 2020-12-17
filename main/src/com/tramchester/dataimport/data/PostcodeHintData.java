package com.tramchester.dataimport.data;

import com.tramchester.geo.BoundingBox;

import java.nio.file.Path;

public class PostcodeHintData {
    private String file;
    private long minEasting;
    private long minNorthing;
    private long maxEasting;
    private long maxNorthing;

    public PostcodeHintData() {
        // for deserialization
    }

    public PostcodeHintData(Path path, BoundingBox boundingBox)
    {
        this.file = path.toString();
        this.minEasting = boundingBox.getMinEastings();
        this.minNorthing = boundingBox.getMinNorthings();
        this.maxEasting = boundingBox.getMaxEasting();
        this.maxNorthing = boundingBox.getMaxNorthings();

    }

    public String getFile() {
        return file;
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
