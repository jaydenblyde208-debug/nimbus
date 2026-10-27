package dev.nimbus.core.event;

/** User-facing message. The GUI layer (Phase 2) renders these as toasts. */
public final class NotificationEvent extends Event {
    public enum Level { INFO, WARN, ERROR }
    public final Level level;
    public final String message;
    public NotificationEvent(Level level, String message) {
        this.level = level;
        this.message = message;
    }
}
