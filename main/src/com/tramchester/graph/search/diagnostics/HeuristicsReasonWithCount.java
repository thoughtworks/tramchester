package com.tramchester.graph.search.diagnostics;

public class HeuristicsReasonWithCount extends HeuristicsReason {
    private final int count;

    protected HeuristicsReasonWithCount(ReasonCode code, HowIGotHere path, int count) {
        super(code, path);
        this.count = count;
    }

    @Override
    public String textForGraph() {
        return super.textForGraph() + " "+count;
    }
}
