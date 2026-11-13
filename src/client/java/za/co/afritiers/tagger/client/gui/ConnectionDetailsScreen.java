package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import za.co.afritiers.tagger.client.AfriTiersClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Read-only connection diagnostics alongside the automatically saved settings. */
final class ConnectionDetailsScreen extends Screen {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss").withZone(ZoneId.systemDefault());
    private final TaggerSettingsScreen owner;
    private final List<StringWidget> values = new ArrayList<>();
    private int x, y, w, h;
    ConnectionDetailsScreen(TaggerSettingsScreen owner) { super(net.minecraft.client.Minecraft.getInstance(), TaggerFonts.body(), Component.literal("Connection details")); this.owner = owner; }

    @Override protected void init() {
        values.clear();
        w = Math.min(460, width - 20);
        h = Math.min(280, height - 16);
        x = (width - w) / 2;
        y = (height - h) / 2;
        LinearLayout content = LinearLayout.vertical().spacing(8);
        for (int i = 0; i < 14; i++) values.add(content.addChild(new StringWidget(w - 44, 12, Component.empty(), font)));
        ScrollableLayout scroll = new ScrollableLayout(minecraft, content, h - 86, ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.arrangeElements();
        scroll.setX(x + 14);
        scroll.setY(y + 40);
        scroll.visitWidgets(this::addRenderableWidget);
        Button back = TaggerWidgets.action("Back", 80, true, pressed -> onClose());
        back.setPosition(x + w - 94, y + h - 30);
        addRenderableWidget(back);
        tick();
    }

    @Override public void tick() {
        var state = AfriTiersClient.instance().repository().state();
        var rankings = state.rankings();
        var mod = AfriTiersClient.instance();
        var provider = mod.selectedProvider();
        boolean bulk = provider == za.co.afritiers.tagger.provider.TierlistProvider.AFRITIERS;
        List<String> lines = List.of(
                provider.label() + ": " + state.message(),
                (bulk ? "Ranked usernames: " : "Cached UUID profiles: ") + rankings.players().size(),
                "Last attempt: " + time(state.lastAttempt()),
                "Last success: " + time(state.lastSuccess()),
                (bulk ? "Source updated: " : "Last profile update: ") + (rankings.updatedAt() == null ? "Unknown" : DATE.format(rankings.updatedAt())),
                "Data age: " + (rankings.updatedAt() == null ? "Unknown" : Math.max(0, java.time.Duration.between(rankings.updatedAt(), Instant.now()).toSeconds()) + "s"),
                "Next refresh: " + time(state.nextRefresh()),
                bulk ? "Source: " + (rankings.updatedAt() == null ? "Not loaded" : rankings.stale() ? "Stale" : "Current")
                        : "Profiles are fetched on demand and cached for five minutes.",
                bulk ? "Ambiguous names: " + rankings.ambiguousNames() + " | Skipped records: " + rankings.skippedRecords()
                        : "Points and overall ranks come directly from " + provider.label() + ".",
                bulk ? "Matches usernames, including offline accounts." : "Tier profiles are matched only by verified account UUID.",
                bulk ? "Paid-name lookup: " + mod.paidNames().status() : "Names resolve through Minecraft; server UUIDs are ignored.",
                bulk ? "Saved username aliases: " + mod.config().usernameAliases.size() : "Username aliases are not used for this tierlist.",
                "Refresh uses saved settings.",
                AfriTiersClient.instance().configWarning());
        for (int i = 0; i < values.size(); i++) {
            String line = lines.get(i);
            values.get(i).setMessage(Component.literal(font.plainSubstrByWidth(line, w - 44)).withColor(TaggerWidgets.MUTED));
            values.get(i).setTooltip(TaggerFonts.tooltip(Component.literal(line)));
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        TaggerWidgets.backdrop(g, width, height);
        TaggerWidgets.panel(g, x, y, w, h);
        g.text(font, TaggerFrame.heading(title.getString()), x + 14, y + 12, TaggerWidgets.TEXT, false);
        g.fill(x + 14, y + 31, x + w - 14, y + 32, TaggerWidgets.LINE);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private static String time(Instant instant) { return instant == null ? "Never" : TIME.format(instant); }
    @Override public void onClose() { minecraft.gui.setScreen(owner); }
    @Override public boolean isPauseScreen() { return false; }
}
