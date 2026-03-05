package dev.pcrykh.achievements;

public enum MovementMode {
    WALK("walk"),
    SPRINT("sprint"),
    SNEAK("sneak"),
    SWIM("swim"),
    JUMP("jump"),
    ETHEREAL_WING("ethereal_wing"),
    BOAT("boat");

    private final String token;

    MovementMode(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }
}
