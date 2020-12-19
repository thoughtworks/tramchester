package com.tramchester.geo;

public class GridPositions {

    public static boolean withinDistEasting(GridPosition gridPositionA, GridPosition gridPositionB, long rangeInMeters) {
        return rangeInMeters >= getDistEasting(gridPositionA, gridPositionB) ;
    }

    public static boolean withinDistNorthing(GridPosition gridPositionA, GridPosition gridPositionB, long rangeInMeters) {
        return rangeInMeters >= getDistNorthing(gridPositionA, gridPositionB);
    }

    private static long getDistNorthing(GridPosition gridPositionA, GridPosition gridPositionB) {
        return Math.abs(gridPositionA.getNorthings() - gridPositionB.getNorthings());
    }

    private static long getDistEasting(GridPosition gridPositionA, GridPosition gridPositionB) {
        return Math.abs(gridPositionA.getEastings() - gridPositionB.getEastings());
    }

    private static long getSumSquaresDistance(GridPosition gridPositionA, GridPosition gridPositionB) {
        long distHorz = getDistEasting(gridPositionA, gridPositionB);
        long distVert = getDistNorthing(gridPositionA, gridPositionB);
        return (distHorz * distHorz) + (distVert * distVert);
    }

    public static boolean withinDist(GridPosition gridPositionA, GridPosition gridPositionB, long rangeInMeters) {
        long hypSquared = rangeInMeters*rangeInMeters;
        long sum = getSumSquaresDistance(gridPositionA, gridPositionB);
        return sum<=hypSquared;
    }

    public static long distanceTo(GridPosition gridPositionA, GridPosition gridPositionB) {
        long sum = getSumSquaresDistance(gridPositionA, gridPositionB);
        return Math.round(Math.sqrt(sum));
    }

}
