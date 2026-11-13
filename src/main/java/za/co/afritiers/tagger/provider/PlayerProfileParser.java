package za.co.afritiers.tagger.provider;

import com.google.gson.*;
import za.co.afritiers.tagger.ranking.*;
import java.util.*;

/** Shared MCTiers-v2 profile shape; points and positions always come from the selected service. */
public final class PlayerProfileParser {
    private PlayerProfileParser() { }
    public record Profile(String key, List<Tier> tiers, PlayerStats stats) {
        public Profile { tiers = List.copyOf(tiers); }
    }
    public static Profile parse(JsonElement json, String requested) {
        if (!requested.matches("[A-Za-z0-9_]{1,16}") || json == null || !json.isJsonObject()) throw new IllegalArgumentException("Invalid player profile");
        JsonObject root = json.getAsJsonObject();
        String name = text(root, "name");
        if (!name.matches("[A-Za-z0-9_]{1,16}") || !name.equalsIgnoreCase(requested))
            throw new IllegalArgumentException("The API returned a different username");
        if (!root.has("rankings") || !root.get("rankings").isJsonObject()) throw new IllegalArgumentException("Profile rankings are missing");
        JsonObject rankings = root.getAsJsonObject("rankings");
        if (rankings.size() > 128) throw new IllegalArgumentException("Too many rankings");
        List<Tier> tiers = new ArrayList<>();
        for (var entry : rankings.entrySet()) {
            if (!entry.getKey().matches("[a-z0-9_-]{1,32}") || !entry.getValue().isJsonObject()) continue;
            var ranking = entry.getValue().getAsJsonObject();
            int number = integer(ranking, "tier", 1, 5, -1), pos = integer(ranking, "pos", 0, 1, -1);
            boolean retired = !ranking.has("retired") || !ranking.get("retired").isJsonPrimitive()
                    || !ranking.getAsJsonPrimitive("retired").isBoolean() || ranking.get("retired").getAsBoolean();
            if (retired) {
                int peak = integer(ranking, "peak_tier", 1, 5, -1), peakPos = integer(ranking, "peak_pos", 0, 1, -1);
                if (peak != -1 && peakPos != -1) { number = peak; pos = peakPos; }
            }
            if (number != -1 && pos != -1) tiers.add(new Tier(entry.getKey(), (pos == 0 ? "HT" : "LT") + number, retired));
        }
        String region = text(root, "region").toUpperCase(Locale.ROOT);
        if (!region.matches("[A-Z]{2,3}|\\?\\?")) region = "Unknown";
        int points = integer(root, "points", 0, Integer.MAX_VALUE, -1);
        int position = integer(root, "overall", 1, Integer.MAX_VALUE, 0);
        return new Profile(Tier.normalizeUsername(name), tiers, new PlayerStats(name, region, points, position, ""));
    }
    private static String text(JsonObject root, String key) {
        var value = root.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : "";
    }
    private static int integer(JsonObject root, String key, int min, int max, int fallback) {
        var value = root.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return fallback;
        try { int result = value.getAsBigDecimal().intValueExact(); return result >= min && result <= max ? result : fallback; }
        catch (ArithmeticException | NumberFormatException exception) { return fallback; }
    }
}
