package io.github.mumberrymountain.util;

public final class Unit {

    private static final double HU_PER_MM = 7200.0 / 25.4; // 1mm ≈ 283.4645669

    private Unit() {}

    public static long mmToHu(double mm) {
        return Math.round(mm * HU_PER_MM);
    }

    public static int mmToHuInt(double mm) {
        return (int) Math.round(mm * HU_PER_MM);
    }

    public static double huToMm(long hu) {
        return hu / HU_PER_MM;
    }
}
