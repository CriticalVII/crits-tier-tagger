package crits.gui;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** High-resolution originals for GUI use; in-world glyph textures and metrics stay unchanged. */
final class GuiIcons {
    private static final Map<Identifier, Identifier> TEXTURES = new HashMap<>();
    private GuiIcons() { }
    static void draw(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int size) {
        draw(g, sprite, x, y, size, size);
    }
    static void draw(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int width, int height) {
        Identifier texture = TEXTURES.computeIfAbsent(sprite, id -> Identifier.fromNamespaceAndPath(id.getNamespace(),
                "textures/gui/icons/" + id.getPath() + ".png"));
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, width, height, 256, 256, 256, 256);
    }
}
