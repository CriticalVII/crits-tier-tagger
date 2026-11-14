package crits.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Own the background pass so Minecraft never applies a second blur in the same frame. */
public abstract class TaggerScreen extends Screen {
    protected TaggerScreen(Minecraft client, Font font, Component title) { super(client, font, title); }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TierHover.clear();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        TierHover.draw(graphics, width, height);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TaggerWidgets.backdrop(graphics, width, height);
        minecraft.gui.hud.extractDeferredSubtitles();
    }
}
