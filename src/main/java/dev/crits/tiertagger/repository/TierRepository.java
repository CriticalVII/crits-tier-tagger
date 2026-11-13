package dev.crits.tiertagger.repository;

import dev.crits.tiertagger.provider.TierlistProvider;
import dev.crits.tiertagger.ranking.Tier;

/** Bulk and per-player services expose the same immutable rendering snapshot. */
public interface TierRepository extends AutoCloseable {
    enum LookupStatus { FOUND, NOT_FOUND, LOADING, ERROR }
    TierlistProvider provider();
    RankingRepository.State state();
    void connect();
    void disconnect();
    void setEnabled(boolean enabled);
    void tick();
    void worldChanged();
    String refresh();
    /** Bulk services fetch all usernames; per-player services accept only verified account UUIDs. */
    default void requestPlayer(String username, boolean priority) { }
    default LookupStatus lookupStatus(String username) {
        var state = state();
        if (state.rankings().players().containsKey(Tier.normalizeUsername(username))) return LookupStatus.FOUND;
        return switch (state.phase()) {
            case ERROR -> LookupStatus.ERROR;
            case LOADING -> LookupStatus.LOADING;
            default -> LookupStatus.NOT_FOUND;
        };
    }
    @Override void close();
}
