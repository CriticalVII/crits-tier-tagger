package za.co.afritiers.tagger.network;

import za.co.afritiers.tagger.provider.*;
import za.co.afritiers.tagger.ranking.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static za.co.afritiers.tagger.network.RankingRepository.Phase;
import static za.co.afritiers.tagger.network.RankingRepository.State;

/** A bounded, throttled username cache. Rendering never initiates or waits for HTTP requests. */
public final class ProfileRepository implements TierRepository {
    private record Entry(PlayerProfileParser.Profile profile, LookupStatus status, Instant expires, Instant updated) { }
    private final PlayerProfileSource source;
    private final ExecutorService worker;
    private final Clock clock;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
    private final LinkedHashSet<String> queue = new LinkedHashSet<>();
    private List<String> modes;
    private volatile State state;
    private Future<?> request;
    private boolean connected, enabled = true, closed, running;
    private long generation;
    private int failures;
    private Instant allowedAt = Instant.MIN, retryAt = Instant.MIN, catalogueAt = Instant.MIN;
    private Instant manualAt = Instant.MIN, worldAllowedAt = Instant.MIN, refreshAt, pruneAt = Instant.MIN;

    public ProfileRepository(PlayerProfileSource source) {
        this(source, Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("crits-" + source.provider().id()).factory()), Clock.systemUTC());
    }
    public ProfileRepository(PlayerProfileSource source, ExecutorService worker, Clock clock) {
        this.source = source; this.worker = worker; this.clock = clock;
        modes = source.provider().modes();
        state = new State(Phase.DISCONNECTED, empty(), null, null, null, "Join a world to load " + provider().label());
    }
    @Override public TierlistProvider provider() { return source.provider(); }
    @Override public State state() { return state; }

    @Override public synchronized void connect() {
        if (closed) return;
        if (connected) { worldChanged(); return; }
        connected = true;
        publish(enabled ? Phase.READY : Phase.DISABLED, "Ready for " + provider().label() + " player lookups");
        tick();
    }
    @Override public synchronized void disconnect() {
        connected = false; resetPending();
        state = new State(Phase.DISCONNECTED, empty(), state.lastAttempt(), state.lastSuccess(), null, "Disconnected");
    }
    @Override public synchronized void setEnabled(boolean value) {
        if (enabled == value || closed) return;
        enabled = value; resetPending();
        if (connected && enabled) { publish(Phase.READY, "Ready for player lookups"); tick(); }
        else state = new State(enabled ? Phase.DISCONNECTED : Phase.DISABLED, empty(), state.lastAttempt(), state.lastSuccess(), null,
                enabled ? "Join a world to load tiers" : "Tier tags are disabled");
    }

    @Override public synchronized void requestPlayer(String username, boolean priority) {
        if (closed || !connected || !enabled || username == null || !username.matches("[A-Za-z0-9_]{1,16}")) return;
        String key = Tier.normalizeUsername(username);
        if (priority && queue.remove(key)) queue.addFirst(key);
        Entry previous = entries.get(key);
        if (previous != null && (previous.status == LookupStatus.LOADING || clock.instant().isBefore(previous.expires))) return;
        if (queue.size() >= 128) return;
        if (entries.size() >= 512 && previous == null) {
            var evict = entries.entrySet().stream().filter(entry -> entry.getValue().status != LookupStatus.LOADING).findFirst();
            if (evict.isEmpty()) return;
            entries.remove(evict.get().getKey());
        }
        entries.put(key, new Entry(previous == null ? null : previous.profile, LookupStatus.LOADING, Instant.MAX,
                previous == null ? clock.instant() : previous.updated));
        if (priority) queue.addFirst(key); else queue.add(key);
    }
    @Override public synchronized LookupStatus lookupStatus(String username) {
        Entry entry = entries.get(Tier.normalizeUsername(username));
        if (entry != null) return entry.status;
        return state.phase() == Phase.ERROR ? LookupStatus.ERROR : LookupStatus.LOADING;
    }

    @Override public synchronized void worldChanged() {
        if (closed || !connected || !enabled) return;
        Instant next = later(clock.instant(), later(worldAllowedAt, retryAt));
        if (refreshAt == null || next.isBefore(refreshAt)) refreshAt = next;
    }
    @Override public synchronized String refresh() {
        if (closed || !connected) return "Open this tierlist in a world or Profiles before refreshing";
        if (!enabled) return "Enable tier tags first";
        Instant next = later(manualAt, retryAt);
        if (clock.instant().isBefore(next)) return "Refresh available in " + Math.max(1, Duration.between(clock.instant(), next).toSeconds()) + "s";
        refreshAt = clock.instant(); manualAt = refreshAt.plusSeconds(30);
        return "Refreshing " + provider().label();
    }

    @Override public synchronized void tick() {
        if (closed || !enabled || !connected) return;
        Instant now = clock.instant();
        if (refreshAt != null && !now.isBefore(refreshAt)) {
            entries.replaceAll((key, entry) -> entry.status == LookupStatus.LOADING ? entry
                    : new Entry(entry.profile, entry.status, Instant.MIN, entry.updated));
            catalogueAt = Instant.MIN; refreshAt = null; worldAllowedAt = now.plusSeconds(30);
        }
        if (!now.isBefore(pruneAt)) {
            boolean removed = entries.entrySet().removeIf(entry -> entry.getValue().status != LookupStatus.LOADING
                    && entry.getValue().updated.isBefore(now.minusSeconds(600)));
            if (removed) publish(state.phase(), state.message());
            pruneAt = now.plusSeconds(1);
        }
        if (running || now.isBefore(allowedAt) || now.isBefore(retryAt)) return;
        if (!now.isBefore(catalogueAt)) start(null);
        else if (!queue.isEmpty()) { String key = queue.iterator().next(); queue.remove(key); start(key); }
    }

    private void start(String username) {
        long token = generation;
        running = true;
        Instant attempted = clock.instant();
        // At most 80 profile requests/minute per service, also respecting service Retry-After responses.
        allowedAt = attempted.plusMillis(750);
        state = new State(username == null && entries.isEmpty() ? Phase.LOADING : state.phase(), state.rankings(), attempted,
                state.lastSuccess(), allowedAt, username == null ? "Loading " + provider().label() + " modes" : "Checking " + username);
        request = worker.submit(() -> {
            try {
                List<String> fetchedModes = username == null ? source.fetchModes() : null;
                var profile = username == null ? Optional.<PlayerProfileParser.Profile>empty() : source.fetchPlayer(username);
                synchronized (this) {
                    if (closed || !connected || token != generation) return;
                    running = false; failures = 0; retryAt = Instant.MIN;
                    Instant now = clock.instant();
                    if (username == null) {
                        modes = List.copyOf(fetchedModes); catalogueAt = now.plusSeconds(300); manualAt = now.plusSeconds(30);
                        worldAllowedAt = later(worldAllowedAt, now.plusSeconds(30));
                    }
                    else entries.put(username, new Entry(profile.orElse(null), profile.isPresent() ? LookupStatus.FOUND : LookupStatus.NOT_FOUND,
                            now.plusSeconds(profile.isPresent() ? 300 : 60), now));
                    state = new State(Phase.READY, state.rankings(), state.lastAttempt(), now, allowedAt, "Ready");
                    publish(Phase.READY, provider().label() + " · " + countProfiles() + " cached profiles");
                }
            } catch (Exception exception) {
                synchronized (this) {
                    if (closed || !connected || token != generation) return;
                    running = false; failures = Math.min(6, failures + 1);
                    retryAt = clock.instant().plusSeconds(Math.min(900, 30L << (failures - 1)));
                    if (exception instanceof RankingSource.ApiException api && api.retryAfter() != null) retryAt = later(retryAt, api.retryAfter());
                    if (username != null) entries.put(username, new Entry(null, LookupStatus.ERROR, retryAt, clock.instant()));
                    publish(Phase.ERROR, exception instanceof RankingSource.ApiException ? exception.getMessage()
                            : provider().label() + " data unavailable; retrying automatically");
                }
            }
        });
    }

    private int countProfiles() { return (int) entries.values().stream().filter(entry -> entry.profile != null).count(); }
    private Rankings empty() { return new Rankings(Map.of(), modes, null, false, 0, 0, Map.of(), Map.of()); }
    private void publish(Phase phase, String message) {
        Map<String, List<Tier>> players = new HashMap<>();
        Map<String, PlayerStats> stats = new HashMap<>();
        entries.forEach((key, entry) -> {
            if (entry.profile != null && entry.status != LookupStatus.ERROR) {
                players.put(key, entry.profile.tiers()); stats.put(key, entry.profile.stats());
            }
        });
        var snapshot = new Rankings(players, modes, state.lastSuccess(), false, 0, 0, Map.of(), stats);
        state = new State(phase, snapshot, state.lastAttempt(), state.lastSuccess(), phase == Phase.ERROR ? retryAt : allowedAt, message);
    }
    private void resetPending() {
        generation++;
        if (request != null) request.cancel(true);
        running = false; queue.clear(); refreshAt = null;
        entries.entrySet().removeIf(entry -> entry.getValue().status == LookupStatus.LOADING && entry.getValue().profile == null);
        entries.replaceAll((key, entry) -> entry.status != LookupStatus.LOADING ? entry
                : new Entry(entry.profile, LookupStatus.FOUND, Instant.MIN, entry.updated));
    }
    private static Instant later(Instant left, Instant right) { return left.isAfter(right) ? left : right; }
    @Override public synchronized void close() {
        closed = true; connected = false; resetPending(); worker.shutdownNow(); source.close(); entries.clear();
        state = new State(Phase.DISCONNECTED, empty(), null, null, null, "Closed");
    }
}
