package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import za.co.afritiers.tagger.provider.TierlistProvider;

/** Bundled provider marks identify the source without adding anything to player tags. */
final class TierlistLogo {
    private TierlistLogo() { }

    static void draw(GuiGraphicsExtractor g, TierlistProvider provider, int x, int y, int size) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("afritiers_tagger", "provider/" + provider.id()), x, y, size, size);
    }

    static Button button(TierlistProvider provider, String label, int width, int height, boolean selected, Button.OnPress action) {
        return new TaggerWidgets.Action(label, width, height, selected, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
                extractBackground(g);
                var font = Minecraft.getInstance().font;
                String text = font.plainSubstrByWidth(getMessage().getString(), getWidth() - 27);
                int left = getX() + (getWidth() - font.width(text) - 17) / 2;
                draw(g, provider, left, getY() + (getHeight() - 12) / 2, 12);
                g.text(font, text, left + 17, getY() + (getHeight() - 8) / 2, active ? selected ? TaggerWidgets.INK : TaggerWidgets.TEXT : TaggerWidgets.FAINT, false);
            }
        };
    }
}
