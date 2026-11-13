package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import za.co.afritiers.tagger.client.AfriTiersClient;
import za.co.afritiers.tagger.provider.TierlistProvider;

/** Exactly one provider supplies in-game tags; selection never silently falls back to another service. */
public final class TierlistsScreen extends Screen {
    private final TaggerSettingsScreen owner;
    private int x, y, w, h, offset;
    private String feedback = "";
    private TaggerFrame frame;
    TierlistsScreen(TaggerSettingsScreen owner) { super(net.minecraft.client.Minecraft.getInstance(), TaggerFonts.body(), Component.literal("Tierlists")); this.owner = owner; }
    @Override protected void init() {
        AfriTiersClient.instance().loadSelectedTierlist();
        frame = TaggerFrame.fit(width, height);
        w = frame.w(); h = frame.h(); x = frame.x(); y = frame.y(); offset = frame.offset();
        TaggerTabs.add(this::addRenderableWidget, owner, "Tierlists", frame);
        var grid = LinearLayout.vertical().spacing(8);
        boolean gallery = w >= 440 && h >= 300;
        int columns = gallery ? 4 : 2, cellWidth = (w - 36 - (columns - 1) * 8) / columns;
        LinearLayout row = null;
        int index = 0;
        for (var provider : TierlistProvider.values()) {
            if (index++ % columns == 0) { row = LinearLayout.horizontal().spacing(8); grid.addChild(row); }
            Button.OnPress choose = pressed -> {
                        try {
                            AfriTiersClient.instance().selectTierlist(provider);
                            feedback = "Saved · now showing " + provider.label();
                            rebuildWidgets();
                        } catch (java.io.IOException exception) { feedback = "Could not save. Check config permissions."; }
                    };
            boolean selected = provider == AfriTiersClient.instance().selectedProvider();
            var button = gallery ? TierlistLogo.tile(provider, cellWidth, 76, selected, choose)
                    : TierlistLogo.button(provider, provider.label(), cellWidth, 34, selected, choose);
            button.setTooltip(TaggerFonts.tooltip(Component.literal(provider.description() + "\n" + provider.website())));
            row.addChild(button);
        }
        var scroll = new ScrollableLayout(minecraft, grid, Math.max(32, h - 121 - offset), ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.setMinWidth(w - 24); scroll.arrangeElements(); scroll.setX(x + 12); scroll.setY(y + 72 + offset);
        scroll.visitWidgets(this::addRenderableWidget);
        var back = TaggerWidgets.action("Back", 80, false, button -> onClose());
        back.setPosition(x + w - 92, y + h - 30); addRenderableWidget(back);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        frame.draw(g, "Tierlists");
        g.text(font, "In-game tierlist", x + 12, y + 44 + offset, TaggerWidgets.TEXT);
        g.text(font, font.plainSubstrByWidth("One source for Tab and nametags. Profiles has its own selector.", w - 24), x + 12, y + 58 + offset, TaggerWidgets.MUTED);
        if (h >= 300) {
            int cardY = y + 166 + offset;
            var mod = AfriTiersClient.instance(); var provider = mod.selectedProvider();
            TaggerWidgets.surface(g, x + 12, cardY, w - 24, 76, 3, 0xFF111111, TaggerWidgets.LINE);
            TierlistLogo.draw(g, provider, x + 24, cardY + 14, 18);
            g.text(font, TaggerFrame.heading(provider.label()), x + 50, cardY + 14, TaggerWidgets.TEXT, false);
            var state = mod.repository().state();
            String status = switch (state.phase()) {
                case READY -> "Ready for player tiers";
                case LOADING -> "Loading rankings...";
                case STALE -> "Source data is out of date";
                case DISABLED -> "Tier tags are disabled";
                case DISCONNECTED -> "Select source to connect";
                case ERROR -> state.message();
            };
            g.text(font, font.plainSubstrByWidth(status, w - 48), x + 24, cardY + 40, TaggerWidgets.MUTED, false);
            g.text(font, font.plainSubstrByWidth("Mode: " + provider.modeLabel(mod.config().mode), w - 48), x + 24, cardY + 56, TaggerWidgets.CYAN, false);
        }
        g.text(font, font.plainSubstrByWidth(feedback.isEmpty() ? "AfriTiers waitlist notifications stay independent." : feedback, w - 24),
                x + 12, y + h - 45, TaggerWidgets.MUTED);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    @Override public void onClose() { minecraft.gui.setScreen(owner); }
    @Override public boolean isPauseScreen() { return false; }
}
