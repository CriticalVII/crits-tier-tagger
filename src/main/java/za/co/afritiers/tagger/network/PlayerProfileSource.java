package za.co.afritiers.tagger.network;

import za.co.afritiers.tagger.provider.*;
import java.util.*;

public interface PlayerProfileSource extends AutoCloseable {
    TierlistProvider provider();
    List<String> fetchModes() throws Exception;
    Optional<PlayerProfileParser.Profile> fetchPlayer(String username) throws Exception;
    @Override void close();
}
