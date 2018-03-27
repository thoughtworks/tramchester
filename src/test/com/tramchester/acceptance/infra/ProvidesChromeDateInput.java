package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.util.Locale;

public class ProvidesChromeDateInput implements ProvidesDateInput {
    Locale locale = Locale.getDefault();

    @Override
    public String createDateInput(LocalDate localDate) {
        String formatter = DateTimeFormat.patternForStyle("S-", locale);
        return localDate.toString(formatter.replaceAll("y","").replaceAll("Y",""));
    }
}
