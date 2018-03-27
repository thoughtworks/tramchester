package com.tramchester.acceptance.infra;

import com.tramchester.acceptance.pages.ProvidesDateInput;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.util.Locale;

public class ProvidesFirefoxDateInput implements ProvidesDateInput {

    private Locale locale = Locale.getDefault();

    @Override
    public String createDateInput(LocalDate localDate) {
        String pattern = DateTimeFormat.patternForStyle("S-", locale);
        if (!pattern.contains("yyyy")) {
            pattern = pattern.replace("yy", "yyyy");
        }
        return localDate.toString(pattern.replaceAll("/",""));
    }
}
