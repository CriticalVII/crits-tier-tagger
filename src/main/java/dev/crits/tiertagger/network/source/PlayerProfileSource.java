package dev.crits.tiertagger.network.source;

import dev.crits.tiertagger.provider.*;
import dev.crits.tiertagger.provider.PlayerProfileParser;
import dev.crits.tiertagger.provider.TierlistProvider;
import java.util.*;

public interface PlayerProfileSource extends AutoCloseable {
    TierlistProvider provider();
    List<String> fetchModes() throws Exception;
    /** Only canonical or dashed account UUIDs are accepted; no tierlist name lookup fallback. */
    Optional<PlayerProfileParser.Profile> fetchPlayer(String uuid) throws Exception;
    @Override void close();
}
