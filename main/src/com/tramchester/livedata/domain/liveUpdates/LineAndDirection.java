package com.tramchester.livedata.domain.liveUpdates;

public class LineAndDirection {
    private final Lines line;
    private final LineDirection direction;

    public final static LineAndDirection Unknown = new LineAndDirection(Lines.UnknownLine, LineDirection.Unknown);

    public LineAndDirection(Lines line, LineDirection direction) {
        this.line = line;
        this.direction = direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LineAndDirection that = (LineAndDirection) o;

        if (line != that.line) return false;
        return direction == that.direction;
    }

    @Override
    public int hashCode() {
        int result = line.hashCode();
        result = 31 * result + direction.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LineAndDirection{" +
                "line=" + line +
                ", direction=" + direction +
                '}';
    }

    public LineDirection getDirection() {
        return direction;
    }

    public Lines getLine() {
        return line;
    }
}
