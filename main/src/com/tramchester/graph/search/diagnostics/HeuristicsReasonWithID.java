package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdFor;

import java.util.Objects;

public class HeuristicsReasonWithID<T extends CoreDomain> extends HeuristicsReason {
    private final IdFor<T> id;
    private final String typeName;

    protected HeuristicsReasonWithID(ReasonCode code, HowIGotHere path, IdFor<T> id) {
        super(code, path);
        this.id = id;
        typeName = id.getClass().getSimpleName();
    }

    @Override
    public String textForGraph() {
        return super.textForGraph() + System.lineSeparator() + " " +typeName+ ":" +id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HeuristicsReasonWithID<?> that = (HeuristicsReasonWithID<?>) o;
        return id.equals(that.id) && typeName.equals(that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, typeName);
    }
}
