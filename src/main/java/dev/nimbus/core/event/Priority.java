package dev.nimbus.core.event;

/** Higher values run first. */
public final class Priority {
    public static final int HIGH = 100;
    public static final int NORMAL = 0;
    public static final int LOW = -100;
    private Priority() {}
}
