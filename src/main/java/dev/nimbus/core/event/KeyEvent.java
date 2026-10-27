package dev.nimbus.core.event;

/** Raw key input, fed by the platform layer (Phase 2). Action values match GLFW. */
public final class KeyEvent extends Event {
    public static final int RELEASE = 0, PRESS = 1, REPEAT = 2;
    public final int key;
    public final int action;
    public final boolean screenOpen;

    public KeyEvent(int key, int action, boolean screenOpen) {
        this.key = key;
        this.action = action;
        this.screenOpen = screenOpen;
    }
}
