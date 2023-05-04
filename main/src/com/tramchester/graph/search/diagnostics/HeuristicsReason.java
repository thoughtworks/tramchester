package com.tramchester.graph.search.diagnostics;

import java.util.Objects;

public abstract class HeuristicsReason {

    protected final HowIGotHere howIGotHere;
    protected final ReasonCode code;

    public HowIGotHere getHowIGotHere() {
        return howIGotHere;
    }

    protected HeuristicsReason(ReasonCode code, HowIGotHere path) {
        this.code = code;
        this.howIGotHere = path;
    }

    public String textForGraph() {
        return code.name();
    }

    public ReasonCode getReasonCode() {
        return code;
    }

    // DEFAULT
    public boolean isValid() {
        return false;
    }

    @Override
    public String toString() {
        return code.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeuristicsReason that = (HeuristicsReason) o;
        return Objects.equals(howIGotHere, that.howIGotHere) &&
                code == that.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(howIGotHere, code);
    }

    public Long getNodeId() {
        return howIGotHere.getEndNodeId();
    }
}
