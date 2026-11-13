package za.co.afritiers.tagger.provider;

import java.util.*;

/** Stable configuration IDs are separate from each service's public branding and API mode IDs. */
public enum TierlistProvider {
    AFRITIERS("afritiers", "AfriTiers", "https://afritiers.co.za", "Username rankings · AfriTiers waitlists",
            "axe", "bed", "cart", "creeper", "diavanilla", "diapot", "diasmp", "mace", "nethop", "ogv", "smp", "spear", "speed", "sword", "uhc", "vanilla"),
    MCTIERS("mctiers", "MCTiers", "https://mctiers.net", "Official points and overall leaderboard",
            "axe", "pot", "mace", "uhc", "smp", "sword", "vanilla", "nethop"),
    PVPTIERS("pvptiers", "PvPTiers", "https://pvptiers.com", "UUID profiles · eight combat modes",
            "crystal", "sword", "uhc", "pot", "neth_pot", "smp", "axe", "mace"),
    SUBTIERS("subtiers", "SubTiers", "https://subtiers.net", "Specialist modes · official overall rank",
            "trident", "bed", "minecart", "creeper", "dia_smp", "dia_crystal", "elytra", "manhunt", "og_vanilla", "speed", "bow", "debuff");

    private final String id, label, website, description;
    private final List<String> modes;
    TierlistProvider(String id, String label, String website, String description, String... modes) {
        this.id = id; this.label = label; this.website = website; this.description = description; this.modes = List.of(modes);
    }
    public String id() { return id; }
    public String label() { return label; }
    public String website() { return website; }
    public String description() { return description; }
    public List<String> modes() { return modes; }
    public static Optional<TierlistProvider> find(String id) {
        return Arrays.stream(values()).filter(provider -> provider.id.equals(id)).findFirst();
    }
    public static TierlistProvider from(String id) { return find(id).orElse(AFRITIERS); }
    public String modeLabel(String mode) {
        return switch (mode) {
            case "strongest" -> "Strongest tier";
            case "pot" -> "Pot";
            case "crystal" -> "Crystal";
            case "minecart" -> "Minecart";
            case "dia_crystal" -> "Diamond Crystal";
            case "neth_pot" -> "Netherite Pot";
            case "nethop" -> this == MCTIERS ? "NethPot" : "NethOP";
            default -> za.co.afritiers.tagger.ranking.Tier.modeLabel(mode);
        };
    }
}
