package dev.nimbus.core.module;

public enum Category {
    VISUALS("Visuals"), MOVEMENT("Movement"), WORLD("World"), CLIENT("Client");

    private final String displayName;
    Category(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
}
