package org.hectoris919.CBCGoingBallistic.compat;

public final class RadarCbcModelMarker {
    private static final ThreadLocal<Double> NEXT_AUTOCANNON_DRAG_DENSITY = new ThreadLocal<>();

    private RadarCbcModelMarker() { }

    public static void markNextAutocannon(double dimensionDragMultiplier) {
        NEXT_AUTOCANNON_DRAG_DENSITY.set(dimensionDragMultiplier);
    }

    public static Double consume() {
        Double density = NEXT_AUTOCANNON_DRAG_DENSITY.get();
        NEXT_AUTOCANNON_DRAG_DENSITY.remove();
        return density;
    }
}