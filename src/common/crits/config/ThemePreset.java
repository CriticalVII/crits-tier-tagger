package crits.config;

import java.util.Arrays;

/** Original, bundled palettes. Colours are RGB; transparency is a separate preference. */
public enum ThemePreset {
    FROST("frost", "Frosted", "Soft charcoal · cool blue", 0x171B22, 0x12161D, 0x242A34, 0xF0F3FA, 0xADB6C9, 0x3B4351, 0x8AABFF),
    GLASS("glass", "Glass", "See your world through the menu", 0x111718, 0x101516, 0x222C2D, 0xEFF8F7, 0xA8BFBC, 0x3B4C4A, 0x8DDEC9),
    BLOCK("block", "Minecraft", "Square edges · familiar blue", 0x131B24, 0x101720, 0x26313E, 0xE7EAF9, 0xABB4CC, 0x414C60, 0x8D9FFF),
    MAUVE("mauve", "Mauve", "Pastel lilac over deep violet", 0x292938, 0x232330, 0x353546, 0xEAE5F8, 0xB8AFCB, 0x4D4961, 0xD6AEED),
    LATTE("latte", "Latte", "A bright canvas with coral accents", 0xEAE8EF, 0xDAD7E1, 0xF6F3F8, 0x38364E, 0x67617A, 0xB9B3C8, 0xA63D56),
    NORD("nord", "Nordic", "Slate blue · glacier cyan", 0x2E3440, 0x272E39, 0x3B4252, 0xECEFF4, 0xB6C2D5, 0x566276, 0x88C0D0);

    public final String id, label, description;
    public final int background, sidebar, surface, text, muted, border, accent;
    ThemePreset(String id, String label, String description, int background, int sidebar, int surface,
                int text, int muted, int border, int accent) {
        this.id = id; this.label = label; this.description = description;
        this.background = background; this.sidebar = sidebar; this.surface = surface;
        this.text = text; this.muted = muted; this.border = border; this.accent = accent;
    }
    public static ThemePreset from(String id) {
        String migrated = switch (id == null ? "" : id) {
            case "obsidian" -> "frost";
            case "rose", "cherry" -> "mauve";
            case "snow", "sand" -> "latte";
            case "amber", "forest", "ocean" -> "nord";
            default -> id == null ? "" : id;
        };
        return Arrays.stream(values()).filter(p -> p.id.equals(migrated)).findFirst().orElse(FROST);
    }
}
