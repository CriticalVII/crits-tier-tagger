package crits.config;

import crits.provider.TierlistProvider;
import crits.waitlist.Waitlists;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TaggerConfig {
    public AppearanceConfig appearance = new AppearanceConfig();
    /** HT/LT tier IDs and retired -> RGB; legacy tier numbers remain fallbacks. */
    public java.util.Map<String, String> tierColours = new java.util.LinkedHashMap<>();
    public boolean enabled = true;
    public boolean nameplates = true;
    public boolean tabList = true;
    public boolean includeRetired = true;
    public boolean waitlistNotifications = true;
    public boolean waitlistSound = true;
    /** Empty is opt-in: no background polling or alerts until a mode is selected. */
    public java.util.Set<String> waitlistModes = new java.util.LinkedHashSet<>();
    public String mode = "strongest";
    public String tierlist = "afritiers";
    /** Empty follows the in-game provider until the user makes an independent profile selection. */
    public String profileTierlist = "";
    public java.util.Map<String, String> tierlistModes = new java.util.LinkedHashMap<>();
    public java.util.Map<String, java.util.Map<String, String>> tierlistAliases = new java.util.LinkedHashMap<>();
    /** Empty uses tier colours; otherwise an explicit six-digit RGB value. */
    public String customColour = "";
    /** Explicit current username -> API username aliases, also usable for cracked players. */
    public java.util.Map<String, String> usernameAliases = new java.util.LinkedHashMap<>();

    public TaggerConfig copy() {
        TaggerConfig copy = new TaggerConfig();
        copy.appearance = appearance == null ? new AppearanceConfig() : appearance.copy();
        copy.tierColours = tierColours == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(tierColours);
        copy.enabled = enabled;
        copy.nameplates = nameplates;
        copy.tabList = tabList;
        copy.includeRetired = includeRetired;
        copy.waitlistNotifications = waitlistNotifications;
        copy.waitlistSound = waitlistSound;
        copy.waitlistModes = waitlistModes == null ? new java.util.LinkedHashSet<>() : new java.util.LinkedHashSet<>(waitlistModes);
        copy.mode = mode;
        copy.tierlist = tierlist;
        copy.profileTierlist = profileTierlist;
        copy.tierlistModes = tierlistModes == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(tierlistModes);
        copy.tierlistAliases = new java.util.LinkedHashMap<>();
        if (tierlistAliases != null) tierlistAliases.forEach((provider, aliases) -> copy.tierlistAliases.put(provider,
                aliases == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(aliases)));
        copy.customColour = customColour;
        copy.usernameAliases = usernameAliases == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(usernameAliases);
        return copy;
    }

    public void validate() {
        if (appearance == null) appearance = new AppearanceConfig();
        appearance.validate();
        if (tierColours == null) tierColours = new java.util.LinkedHashMap<>();
        if (tierColours.size() > 16) throw new IllegalArgumentException("Too many tier colour entries");
        var cleanColours = new java.util.LinkedHashMap<String, String>();
        tierColours.forEach((key, value) -> {
            if (key == null || !key.matches("[1-5]|[HL]T[1-5]|retired")) throw new IllegalArgumentException("Invalid tier colour number");
            String clean = AppearanceConfig.hex(value, true);
            if (!clean.isEmpty()) cleanColours.put(key, clean);
        });
        tierColours = cleanColours;
        tierlist = TierlistProvider.from(tierlist).id();
        if (profileTierlist == null || TierlistProvider.find(profileTierlist).isEmpty()) profileTierlist = "";
        if (tierlistModes == null) tierlistModes = new java.util.LinkedHashMap<>();
        if (tierlistModes.size() > 4) throw new IllegalArgumentException("Too many provider mode preferences");
        for (var entry : tierlistModes.entrySet()) {
            if (TierlistProvider.find(entry.getKey()).isEmpty()
                    || entry.getValue() == null || !entry.getValue().matches("[a-z0-9_-]{1,32}"))
                throw new IllegalArgumentException("Invalid provider mode preference");
        }
        if (tierlistAliases == null) tierlistAliases = new java.util.LinkedHashMap<>();
        if (tierlistAliases.size() > 3) throw new IllegalArgumentException("Too many provider alias groups");
        var normalizedGroups = new java.util.LinkedHashMap<String, java.util.Map<String, String>>();
        for (var entry : tierlistAliases.entrySet()) {
            if (TierlistProvider.find(entry.getKey()).isEmpty() || entry.getKey().equals("afritiers"))
                throw new IllegalArgumentException("Invalid alias provider");
            var group = new TaggerConfig();
            group.usernameAliases = entry.getValue();
            group.normalizeUsernameAliases();
            normalizedGroups.put(entry.getKey(), group.usernameAliases);
        }
        tierlistAliases = normalizedGroups;
        if (waitlistModes == null) waitlistModes = new java.util.LinkedHashSet<>();
        if (waitlistModes.size() > 128) throw new IllegalArgumentException("At most 128 waitlist subscriptions are supported");
        var normalizedModes = new java.util.LinkedHashSet<String>();
        for (String mode : waitlistModes) normalizedModes.add(Waitlists.modeKey(mode));
        waitlistModes = normalizedModes;
        normalizeUsernameAliases();
        if (mode == null || !Pattern.matches("[a-z0-9_-]{1,32}", mode)) mode = "strongest";
        if (customColour == null) customColour = "";
        customColour = customColour.replace("#", "").trim().toUpperCase(Locale.ROOT);
        if (!customColour.isEmpty() && !Pattern.matches("[0-9A-F]{6}", customColour)) {
            throw new IllegalArgumentException("Colour must be six hexadecimal digits or blank");
        }
    }

    private void normalizeUsernameAliases() {
        if (usernameAliases == null) usernameAliases = new java.util.LinkedHashMap<>();
        if (usernameAliases.size() > 128) throw new IllegalArgumentException("At most 128 username aliases are supported");
        var normalizedAliases = new java.util.LinkedHashMap<String, String>();
        for (var entry : usernameAliases.entrySet()) {
            String current = entry.getKey(), api = entry.getValue();
            if (current == null || api == null || !current.matches("[A-Za-z0-9_]{1,16}") || !api.matches("[A-Za-z0-9_]{1,16}")) {
                throw new IllegalArgumentException("Aliases require valid Minecraft usernames");
            }
            String previous = normalizedAliases.putIfAbsent(current.toLowerCase(Locale.ROOT), api.toLowerCase(Locale.ROOT));
            if (previous != null && !previous.equalsIgnoreCase(api)) throw new IllegalArgumentException("Conflicting aliases for one username");
        }
        usernameAliases = normalizedAliases;
    }

    public java.util.Map<String, String> aliases(TierlistProvider provider) {
        return provider == TierlistProvider.AFRITIERS ? usernameAliases
                : tierlistAliases.getOrDefault(provider.id(), java.util.Map.of());
    }
    public java.util.Map<String, String> editAliases(TierlistProvider provider) {
        return provider == TierlistProvider.AFRITIERS ? usernameAliases
                : tierlistAliases.computeIfAbsent(provider.id(), ignored -> new java.util.LinkedHashMap<>());
    }

    public int colour(String level) {
        if (!customColour.isEmpty()) return Integer.parseInt(customColour, 16);
        return tierColour(level);
    }

    /** Individual tier palette; independent of the optional all-tags override. */
    public int tierColour(String level) {
        if (level.startsWith("R") && tierColours.containsKey("retired")) return Integer.parseInt(tierColours.get("retired"), 16);
        String active = level.startsWith("R") ? level.substring(1) : level;
        if (tierColours.containsKey(active)) return Integer.parseInt(tierColours.get(active), 16);
        String number = level.substring(level.length() - 1);
        if (tierColours.containsKey(number)) return Integer.parseInt(tierColours.get(number), 16);
        return switch (number.charAt(0)) {
            case '1' -> 0xFFD700;
            case '2' -> 0xC0C0C0;
            case '3' -> 0xCD7F32;
            default -> 0xB58AE8;
        };
    }
}
