package dev.crits.tiertagger.provider;

import java.util.Locale;

/** Strict UUID keys for the external tierlists; AfriTiers retains its existing identity rules. */
public final class PlayerIdentity {
    private PlayerIdentity() { }
    public static String uuid(String value) {
        if (value == null || !value.matches("(?i)([0-9a-f]{32}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")) return null;
        String compact = value.replace("-", "").toLowerCase(Locale.ROOT);
        return compact.equals("00000000000000000000000000000000") ? null : compact;
    }
    public static String dashed(String compact) {
        String key = uuid(compact);
        if (key == null) throw new IllegalArgumentException("Invalid Minecraft account UUID");
        return key.substring(0, 8) + "-" + key.substring(8, 12) + "-" + key.substring(12, 16) + "-" + key.substring(16, 20) + "-" + key.substring(20);
    }
}
