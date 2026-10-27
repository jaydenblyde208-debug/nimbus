package dev.nimbus.core.policy;

public enum PolicyClass {
    /** Cosmetic / informational; always allowed (GUI, HUD telemetry). */
    SAFE,
    /** Gives information or control beyond vanilla; blocked in RESTRICTED mode (ESP, Freecam, Base Finder). */
    GATED
}
