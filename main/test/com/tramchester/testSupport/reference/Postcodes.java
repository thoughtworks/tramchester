package com.tramchester.testSupport.reference;

import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;

public class Postcodes {
    public static PostcodeLocation CentralBury = createLocation("BL90AY", 380377, 410652);
    public static PostcodeLocation NearPiccadillyGardens = createLocation("M11RG", 384415, 398217);
    public static PostcodeLocation NearShudehill = createLocation("M44AA", 384339, 398749);

    @NotNull
    private static PostcodeLocation createLocation(String id, int eastings, int northings) {
        return new PostcodeLocation(getLatLong(eastings, northings), id);
    }

    @NotNull
    private static LatLong getLatLong(int eastings, int northings) {
        try {
            return CoordinateTransforms.getLatLong(eastings, northings);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }
}
