package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.PostcodeHintData;
import org.apache.commons.csv.CSVRecord;

import java.util.List;

public class PostcodeHintsDataMapper extends CSVEntryMapper<PostcodeHintData> {

    private int indexOfFile = -1;
    private int indexOfMinEasting = -1;
    private int indexOfMinNorthing = -1;
    private int indexOfMaxEasting = -1;
    private int indexOfMaxNorthing = -1;

    public enum Columns implements ColumnDefination {
        file, minEasting, minNorthing, maxEasting, maxNorthing
    }

    @Override
    public PostcodeHintData parseEntry(CSVRecord data) {
        String file = data.get(indexOfFile);
        int minEasting = Integer.parseInt(data.get(indexOfMinEasting));
        int minNorthing = Integer.parseInt(data.get(indexOfMinNorthing));
        int maxEasting = Integer.parseInt(data.get(indexOfMaxEasting));
        int maxNorthing = Integer.parseInt(data.get(indexOfMaxNorthing));

        return new PostcodeHintData(file, minEasting, minNorthing, maxEasting, maxNorthing);

    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        return true;
    }

    @Override
    protected void initColumnIndex(List<String> headers) {
        indexOfFile = findIndexOf(headers, PostcodeHintsDataMapper.Columns.file);
        indexOfMinEasting = findIndexOf(headers, PostcodeHintsDataMapper.Columns.minEasting);
        indexOfMinNorthing = findIndexOf(headers, PostcodeHintsDataMapper.Columns.minNorthing);
        indexOfMaxEasting = findIndexOf(headers, PostcodeHintsDataMapper.Columns.maxEasting);
        indexOfMaxNorthing = findIndexOf(headers, PostcodeHintsDataMapper.Columns.maxNorthing);

    }
}
