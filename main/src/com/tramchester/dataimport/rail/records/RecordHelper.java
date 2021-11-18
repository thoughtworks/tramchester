package com.tramchester.dataimport.rail.records;

public class RecordHelper {
    /***
     *
     * @param line string to extract record from
     * @param begin counting from 1, as per docs
     * @param end counting from 1, as per docs
     * @return the extracted record
     */
    public static String extract(String line, int begin, int end) {
        return line.substring(begin-1, end-1).trim();
    }
}
