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
    TierlistsScreen(TaggerSettingsScreen owner) { super(Component.literal("Tierlists")); this.owner = owner; }
    @Override protected void init() {
        AfriTiersClient.instance().loadSelectedTierlist();
        w = Math.min(520, width - 20); h = Math.min(348, height - 16);
        x = (width - w) / 2; y = (height - h) / 2; offset = TaggerTabs.offset(w);
        TaggerTabs.add(this::addRenderableWidget, owner, "Tierlists", x, y, w);
        var grid = LinearLayout.vertical().spacing(10);
        LinearLayout row = null;
        int index = 0;
        for (var provider : TierlistProvider.values()) {
            if (index++ % 2 == 0) { row = LinearLayout.horizontal().spacing(12); grid.addChild(row); }
            var button = TierlistLogo.button(provider, provider.label(), (w - 36) / 2, 34,
                    provider == AfriTiersClient.instance().selectedProvider(), pressed -> {
                        try {
                            AfriTiersClient.instance().selectTierlist(provider);
                            feedback = "Saved · now showing " + provider.label();
                            rebuildWidgets();
                        } catch (java.io.IOException exception) { feedback = "Could not save. Check config permissions."; }
                    });
            button.setTooltip(Tooltip.create(Component.literal(provider.description() + "\n" + provider.website())));
            row.addChild(button);
        }
        var scroll = new ScrollableLayout(minecraft, grid, Math.max(32, h - 121 - offset), ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.setMinWidth(w - 24); scroll.arrangeElements(); scroll.setX(x + 12); scroll.setY(y + 72 + offset);
        scroll.visitWidgets(this::addRenderableWidget);
        var back = TaggerWidgets.action("Back", 80, false, button -> onClose());
        back.setPosition(x + w - 92, y + h - 30); addRenderableWidget(back);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x28090A13); TaggerWidgets.panel(g, x, y, w, h);
        g.text(font, "Crit's", x + 12, y + 12, TaggerWidgets.TEXT);
        g.fill(x + 12, y + 32 + offset, x + w - 12, y + 33 + offset, TaggerWidgets.LINE);
        g.text(font, "In-game tierlist", x + 12, y + 44 + offset, TaggerWidgets.GREEN);
        g.text(font, font.plainSubstrByWidth("One source for Tab and nametags. Profiles has its own selector.", w - 24), x + 12, y + 58 + offset, TaggerWidgets.MUTED);
        g.text(font, font.plainSubstrByWidth(feedback.isEmpty() ? "AfriTiers waitlist notifications stay independent." : feedback, w - 24),
                x + 12, y + h - 45, TaggerWidgets.MUTED);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    @Override public void onClose() { minecraft.gui.setScreen(owner); }
    @Override public boolean isPauseScreen() { return false; }
}
