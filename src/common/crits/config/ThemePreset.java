package crits.config;

import java.util.Arrays;

/** Original, bundled palettes. Colours are RGB; transparency is a separate preference. */
public enum ThemePreset {
    FROST("frost", "Frosted", "Soft charcoal · cool blue", 0x171B22, 0x12161D, 0x242A34, 0xF0F3FA, 0xADB6C9, 0x3B4351, 0x8AABFF),
    GLASS("glass", "Glass", "See your world through the menu", 0x111718, 0x101516, 0x222C2D, 0xEFF8F7, 0xA8BFBC, 0x3B4C4A, 0x8DDEC9),
    OBSIDIAN("obsidian", "Obsidian", "The original charcoal workspace", 0x0D0E10, 0x0B0C0E, 0x212327, 0xF2F2F2, 0xACAFB7, 0x34373E, 0x83CBA5),
    BLOCK("block", "Minecraft", "Square edges · familiar blue", 0x131B24, 0x101720, 0x26313E, 0xE7EAF9, 0xABB4CC, 0x414C60, 0x8D9FFF),
    MAUVE("mauve", "Mauve", "Pastel lilac over deep violet", 0x292938, 0x232330, 0x353546, 0xEAE5F8, 0xB8AFCB, 0x4D4961, 0xD6AEED),
    ROSE("rose", "Rose", "Soft pink · muted plum", 0x30303F, 0x282836, 0x3C3D50, 0xE8E4F7, 0xB6AEC9, 0x55536A, 0xF0B9DD),
    LATTE("latte", "Latte", "A bright canvas with coral accents", 0xEAE8EF, 0xDAD7E1, 0xF6F3F8, 0x38364E, 0x67617A, 0xB9B3C8, 0xA63D56),
    NORD("nord", "Nordic", "Slate blue · glacier cyan", 0x2E3440, 0x272E39, 0x3B4252, 0xECEFF4, 0xB6C2D5, 0x566276, 0x88C0D0),
    SNOW("snow", "Snow", "Clean daylight · arctic teal", 0xE7EEF1, 0xD4E0E5, 0xF4F8FA, 0x253C48, 0x536E7B, 0xAEC2CC, 0x24697B),
    AMBER("amber", "Ember", "Warm stone · copper light", 0x25211E, 0x1C1A18, 0x342D28, 0xF2E9D9, 0xC0AD96, 0x514538, 0xE4B379),
    FOREST("forest", "Forest", "Pine shadows · soft sage", 0x1B2622, 0x151E1A, 0x29372F, 0xE7F0E7, 0xABC1B0, 0x43584A, 0xACD39B),
    OCEAN("ocean", "Abyss", "Midnight blue · seafoam", 0x101E2C, 0x0C1722, 0x1A2E40, 0xE3F0FA, 0x9DB9CB, 0x324E64, 0x7DD5D1),
    SAND("sand", "Linen", "Warm paper · olive ink", 0xEEE8DC, 0xDFD6C6, 0xF8F4EB, 0x3D3B30, 0x6D6857, 0xBFB5A0, 0x636B35),
    CHERRY("cherry", "Cherry", "Ink black · dusty red", 0x21191E, 0x191317, 0x30242B, 0xF6E8EE, 0xC9ABB8, 0x513A45, 0xE9A0AF);

    public final String id, label, description;
    public final int background, sidebar, surface, text, muted, border, accent;
    ThemePreset(String id, String label, String description, int background, int sidebar, int surface,
                int text, int muted, int border, int accent) {
        this.id = id; this.label = label; this.description = description;
        this.background = background; this.sidebar = sidebar; this.surface = surface;
        this.text = text; this.muted = muted; this.border = border; this.accent = accent;
    }
    public static ThemePreset from(String id) {
        return Arrays.stream(values()).filter(p -> p.id.equals(id)).findFirst().orElse(FROST);
    }
}
