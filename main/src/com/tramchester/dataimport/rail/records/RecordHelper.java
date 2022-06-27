package com.tramchester.dataimport.rail.records;

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

    private static final DateTimeFormatter dateFormat = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(YEAR, 4)
            .appendValue(MONTH_OF_YEAR, 2)
            .appendValue(DAY_OF_MONTH, 2)
            .toFormatter();

    private static final DateTimeFormatter century = new DateTimeFormatterBuilder().
            parseCaseInsensitive().appendValue(YEAR, 4).toFormatter();

    /***
     *
     * @param text string to extract record from
     * @param begin counting from 1, as per docs
     * @param end counting from 1, as per docs
     * @return the extracted record
     */
    public static String extract(String text, int begin, int end) {
        final int length = text.length();
        if (begin> length) {
            logger.warn(format("Record length too short was %s but looking for substring(%s,%s) in '%s'",
                    length, begin, end, text));
            return "";
        }
        if (end> length) {
            logger.warn(format("Record length too short was %s but looking for substring(%s,%s) in '%s'",
                    length, begin, end, text));
            return text.substring(begin-1, length -1).trim();
        }

        return text.substring(begin-1, end-1).trim();
    }

    public static LocalDate extractDate(String text, int begin, int end, ProvidesNow providesNow) {
        String fullYear = providesNow.getDate().format(century);
        String rawDateWithCentury = fullYear.substring(0, 2) + extract(text, begin, end);
        return LocalDate.parse(rawDateWithCentury, dateFormat);
    }

    public static TramTime extractTime(String text, int begin, int end) {
        String rawTime = text.substring(begin, end);
        if (rawTime.length()!=4 || rawTime.isBlank()) {
            return TramTime.invalid();
        }
        int hour = Integer.parseUnsignedInt(rawTime,0, 2 ,10);
        int minite = Integer.parseUnsignedInt(rawTime,2, 4 ,10);
        return TramTime.of(hour, minite);
    }
}
