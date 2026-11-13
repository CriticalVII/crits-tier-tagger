package crits.ranking;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Stable glyphs and provider aliases for the bundled smooth mode artwork. */
public enum ModeIcon {
    AXE('\uE000', "axe", "Axe", "axe"),
    BED('\uE001', "bed-pvp", "Bed PvP", "bed", "bed_pvp"),
    CART('\uE002', "cart", "Cart", "cart", "minecart"),
    CREEPER('\uE003', "creeper", "Creeper", "creeper"),
    DIAMOND_VANILLA('\uE004', "diamond-vanilla", "Diamond Vanilla", "diavanilla", "diamond_vanilla", "dia_crystal"),
    DIAPOT('\uE005', "diapot", "DiaPot", "diapot", "diamond_pot", "pot"),
    DIASMP('\uE006', "diasmp", "DiaSMP", "diasmp", "diamond_smp", "dia_smp"),
    MACE('\uE007', "mace", "Mace", "mace"),
    NETHOP('\uE008', "nethop", "NethOP", "nethop", "nethpot", "nether_pot", "neth_pot"),
    OG_VANILLA('\uE009', "og-vanilla", "OG Vanilla", "ogv", "og_vanilla"),
    OVERALL('\uE00A', "overall", "Overall", "overall"),
    SMP('\uE00B', "smp", "SMP", "smp"),
    SPEAR_MACE('\uE00C', "spear-mace", "Spear Mace", "spear", "spear_mace", "spearmace"),
    SPEED('\uE00D', "speed", "Speed", "speed"),
    SWORD('\uE00E', "sword", "Sword", "sword"),
    UHC('\uE00F', "uhc", "UHC", "uhc"),
    VANILLA('\uE010', "vanilla", "Vanilla", "vanilla", "crystal"),
    BOW('\uE011', "bow", "Bow", "bow"),
    DEBUFF('\uE012', "debuff", "Debuff", "debuff"),
    ELYTRA('\uE013', "elytra", "Elytra", "elytra"),
    MANHUNT('\uE014', "manhunt", "Manhunt", "manhunt"),
    TRIDENT('\uE015', "trident", "Trident", "trident");

    private final char glyph;
    private final String texture;
    private final String displayName;
    private final List<String> modes;
    private static final Map<String, ModeIcon> BY_MODE;

    static {
        Map<String, ModeIcon> icons = new HashMap<>();
        for (ModeIcon icon : values()) for (String mode : icon.modes) icons.put(mode, icon);
        BY_MODE = Map.copyOf(icons);
    }

    ModeIcon(char glyph, String texture, String displayName, String... modes) {
        this.glyph = glyph;
        this.texture = texture;
        this.displayName = displayName;
        this.modes = List.of(modes);
    }

    public String glyph() { return Character.toString(glyph); }
    public String texture() { return texture; }
    public String displayName() { return displayName; }
    public String primaryMode() { return modes.getFirst(); }

    public static Optional<ModeIcon> find(String mode) {
        if (mode == null) return Optional.empty();
        String normalized = mode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return Optional.ofNullable(BY_MODE.get(normalized));
    }

    /** Unexpected future API modes use a neutral trophy until artwork is added. */
    public static ModeIcon forMode(String mode) { return find(mode).orElse(OVERALL); }
}
