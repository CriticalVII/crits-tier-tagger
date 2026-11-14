package crits.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** GUI-only preferences never recolour Minecraft's HUD or bundled mode artwork. */
public final class AppearanceConfig {
    public static final Set<String> PALETTE_KEYS = Set.of("background", "sidebar", "surface", "text", "muted", "border");
    public String theme = "frost";
    public String accent = "";
    public int opacity = 94;
    public int cornerRadius = 8;
    public boolean transparentGui = true;
    public boolean blur = true;
    public boolean animations = true;
    public boolean smoothFont = true;
    /** Overrides are applied only while customPalette is enabled, preserving the base preset. */
    public boolean customPalette = false;
    public Map<String, String> palette = new LinkedHashMap<>();

    public AppearanceConfig copy() {
        var copy = new AppearanceConfig();
        copy.theme = theme; copy.accent = accent; copy.opacity = opacity; copy.cornerRadius = cornerRadius;
        copy.transparentGui = transparentGui;
        copy.blur = blur; copy.animations = animations; copy.smoothFont = smoothFont; copy.customPalette = customPalette;
        copy.palette = palette == null ? new LinkedHashMap<>() : new LinkedHashMap<>(palette);
        return copy;
    }
    public void validate() {
        theme = ThemePreset.from(theme).id;
        accent = hex(accent, true);
        opacity = Math.max(60, Math.min(100, opacity));
        cornerRadius = Math.max(0, Math.min(12, cornerRadius));
        if (palette == null) palette = new LinkedHashMap<>();
        if (palette.size() > PALETTE_KEYS.size()) throw new IllegalArgumentException("Too many custom theme colours");
        var clean = new LinkedHashMap<String, String>();
        palette.forEach((key, value) -> {
            if (!PALETTE_KEYS.contains(key)) throw new IllegalArgumentException("Unknown theme colour");
            String colour = hex(value, true);
            if (!colour.isEmpty()) clean.put(key, colour);
        });
        palette = clean;
    }
    public static String hex(String value, boolean allowBlank) {
        String clean = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (clean.startsWith("#")) clean = clean.substring(1);
        if (allowBlank && clean.isEmpty() || clean.matches("[0-9A-F]{6}")) return clean;
        throw new IllegalArgumentException("Enter six hex digits, for example #8AABFF");
    }
    public int colour(String key, int fallback) {
        return customPalette && palette.containsKey(key) ? Integer.parseInt(palette.get(key), 16) : fallback;
    }
}
