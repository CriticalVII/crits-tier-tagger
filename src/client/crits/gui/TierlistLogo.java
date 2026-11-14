package crits.gui;

import crits.provider.TierlistProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Bundled provider marks identify the source without adding anything to player tags. */
final class TierlistLogo {
    private TierlistLogo() { }

    static void draw(GuiGraphicsExtractor g, TierlistProvider provider, int x, int y, int size) {
        GuiIcons.draw(g,
                Identifier.fromNamespaceAndPath("afritiers_tagger", "provider/" + provider.id()), x, y, size, size);
    }

    static Button button(TierlistProvider provider, String label, int width, int height, boolean selected, Button.OnPress action) {
        return new TaggerWidgets.Action(label, width, height, selected, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
                float hover = hoverAmount();
                TaggerWidgets.surface(g, getX(), getY(), getWidth(), getHeight(), 3,
                        selected ? GuiTheme.selection : hover > 0 ? GuiTheme.hover : GuiTheme.surface,
                        selected || isFocused() ? GuiTheme.accent : GuiTheme.border);
                var font = TaggerFonts.body();
                String text = font.plainSubstrByWidth(getMessage().getString(), getWidth() - 27);
                int left = getX() + (getWidth() - font.width(text) - 17) / 2;
                draw(g, provider, left, getY() + (getHeight() - 12) / 2, 12);
                g.text(font, text, left + 17, getY() + (getHeight() - 8) / 2, active ? GuiTheme.text : GuiTheme.muted, false);
            }
        };
    }

    static Button tile(TierlistProvider provider, int width, int height, boolean selected, Button.OnPress action) {
        return new TaggerWidgets.Tile(provider.label(), width, height, selected,
                Identifier.fromNamespaceAndPath("afritiers_tagger", "provider/" + provider.id()), action);
    }
}
