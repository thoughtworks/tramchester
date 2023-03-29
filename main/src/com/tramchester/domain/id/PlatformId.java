package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;

import java.util.Objects;

public class PlatformId extends ContainsId<Platform> {
    private final String platformNumber;
    private final StringIdFor<Platform> containedId;

    private PlatformId(String stationText, String platformNumber) {
        containedId = new StringIdFor<>(stationText + platformNumber, Platform.class);
        this.platformNumber = platformNumber;
    }

    @Deprecated
    public static PlatformId createId(String text, String platformNumber) {
        return new PlatformId(text, platformNumber);
    }

    public static PlatformId createId(IdFor<Station> originalStationId, final String platformNumber) {
        StringIdFor<Station> stationId = (StringIdFor<Station>) originalStationId;
        final String stationIdText = stationId.getContainedId();
        final String updatedPlatformNumber = platformNumber.replace(stationIdText,"");
        if (updatedPlatformNumber.isEmpty()) {
            throw new RuntimeException("Resulting platform number is empty for " + originalStationId + " and " + platformNumber);
        }

        return new PlatformId(stationIdText, updatedPlatformNumber);
    }

    public static <FROM extends CoreDomain,TO extends CoreDomain> IdFor<TO> convert(IdFor<FROM> original, Class<TO> domainType) {
        guardForType(original);
        PlatformId originalPlatformId = (PlatformId) original;
        return StringIdFor.convert(originalPlatformId.getContainedId(), domainType);
    }

    private static <FROM extends CoreDomain> void guardForType(IdFor<FROM> original) {
        if (!(original instanceof PlatformId)) {
            throw new RuntimeException(original + " is not a PlatformId");
        }
    }

    @Override
    public String getGraphId() {
        return containedId.getGraphId();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Class<Platform> getDomainType() {
        return Platform.class;
    }

    public String getNumber() {
        return platformNumber;
    }

    @Override
    public StringIdFor<Platform> getContainedId() {
        return containedId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlatformId that = (PlatformId) o;
        return Objects.equals(platformNumber, that.platformNumber) && Objects.equals(containedId, that.containedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformNumber, containedId);
    }

    @Override
    public String toString() {
        return "PlatformId{" +
                "platformNumber='" + platformNumber + '\'' +
                ", containedId=" + containedId +
                "} ";
    }
}
