package dev.nimbus.core.policy;

public enum PolicyMode {
    /** Local single-player world: everything allowed. */
    SINGLEPLAYER,
    /** Multiplayer default: gated modules cannot be enabled. */
    RESTRICTED,
    /** User confirmed they have the server owner's permission; gated modules allowed and audited. */
    AUTHORIZED_TEST
}
