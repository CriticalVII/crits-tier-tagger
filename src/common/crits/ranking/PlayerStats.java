package crits.ranking;




/** Public profile fields and the overall position in one normalized API snapshot. */
public record PlayerStats(String name, String region, int points, int position, String combatTitle) {
    public PlayerStats(String name, String region, int points, int position) {
        this(name, region, points, position, afriTitle(points));
    }
    private static String afriTitle(int points) {
        if (points >= 180) return "Combat Master";
        if (points >= 80) return "Combat Ace";
        if (points >= 40) return "Combat Specialist";
        if (points >= 20) return "Combat Cadet";
        if (points >= 10) return "Combat Novice";
        return "Rookie";
    }

}
