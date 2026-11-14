package crits.gui;

import crits.config.AppearanceConfig;
import crits.config.ThemePreset;

/** Cached rendering tokens; no config copies or disk access in a widget's drawing path. */
public final class GuiTheme {
    private static AppearanceConfig preferences = new AppearanceConfig();
    static int background, sidebar, surface, text, muted, border, accent, hover, selection;
    static { apply(preferences); }
    private GuiTheme() { }

    public static void apply(AppearanceConfig next) {
        preferences = next.copy();
        var preset = ThemePreset.from(preferences.theme);
        background = rgb(preferences.colour("background", preset.background));
        sidebar = rgb(preferences.colour("sidebar", preset.sidebar));
        surface = rgb(preferences.colour("surface", preset.surface));
        text = rgb(preferences.colour("text", preset.text));
        muted = rgb(preferences.colour("muted", preset.muted));
        border = rgb(preferences.colour("border", preset.border));
        accent = rgb(preferences.accent.isEmpty() ? preset.accent : Integer.parseInt(preferences.accent, 16));
        hover = blend(surface, text, 0.07F);
        selection = blend(surface, accent, 0.19F);
    }
    static int rgb(int value) { return 0xFF000000 | value; }
    static int translucent(int value) { return ((preferences.transparentGui ? (int) (255 * preferences.opacity / 100F) : 255) << 24) | (value & 0xFFFFFF); }
    static int radius(int requested) {
        if (requested > 12) return requested; // Avatars and mode circles retain their shape.
        return preferences.theme.equals("block") ? 0 : Math.min(requested, preferences.cornerRadius);
    }
    static boolean animations() { return preferences.animations && net.minecraft.client.Minecraft.getInstance().options.screenEffectScale().get() > 0; }
    static boolean smoothFont() { return preferences.smoothFont; }
    static boolean blur() { return preferences.blur; }
    static int blend(int from, int to, float t) {
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8)
            out |= Math.round(((from >>> shift) & 255) * (1 - t) + ((to >>> shift) & 255) * t) << shift;
        return out;
    }
}
