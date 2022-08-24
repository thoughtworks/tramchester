package com.tramchester.dataimport.rail.records;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.lang.String.format;
import static java.time.temporal.ChronoField.*;

public class RecordHelper {
    private static final Logger logger = LoggerFactory.getLogger(RecordHelper.class);

    /***
     *
     * @param text string to extract record from
     * @param begin counting from 1, as per docs
     * @param end counting from 1, as per docs
     * @return the extracted record
     */
    public static String extract(String text, int begin, int end) {
        final int length = text.length();
        if ((begin-1) > length) {
            logger.warn(format("Record length too short (begin) was %s but looking for substring(%s,%s) in '%s'",
                    length, begin, end, text));
            return "";
        }
        if ((end-1) > length) {
            logger.warn(format("Record length too short (end) was %s but looking for substring(%s,%s) in '%s'",
                    length, begin, end, text));
            return text.substring(begin-1, length-1).trim();
        }

        return text.substring(begin-1, end-1).trim();
    }

    public static TramDate extractTramDate(String text, int begin, int century) {
        return TramDate.parseSimple(text, century, begin);
    }

    /***
     * Parse time in format HHMM embedded within larger string
     * @param text the text to extract time from
     * @param begin begin index of time
     * @return TramTime or TramTime.Invalid
     */
    public static TramTime extractTime(final String text, final int begin) {
        if (text.isBlank()) {
            return TramTime.invalid();
        }
        return TramTime.parseBasicFormat(text, begin);

    }
}
