package com.tramchester.cloud.data;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static java.lang.String.format;

public class S3Keys {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.BASIC_ISO_DATE;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_TIME;

    public String create(LocalDateTime timeStamp, String environment) {
        // TODO this ought to be UTC, not local

        String date = timeStamp.toLocalDate().format(dateFormatter);
        String time = timeStamp.toLocalTime().format(timeFormatter);

        return format("%s/%s/%s", environment.toLowerCase(), date, time);
    }

    public String createPrefix(LocalDate timeStamp) {
        // TODO this ought to be UTC, not local

        return timeStamp.format(dateFormatter);
    }

    public LocalDateTime parse(String key) throws S3KeyException {
        String[] parts = key.split("/");

        if (parts.length!=3) {
            throw new S3KeyException("Cannot parse key: " + key);
        }

        try {
            LocalDate date = LocalDate.parse(parts[1], dateFormatter);
            LocalTime time = LocalTime.parse(parts[2], timeFormatter);
            return LocalDateTime.of(date, time);
        }
        catch (DateTimeParseException inner) {
            throw new S3KeyException("Could not parse " + key , inner);
        }
    }

    public static class S3KeyException extends Throwable {
        public S3KeyException(String message) {
            super(message);
        }

        public S3KeyException(String message, Exception exception) {
            super(message, exception);
        }
    }
}
