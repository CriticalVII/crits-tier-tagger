package dev.crits.tiertagger.network.http;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import dev.crits.tiertagger.network.source.PlayerProfileSource;
import dev.crits.tiertagger.network.source.RankingSource;
import dev.crits.tiertagger.provider.PlayerIdentity;
import dev.crits.tiertagger.provider.PlayerProfileParser;
import dev.crits.tiertagger.provider.TierlistProvider;
import java.io.StringReader;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** UUID-only tierlist endpoints; IDs are obtained from the official Minecraft account resolver. */
public final class HttpPlayerProfileSource implements PlayerProfileSource {
    public static final int MAX_BYTES = 256 * 1024;
    private final TierlistProvider provider;
    private final URI base;
    private final Duration timeout;
    private final Clock clock;
    private final HttpClient client;
    public HttpPlayerProfileSource(TierlistProvider provider) {
        this(provider, URI.create(provider.website()), Duration.ofSeconds(10), Clock.systemUTC());
    }
    public HttpPlayerProfileSource(TierlistProvider provider, URI base, Duration timeout, Clock clock) {
        if (provider == TierlistProvider.AFRITIERS) throw new IllegalArgumentException("AfriTiers uses the bulk rankings endpoint");
        this.provider = provider; this.base = base; this.timeout = timeout; this.clock = clock;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    @Override public TierlistProvider provider() { return provider; }
    @Override public List<String> fetchModes() throws Exception {
        // PvPTiers exposes profiles at its legacy endpoint; its supported combat modes are bundled.
        if (provider == TierlistProvider.PVPTIERS) return provider.modes();
        JsonElement value = get("/api/v2/mode/list", false).orElseThrow();
        if (!value.isJsonObject() || value.getAsJsonObject().size() > 128) throw new IllegalArgumentException("Invalid mode catalogue");
        var modes = value.getAsJsonObject().entrySet().stream().filter(entry -> entry.getKey().matches("[a-z0-9_-]{1,32}")
                && entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().has("title")
                && entry.getValue().getAsJsonObject().get("title").isJsonPrimitive()
                && entry.getValue().getAsJsonObject().getAsJsonPrimitive("title").isString())
                .map(java.util.Map.Entry::getKey).sorted().toList();
        if (modes.isEmpty()) throw new IllegalArgumentException("Mode catalogue is empty");
        return modes;
    }
    @Override public Optional<PlayerProfileParser.Profile> fetchPlayer(String uuid) throws Exception {
        String key = PlayerIdentity.uuid(uuid);
        if (key == null) throw new IllegalArgumentException("A verified Minecraft account UUID is required");
        String route = provider == TierlistProvider.PVPTIERS ? "/api/profile/" + key
                : "/api/v2/profile/" + PlayerIdentity.dashed(key);
        return get(route, true).map(json -> PlayerProfileParser.parse(json, key));
    }
    private Optional<JsonElement> get(String route, boolean missingAllowed) throws Exception {
        var request = HttpRequest.newBuilder(base.resolve(route)).timeout(timeout).header("Accept", "application/json")
                .header("User-Agent", "Crits-Tier-Tagger/2.0 (Minecraft 26.2)").GET().build();
        var pending = client.sendAsync(request, info -> new HttpRankingSource.LimitedSubscriber(
                info.headers().firstValueAsLong("Content-Length").orElse(0) > MAX_BYTES ? 0 : MAX_BYTES));
        try {
            var response = pending.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (response.statusCode() == 404 && missingAllowed) return Optional.empty();
            if (response.statusCode() != 200) {
                Instant retry = response.statusCode() == 429 || response.statusCode() == 503
                        ? HttpRankingSource.parseRetryAfter(response.headers().firstValue("Retry-After").orElse(""), clock.instant()) : null;
                throw new RankingSource.ApiException(provider.label() + " returned HTTP " + response.statusCode(), retry);
            }
            if (!response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT).contains("application/json"))
                throw new RankingSource.ApiException(provider.label() + " did not return JSON", null);
            var reader = new JsonReader(new StringReader(new String(response.body(), StandardCharsets.UTF_8)));
            reader.setStrictness(Strictness.STRICT);
            JsonElement value = JsonParser.parseReader(reader);
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Invalid profile JSON");
            return Optional.of(value);
        } catch (TimeoutException exception) {
            pending.cancel(true); throw new RankingSource.ApiException(provider.label() + " request timed out", null);
        } catch (InterruptedException exception) {
            pending.cancel(true); Thread.currentThread().interrupt(); throw exception;
        } catch (ExecutionException exception) {
            throw new RankingSource.ApiException(provider.label() + " connection failed or response exceeded 256 KiB", null);
        }
    }
    @Override public void close() { client.shutdownNow(); }
}
