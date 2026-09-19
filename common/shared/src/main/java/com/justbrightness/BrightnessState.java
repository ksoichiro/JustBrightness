package com.justbrightness;

public final class BrightnessState {
    private static boolean enabled = false;

    private BrightnessState() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static Double getOverrideGamma() {
        return BrightnessConfig.getGamma();
    }

    public static void applyWorldJoinDefault() {
        enabled = BrightnessConfig.isDefaultEnabled();
    }
}
