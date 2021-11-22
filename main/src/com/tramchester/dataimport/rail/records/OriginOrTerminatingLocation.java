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
        String tiplocCode = RecordHelper.extract(line, 3, 10+1);
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
}
