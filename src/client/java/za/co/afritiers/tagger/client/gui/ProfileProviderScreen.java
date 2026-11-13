package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import za.co.afritiers.tagger.provider.TierlistProvider;

/** A profile-only choice never changes the user's in-game provider. */
final class ProfileProviderScreen extends Screen {
    private final ProfilesScreen owner;
    private final TierlistProvider selected;
    private int x, y, w, h;
    ProfileProviderScreen(ProfilesScreen owner, TierlistProvider selected) {
        super(net.minecraft.client.Minecraft.getInstance(), TaggerFonts.body(), Component.literal("Profile tierlist")); this.owner = owner; this.selected = selected;
    }
    @Override protected void init() {
        w = Math.min(420, width - 20); h = Math.min(220, height - 16);
        x = (width - w) / 2; y = (height - h) / 2;
        int index = 0, cell = (w - 36) / 2;
        for (var provider : TierlistProvider.values()) {
            var button = TierlistLogo.button(provider, provider.label(), cell, 34, provider == selected, pressed -> {
                owner.selectProvider(provider); onClose();
            });
            button.setPosition(x + 12 + index % 2 * (cell + 12), y + 66 + index / 2 * 44);
            addRenderableWidget(button); index++;
        }
        var back = TaggerWidgets.action("Back", 80, false, pressed -> onClose());
        back.setPosition(x + w - 92, y + h - 30); addRenderableWidget(back);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        TaggerWidgets.backdrop(g, width, height); TaggerWidgets.panel(g, x, y, w, h);
        g.text(font, TaggerFrame.heading("Profile tierlist"), x + 12, y + 12, TaggerWidgets.TEXT, false);
        g.fill(x + 12, y + 29, x + w - 12, y + 30, TaggerWidgets.LINE);
        g.text(font, font.plainSubstrByWidth("Choose where to search this player's rankings.", w - 24), x + 12, y + 36, TaggerWidgets.MUTED);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    @Override public void onClose() { minecraft.gui.setScreen(owner); }
    @Override public boolean isPauseScreen() { return false; }
}
