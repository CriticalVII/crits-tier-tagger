package dev.crits.tiertagger.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.crits.tiertagger.client.CritsTierTaggerClient;
import dev.crits.tiertagger.client.gui.ProfilesScreen;
import dev.crits.tiertagger.client.gui.TaggerSettingsScreen;
import dev.crits.tiertagger.provider.TierlistProvider;
import java.io.IOException;
import java.util.Locale;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Client commands delegate to the same validated settings and ranking services as the GUI. */
public final class TierTaggerCommands {
    private final CritsTierTaggerClient mod;

    private TierTaggerCommands(CritsTierTaggerClient mod) {
        this.mod = mod;
    }

    public static void register(CritsTierTaggerClient mod) {
        var commands = new TierTaggerCommands(mod);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String root : new String[] {"crittiers", "ctiers", "afritiers"}) dispatcher.register(
                literal(root)
                        .then(commands.aliases())
                        .then(literal("config").requires(FabricClientCommandSource::attended).executes(context -> {
                            Minecraft client = context.getSource().getClient();
                            // Chat closes after command execution; open the screen on the next client task.
                            client.schedule(() -> client.gui.setScreen(new TaggerSettingsScreen(null)));
                            return 1;
                        }))
                        .then(literal("refresh").requires(FabricClientCommandSource::attended).executes(context -> {
                            context.getSource().sendFeedback(Component.literal(mod.repository().refresh()));
                            return 1;
                        }))
                        .then(literal("status").executes(context -> commands.status(context.getSource()))
                                .then(argument("username", StringArgumentType.word()).executes(context ->
                                        commands.playerStatus(context.getSource(), StringArgumentType.getString(context, "username"))))));
            dispatcher.register(literal("tiers").requires(FabricClientCommandSource::attended)
                    .executes(context -> commands.openProfile(context.getSource(), null))
                    .then(argument("player", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                var names = new java.util.TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                                var connection = context.getSource().getClient().getConnection();
                                if (connection != null) connection.getListedOnlinePlayers().forEach(info -> names.add(info.getProfile().name()));
                                mod.repository().state().rankings().stats().values().forEach(profile -> names.add(profile.name()));
                                names.stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase()))
                                        .limit(100).forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(context -> commands.openProfile(context.getSource(), StringArgumentType.getString(context, "player")))
                            .then(argument("tierlist", StringArgumentType.word()).suggests((context, builder) -> {
                                for (var provider : TierlistProvider.values()) if (provider.id().startsWith(builder.getRemainingLowerCase())) builder.suggest(provider.id());
                                return builder.buildFuture();
                            }).executes(context -> commands.openProviderProfile(context.getSource(), StringArgumentType.getString(context, "player"),
                                    StringArgumentType.getString(context, "tierlist"))))));
        });
    }

    private int openProfile(FabricClientCommandSource source, String username) {
        return openProfile(source, username, mod.defaultProfileProvider());
    }

    private int openProviderProfile(FabricClientCommandSource source, String username, String providerId) {
        var provider = TierlistProvider.find(providerId.toLowerCase(Locale.ROOT));
        if (provider.isEmpty()) { source.sendError(Component.literal("Choose afritiers, mctiers, pvptiers or subtiers.")); return 0; }
        return openProfile(source, username, provider.get());
    }

    private int openProfile(FabricClientCommandSource source, String username, TierlistProvider provider) {
        if (username != null && !username.matches("[A-Za-z0-9_]{1,16}")) {
            source.sendError(Component.literal("Use a Minecraft username (1–16 letters, numbers or underscores)."));
            return 0;
        }
        Minecraft client = source.getClient();
        client.schedule(() -> {
            var settings = new TaggerSettingsScreen(null);
            client.gui.setScreen(username == null ? new ProfilesScreen(settings) : new ProfilesScreen(settings, username, provider));
        });
        return 1;
    }

    private int status(FabricClientCommandSource source) {
        var state = mod.repository().state();
        var config = mod.config();
        source.sendFeedback(Component.literal("Crit's tiers · " + mod.selectedProvider().label() + ": " + state.message()
                + " | " + state.rankings().players().size() + (mod.selectedProvider() == TierlistProvider.AFRITIERS ? " usernames" : " UUID profiles")
                + " | ambiguous: " + state.rankings().ambiguousNames()
                + " | Nameplates: " + (config.nameplates ? "ON" : "OFF")
                + " | Tab: " + (config.tabList ? "ON" : "OFF")
                + " | Mode: " + config.mode));
        return 1;
    }

    private int playerStatus(FabricClientCommandSource source, String username) {
        var rankings = mod.repository().state().rankings();
        var config = mod.config();
        String apiName = mod.rankingUsername(username);
        mod.requestPlayerStatus(username);
        String result = rankings.select(apiName, config.mode, config.includeRetired)
                .map(tier -> tier.label())
                .orElseGet(() -> rankings.players().containsKey(apiName)
                        ? "No tier matches the selected mode/retired settings"
                        : "No matching ranking yet; " + mod.playerLookupStatus(username, mod.selectedProvider(), mod.repository()));
        source.sendFeedback(Component.literal(mod.selectedProvider().label() + ": " + username + " | " + result
                + (apiName.isEmpty() || apiName.equalsIgnoreCase(username) ? "" : (mod.selectedProvider() == TierlistProvider.AFRITIERS ? " | API name: " : " | UUID: ") + apiName)
                + " | Nameplates: " + (config.nameplates ? "ON" : "OFF")));
        return 1;
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> aliases() {
        return literal("alias")
                .then(literal("set").requires(FabricClientCommandSource::attended)
                        .then(argument("current", StringArgumentType.word())
                                .then(argument("apiName", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            mod.repository().state().rankings().stats().values().stream().map(profile -> profile.name()).sorted()
                                                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase()))
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> setAlias(context.getSource(),
                                                StringArgumentType.getString(context, "current"),
                                                StringArgumentType.getString(context, "apiName"))))))
                .then(literal("remove").requires(FabricClientCommandSource::attended)
                        .then(argument("current", StringArgumentType.word()).executes(context ->
                                removeAlias(context.getSource(), StringArgumentType.getString(context, "current")))))
                .then(literal("list").executes(context -> {
                    if (mod.selectedProvider() != TierlistProvider.AFRITIERS) return uuidOnly(context.getSource());
                    context.getSource().sendFeedback(Component.literal(mod.selectedProvider().label() + " username aliases: " + new TreeMap<>(mod.config().aliases(mod.selectedProvider()))));
                    return 1;
                }));
    }

    private int setAlias(FabricClientCommandSource source, String current, String apiName) {
        if (mod.selectedProvider() != TierlistProvider.AFRITIERS) return uuidOnly(source);
        var edited = mod.config();
        edited.editAliases(mod.selectedProvider()).put(current.toLowerCase(Locale.ROOT), apiName);
        try {
            mod.saveConfig(edited);
            source.sendFeedback(Component.literal("Saved username alias: " + current + " -> " + apiName));
            return 1;
        } catch (IOException | IllegalArgumentException exception) {
            source.sendError(Component.literal("Could not save alias: " + exception.getMessage()));
            return 0;
        }
    }

    private int removeAlias(FabricClientCommandSource source, String current) {
        if (mod.selectedProvider() != TierlistProvider.AFRITIERS) return uuidOnly(source);
        var edited = mod.config();
        edited.editAliases(mod.selectedProvider()).remove(current.toLowerCase(Locale.ROOT));
        try {
            mod.saveConfig(edited);
            source.sendFeedback(Component.literal("Removed username alias: " + current));
            return 1;
        } catch (IOException | IllegalArgumentException exception) {
            source.sendError(Component.literal("Could not save aliases: " + exception.getMessage()));
            return 0;
        }
    }

    private int uuidOnly(FabricClientCommandSource source) {
        source.sendError(Component.literal(mod.selectedProvider().label() + " uses verified Minecraft UUIDs. Username aliases apply only to AfriTiers."));
        return 0;
    }
}
