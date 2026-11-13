package za.co.afritiers.tagger.network;

import za.co.afritiers.tagger.provider.PlayerIdentity;
import za.co.afritiers.tagger.ranking.Tier;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Shared official-account cache for UUID-only tierlists. No server UUID or tierlist name is trusted. */
public final class AccountIdentityResolver implements AutoCloseable {
    private record Entry(String uuid, TierRepository.LookupStatus status, Instant expires) { }
    private final ProfileLookup source;
    private final Clock clock;
    private final ScheduledExecutorService worker;
    private final LinkedHashMap<String, Entry> cache = new LinkedHashMap<>();
    private final LinkedHashSet<String> queue = new LinkedHashSet<>();
    private String inFlight;
    private boolean active, closed;
    private long generation;
    private Instant retryAt = Instant.MIN;

    public AccountIdentityResolver(ProfileLookup source) { this(source, Clock.systemUTC()); }
    public AccountIdentityResolver(ProfileLookup source, Clock clock) {
        this(source, clock, Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("crits-account-uuids").factory()));
    }
    public AccountIdentityResolver(ProfileLookup source, Clock clock, ScheduledExecutorService worker) {
        this.source = source; this.clock = clock; this.worker = worker;
        worker.scheduleWithFixedDelay(this::processNext, 0, 1000, TimeUnit.MILLISECONDS);
    }
    public synchronized void setActive(boolean value) {
        if (active == value || closed) return;
        active = value; generation++;
        if (!active) queue.clear();
    }
    public synchronized void request(String username, boolean priority) {
        if (!active || closed || username == null || !username.matches("[A-Za-z0-9_]{1,16}")) return;
        String key = Tier.normalizeUsername(username);
        if (entry(key) != null || key.equals(inFlight)) return;
        if (priority && queue.remove(key)) queue.addFirst(key);
        if (queue.contains(key)) return;
        if (queue.size() >= 128) { if (!priority) return; queue.removeLast(); }
        if (priority) queue.addFirst(key); else queue.add(key);
    }
    public synchronized Optional<String> profileId(String username) {
        var entry = entry(Tier.normalizeUsername(username));
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.uuid);
    }
    public synchronized TierRepository.LookupStatus lookupStatus(String username) {
        var entry = entry(Tier.normalizeUsername(username));
        return entry != null ? entry.status : clock.instant().isBefore(retryAt) ? TierRepository.LookupStatus.ERROR : TierRepository.LookupStatus.LOADING;
    }
    private Entry entry(String key) {
        var value = cache.get(key);
        if (value != null && !clock.instant().isBefore(value.expires)) { cache.remove(key); return null; }
        return value;
    }
    private void processNext() {
        String name; long token;
        synchronized (this) {
            if (closed || !active || queue.isEmpty() || clock.instant().isBefore(retryAt)) return;
            name = queue.iterator().next(); queue.remove(name); inFlight = name; token = generation;
        }
        String uuid = null;
        Exception failure = null;
        try {
            var profile = source.lookup(name);
            if (profile.isPresent()) {
                uuid = PlayerIdentity.uuid(profile.get().id());
                if (uuid == null || !profile.get().name().equalsIgnoreCase(name)) throw new IllegalArgumentException("Account identity did not match");
            }
        } catch (Exception exception) { failure = exception; }
        synchronized (this) {
            inFlight = null;
            Instant now = clock.instant();
            // Service cooldowns also apply when the user leaves the screen during a request.
            if (failure != null) {
                Instant next = now.plusSeconds(30);
                if (failure instanceof RankingSource.ApiException api && api.retryAfter() != null && api.retryAfter().isAfter(next)) next = api.retryAfter();
                if (next.isAfter(retryAt)) retryAt = next;
            }
            if (closed || !active || token != generation) return;
            var status = failure != null ? TierRepository.LookupStatus.ERROR : uuid == null ? TierRepository.LookupStatus.NOT_FOUND : TierRepository.LookupStatus.FOUND;
            Instant expires = now.plusSeconds(failure != null ? 30 : uuid == null ? 60 : 300);
            if (failure != null) {
                expires = retryAt; uuid = null;
            } else retryAt = Instant.MIN;
            if (cache.size() >= 512) cache.remove(cache.keySet().iterator().next());
            cache.put(name, new Entry(uuid, status, expires));
        }
    }
    @Override public synchronized void close() {
        closed = true; active = false; generation++; queue.clear(); cache.clear();
        worker.shutdownNow(); source.close();
    }
}
