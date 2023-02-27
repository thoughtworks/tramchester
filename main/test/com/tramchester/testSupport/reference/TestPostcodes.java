package com.tramchester.testSupport.reference;

import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import org.jetbrains.annotations.NotNull;

public class TestPostcodes {
    public static final PostcodeLocation CentralBury = createLocation("BL90AY", 380377, 410652, "BL");
    public static final PostcodeLocation NearPiccadillyGardens = createLocation("M11RG", 384415, 398217, "M");
    public static final PostcodeLocation NearShudehill = createLocation("M44AA", 384339, 398749, "M");

    public static String postcodeForWythenshaweHosp() {
        return "M239LT";
    }

    public static String postcodeForPiccGardens() {
        return "M601AY";
    }

    @NotNull
    private static PostcodeLocation createLocation(String id, int eastings, int northings, String area) {
        return new PostcodeLocation(CoordinateTransforms.getLatLong(new GridPosition(eastings, northings)),
                PostcodeLocation.createId(id));
    }

}
