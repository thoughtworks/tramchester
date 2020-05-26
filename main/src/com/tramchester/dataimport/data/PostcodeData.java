package com.tramchester.dataimport.data;

import java.util.Objects;

public class PostcodeData {
    private final String postcode;
    private final int eastings;
    private final int northings;

    public PostcodeData(String postcode, int eastings, int northings) {
        this.postcode = postcode;
        this.eastings = eastings;
        this.northings = northings;
    }

    public String getId() {
        return postcode;
    }

    public int getEastings() {
        return eastings;
    }

    public int getNorthings() {
        return northings;
    }

    @Override
    public String toString() {
        return "PostcodeData{" +
                "postcode='" + postcode + '\'' +
                ", eastings=" + eastings +
                ", northings=" + northings +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostcodeData that = (PostcodeData) o;
        return postcode.equals(that.postcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postcode);
    }
}
