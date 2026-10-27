package dev.nimbus.core.event;

/** Posted once per client tick. {@code inWorld} is false on the title screen etc. */
public final class TickEvent extends Event {
    public final boolean inWorld;
    public TickEvent(boolean inWorld) { this.inWorld = inWorld; }
}
