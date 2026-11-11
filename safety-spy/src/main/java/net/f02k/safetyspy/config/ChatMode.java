package net.f02k.safetyspy.config;

import java.util.Locale;

/**
 * How public chat messages of spies are handled.
 */
public enum ChatMode {
    BLOCK,
    SEERS,
    PUBLIC;

    public static ChatMode parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return BLOCK;
        }
    }
}
