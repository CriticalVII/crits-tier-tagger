package crits;

import com.mojang.blaze3d.platform.InputConstants;
import crits.command.TierTaggerCommands;
import crits.gui.TaggerSettingsScreen;
import crits.render.ModeIcons;
import crits.config.ConfigStore;
import crits.config.TaggerConfig;
import crits.identity.AccountIdentityResolver;
import crits.identity.PaidNameResolver;
import crits.network.http.HttpPlayerProfileSource;
import crits.network.http.HttpProfileLookup;
import crits.network.http.HttpQueueSource;
import crits.network.http.HttpRankingSource;
import crits.provider.TierlistProvider;
import crits.ranking.Rankings;
import crits.ranking.Tier;
import crits.repository.ProfileRepository;
import crits.repository.QueueRepository;
import crits.repository.RankingRepository;
import crits.repository.TierRepository;
import java.io.IOException;
import java.util.LinkedHashSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CritsTierTaggerClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("afritiers_tagger");
    private static CritsTierTaggerClient instance;
    private ConfigStore store;
    private volatile TaggerConfig config;
    private TierRepository repository;
    private PaidNameResolver paidNames;
    private AccountIdentityResolver identities;
    private TierRepository browser;
    private TierlistProvider selectedProvider = TierlistProvider.AFRITIERS;
    private TierlistProvider profileProvider = TierlistProvider.AFRITIERS;
    private final java.util.Map<TierlistProvider, TierRepository> gameRepositories = new java.util.EnumMap<>(TierlistProvider.class);
    private final java.util.Map<TierlistProvider, TierRepository> profileRepositories = new java.util.EnumMap<>(TierlistProvider.class);
    private boolean browsingPlayers;
    private QueueRepository waitlists;
    private boolean browsingWaitlists;
    private net.minecraft.client.multiplayer.ClientLevel observedWorld;
    private net.minecraft.client.player.LocalPlayer observedPlayer;
    private boolean worldRefreshRequested;
    private final java.util.Map<TierRepository, LinkedHashSet<String>> pendingProfiles = new java.util.HashMap<>();

    public static CritsTierTaggerClient instance() { return instance; }
    public TaggerConfig config() { return config.copy(); }
    TaggerConfig activeConfig() { return config; }
    public TierRepository repository() { return repository; }
    /** API sessions belong to the menu as well as worlds; opening settings must be enough to sync. */
    public void loadSelectedTierlist() {
        if (config.enabled && repository.state().phase() == RankingRepository.Phase.DISCONNECTED) repository.connect();
    }
    public TierlistProvider selectedProvider() { return selectedProvider; }
    public TierlistProvider defaultProfileProvider() {
        return config.profileTierlist.isEmpty() ? selectedProvider : TierlistProvider.from(config.profileTierlist);
    }
    public PaidNameResolver paidNames() { return paidNames; }
    public QueueRepository waitlists() { return waitlists; }
    public void browseWaitlists(boolean browsing) { browsingWaitlists = browsing; }
    public String configWarning() { return store.warning(); }
    public void requestWorldRefresh() { worldRefreshRequested = true; }

    @Override public void onInitializeClient() {
        instance = this;
        store = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("afritiers_tagger.json"));
        config = store.load();
        selectedProvider = TierlistProvider.from(config.tierlist);
        repository = createRepository(selectedProvider);
        browser = new RankingRepository(new HttpRankingSource());
        paidNames = new PaidNameResolver(new HttpProfileLookup());
        identities = new AccountIdentityResolver(new HttpProfileLookup());
        waitlists = new QueueRepository(new HttpQueueSource());
        repository.setEnabled(config.enabled);
        KeyMapping openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.afritiers_tagger.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("afritiers_tagger", "controls"))));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof net.minecraft.client.gui.screens.TitleScreen)
                ScreenKeyboardEvents.allowKeyPress(screen).register((current, event) -> {
                    if (!openSettings.matches(event)) return true;
                    client.schedule(() -> client.gui.setScreen(new TaggerSettingsScreen(current)));
                    return false;
                });
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            observedWorld = null;
            observedPlayer = null;
            worldRefreshRequested = false;
            paidNames.resetSession();
            repository.connect();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            observedWorld = null; observedPlayer = null; worldRefreshRequested = false;
            pendingProfiles.clear();
            repository.disconnect(); paidNames.setActive(false);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            var services = new java.util.HashSet<TierRepository>(gameRepositories.values());
            services.addAll(profileRepositories.values()); services.add(repository); services.add(browser);
            services.forEach(TierRepository::close); paidNames.close(); identities.close(); waitlists.close();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            identities.setActive(config.enabled && selectedProvider != TierlistProvider.AFRITIERS
                    || browsingPlayers && profileProvider != TierlistProvider.AFRITIERS);
            updateWorldSession(client);
            repository.tick();
            if (browsingPlayers && playerSearchRepository() != repository) playerSearchRepository().tick();
            updatePendingProfiles();
            updateVisibleProfiles(client);
            updatePaidNames(client);
            var watched = config.waitlistNotifications ? config.waitlistModes : java.util.Set.<String>of();
            var notifications = waitlists.tick(client.player != null && client.getConnection() != null, browsingWaitlists, watched);
            for (var queue : notifications) {
                Component message = Component.literal("[Crit's · AfriTiers] ").withColor(0xC8ACF2)
                        .append(ModeIcons.component(queue.mode()))
                        .append(Component.literal(" " + Tier.modeLabel(queue.mode()) + " waitlist is open!").withColor(0xFFD700));
                if (queue.joinUrl() != null) message = message.copy().append(Component.literal(" [Join]").withStyle(style -> style
                        .withColor(0x85DBDA).withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(queue.joinUrl()))));
                client.gui.hud.getChat().addClientSystemMessage(message);
            }
            if (!notifications.isEmpty() && config.waitlistSound && client.player != null)
                client.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 0.65F, 1.2F);
            while (openSettings.consumeClick()) client.gui.setScreen(new TaggerSettingsScreen(client.gui.screen()));
        });
        TierTaggerCommands.register(this);
        LOGGER.info("Crit's Tier Tagger ready: {} selected; external providers use verified account UUIDs", selectedProvider.label());
        if (!store.warning().isEmpty()) LOGGER.warn("{}", store.warning());
    }

    public void saveConfig(TaggerConfig edited) throws IOException {
        TaggerConfig validated = edited.copy();
        validated.validate();
        store.save(validated);
        config = validated;
        TierlistProvider next = TierlistProvider.from(validated.tierlist);
        if (selectedProvider != next) {
            repository.disconnect();
            gameRepositories.put(selectedProvider, repository);
            repository = gameRepositories.computeIfAbsent(next, CritsTierTaggerClient::createRepository);
            selectedProvider = next;
            repository.setEnabled(validated.enabled);
            if (validated.enabled) repository.connect();
        }
        repository.setEnabled(validated.enabled);
        loadSelectedTierlist();
        if (!validated.enabled) paidNames.setActive(false);
    }

    public String rankingUsername(String username) {
        return rankingUsername(username, repository.state().rankings(), selectedProvider);
    }

    public String rankingUsername(String username, Rankings rankings) {
        return rankingUsername(username, rankings, selectedProvider);
    }

    public String rankingUsername(String username, Rankings rankings, TierlistProvider provider) {
        String key = Tier.normalizeUsername(username);
        boolean direct = rankings.players().containsKey(key);
        if (provider != TierlistProvider.AFRITIERS) {
            return identities.profileId(username).filter(rankings.players()::containsKey).orElse("");
        }
        // Cracked records and paid records without a usable paid ID remain username-only.
        if (direct && !rankings.paidProfiles().containsValue(key)) return key;
        if (!direct) {
            String alias = config.usernameAliases.get(key);
            if (alias != null && rankings.players().containsKey(alias)) return alias;
        }
        var verified = paidNames.profileId(key);
        if (verified.isPresent()) return rankings.paidProfiles().getOrDefault(verified.get(), "");
        if (direct && paidNames.notFound(key)) return "";
        return direct ? key : "";
    }

    public TierRepository playerSearchRepository() {
        if (profileProvider == selectedProvider && config.enabled) return repository;
        return profileProvider == TierlistProvider.AFRITIERS ? browser
                : profileRepositories.computeIfAbsent(profileProvider, CritsTierTaggerClient::createRepository);
    }

    public void beginPlayerSearch() {
        beginPlayerSearch(defaultProfileProvider());
    }
    public void beginPlayerSearch(TierlistProvider provider) {
        profileProvider = provider;
        browsingPlayers = true;
        var service = playerSearchRepository();
        if (service.state().phase() == RankingRepository.Phase.DISCONNECTED) service.connect();
    }

    public void endPlayerSearch() {
        if (playerSearchRepository() != repository) playerSearchRepository().disconnect();
        browsingPlayers = false;
    }

    public void requestPlayerSearch(String username) {
        var rankings = playerSearchRepository().state().rankings();
        String key = Tier.normalizeUsername(username);
        if (profileProvider != TierlistProvider.AFRITIERS) {
            requestUuidProfile(username, playerSearchRepository(), true);
            return;
        }
        if (rankings.players().containsKey(key) && !rankings.paidProfiles().containsValue(key)) return;
        if (config.usernameAliases.containsKey(key)) return;
        if (!rankings.paidProfiles().isEmpty()) paidNames.request(username);
    }

    public void requestPlayerStatus(String username) {
        if (selectedProvider != TierlistProvider.AFRITIERS) requestUuidProfile(username, repository, true);
    }

    public TierRepository.LookupStatus playerLookupStatus(String username, TierlistProvider provider, TierRepository service) {
        if (provider == TierlistProvider.AFRITIERS) return service.lookupStatus(config.usernameAliases.getOrDefault(Tier.normalizeUsername(username), username));
        var status = identities.lookupStatus(username);
        return status == TierRepository.LookupStatus.FOUND
                ? identities.profileId(username).map(service::lookupStatus).orElse(TierRepository.LookupStatus.LOADING) : status;
    }

    private void requestUuidProfile(String username, TierRepository service, boolean priority) {
        identities.setActive(true);
        identities.request(username, priority);
        var id = identities.profileId(username);
        if (id.isPresent()) service.requestPlayer(id.get(), priority);
        else if (priority && username != null && username.matches("[A-Za-z0-9_]{1,16}")
                && identities.lookupStatus(username) != TierRepository.LookupStatus.NOT_FOUND) {
            var pending = pendingProfiles.computeIfAbsent(service, ignored -> new LinkedHashSet<>());
            if (pending.size() < 128) pending.add(Tier.normalizeUsername(username));
        }
    }

    private void updatePendingProfiles() {
        var services = pendingProfiles.entrySet().iterator();
        while (services.hasNext()) {
            var entry = services.next(); var service = entry.getKey();
            var phase = service.state().phase();
            if (phase == RankingRepository.Phase.DISCONNECTED || phase == RankingRepository.Phase.DISABLED) { services.remove(); continue; }
            entry.getValue().removeIf(name -> {
                identities.request(name, true);
                var id = identities.profileId(name);
                if (id.isPresent()) { service.requestPlayer(id.get(), true); return true; }
                return identities.lookupStatus(name) == TierRepository.LookupStatus.NOT_FOUND;
            });
            if (entry.getValue().isEmpty()) services.remove();
        }
    }

    public void selectTierlist(TierlistProvider provider) throws IOException {
        var edited = config();
        edited.tierlistModes.put(edited.tierlist, edited.mode);
        edited.tierlist = provider.id();
        edited.mode = edited.tierlistModes.getOrDefault(provider.id(), "strongest");
        saveConfig(edited);
    }

    private static TierRepository createRepository(TierlistProvider provider) {
        return provider == TierlistProvider.AFRITIERS ? new RankingRepository(new HttpRankingSource())
                : new ProfileRepository(new HttpPlayerProfileSource(provider));
    }

    private void updateVisibleProfiles(Minecraft client) {
        if (!config.enabled || selectedProvider == TierlistProvider.AFRITIERS) return;
        if (repository.state().phase() == RankingRepository.Phase.DISCONNECTED) return;
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(client.getUser().getName());
        if (client.level != null) client.level.players().stream().limit(512).forEach(player -> names.add(player.getGameProfile().name()));
        if (client.getConnection() != null) client.getConnection().getListedOnlinePlayers().stream().limit(512).forEach(info -> names.add(info.getProfile().name()));
        names.stream().limit(512).forEach(name -> requestUuidProfile(name, repository, false));
    }

    private void updateWorldSession(Minecraft client) {
        if (client.level == null || client.player == null || client.getConnection() == null) return;
        // JOIN already starts the first fetch; initial spawn packets belong to that same sync.
        if (observedWorld == null) {
            observedWorld = client.level;
            observedPlayer = client.player;
            worldRefreshRequested = false;
            return;
        }
        boolean replaced = observedWorld != client.level || observedPlayer != client.player;
        if (!replaced && !worldRefreshRequested) return;
        observedWorld = client.level;
        observedPlayer = client.player;
        worldRefreshRequested = false;
        if (replaced) paidNames.resetSession();
        repository.worldChanged();
    }

    private void updatePaidNames(Minecraft client) {
        boolean active = browsingPlayers || config.enabled && client.player != null && client.getConnection() != null;
        paidNames.setActive(active);
        var rankings = repository.state().rankings();
        if (!active || client.player == null || rankings.paidProfiles().isEmpty()) return;
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(client.player.getGameProfile().name());
        client.level.players().stream().limit(80).forEach(player -> names.add(player.getGameProfile().name()));
        client.getConnection().getListedOnlinePlayers().stream().limit(80).forEach(info -> names.add(info.getProfile().name()));
        for (String username : names) {
            String key = Tier.normalizeUsername(username);
            if (rankings.players().containsKey(key) && !rankings.paidProfiles().containsValue(key)) continue;
            if (!rankings.players().containsKey(key) && config.usernameAliases.containsKey(key)) continue;
            paidNames.request(username);
        }
    }
}
