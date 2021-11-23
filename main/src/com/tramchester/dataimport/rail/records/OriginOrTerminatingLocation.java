package com.tramchester.dataimport.rail.records;

import com.tramchester.domain.time.TramTime;

public abstract class OriginOrTerminatingLocation {

    private final String tiplocCode;
    private final TramTime publicTime;
    private final String platform;

    protected OriginOrTerminatingLocation(String tiplocCode, TramTime publicTime, String platform) {
        this.tiplocCode = tiplocCode;
        this.publicTime = publicTime;
        this.platform = platform;
    }

    protected static <T extends OriginOrTerminatingLocation> T parse(String line, Constructor<T> builder) {
        // NOTE: for terminating and originating locations a suffix is added and docs give total length as 8
        // but this causes stations not to be found, so use length of 7 here
        String tiplocCode = RecordHelper.extract(line, 3, 10);
        TramTime tramTime = RecordHelper.extractTime(line, 15, 18+1);
        String platform = RecordHelper.extract(line, 20, 22+1);
        return builder.create(tiplocCode, tramTime, platform);
    }

    public String getTiplocCode() {
        return tiplocCode;
    }

    protected TramTime getPublicTime() {
        return publicTime;
    }

    public String getPlatform() {
        return platform;
    }

    protected interface Constructor<T> {
        T create(String tiplocCode, TramTime tramTime, String platform);
    }

    @Override
    public String toString() {
        return "OriginOrTerminatingLocation{" +
                "tiplocCode='" + tiplocCode + '\'' +
                ", publicTime=" + publicTime +
                ", platform='" + platform + '\'' +
                '}';
    }
}
