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
            row.addChild(new ProviderCard(provider, (w - 36) / 2, h < 280 ? 58 : 72));
        }
        var scroll = new ScrollableLayout(minecraft, grid, Math.max(32, h - 121 - offset), ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.setMinWidth(w - 24); scroll.arrangeElements(); scroll.setX(x + 12); scroll.setY(y + 72 + offset);
        scroll.visitWidgets(this::addRenderableWidget);
        var back = TaggerWidgets.action("Back", 80, false, button -> onClose());
        back.setPosition(x + w - 92, y + h - 30); addRenderableWidget(back);
    }
    private final class ProviderCard extends TaggerWidgets.Action {
        private final TierlistProvider provider;
        ProviderCard(TierlistProvider provider, int width, int height) {
            super(provider.label(), width, height, false, button -> {
                try { AfriTiersClient.instance().selectTierlist(provider); feedback = "Saved · now showing " + provider.label(); }
                catch (java.io.IOException exception) { feedback = "Could not save. Check config permissions."; }
            });
            this.provider = provider;
            setTooltip(Tooltip.create(Component.literal(provider.description() + "\n" + provider.website())));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            boolean selected = provider == AfriTiersClient.instance().selectedProvider();
            g.fill(getX(), getY(), getRight(), getBottom(), selected ? 0x90694C8C : isHoveredOrFocused() ? 0x70584C6C : 0x40352E45);
            g.fill(getX(), getY(), getX() + 2, getBottom(), selected ? TaggerWidgets.GREEN : TaggerWidgets.LINE);
            if (isFocused()) g.outline(getX(), getY(), getWidth(), getHeight(), TaggerWidgets.GREEN);
            g.text(font, Component.literal(provider.label()).withStyle(style -> style.withBold(true)), getX() + 10, getY() + 10, TaggerWidgets.TEXT);
            g.text(font, font.plainSubstrByWidth(selected ? "SELECTED" : "Select tierlist", getWidth() - 20), getX() + 10, getY() + 26,
                    selected ? 0xFFFFD700 : TaggerWidgets.MUTED);
            if (getHeight() >= 68) g.text(font, font.plainSubstrByWidth(provider.description(), getWidth() - 20), getX() + 10, getY() + 43, TaggerWidgets.MUTED);
            g.text(font, font.plainSubstrByWidth(AfriTiersClient.instance().providerStatus(provider), getWidth() - 20),
                    getX() + 10, getBottom() - 13, TaggerWidgets.MUTED);
        }
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
