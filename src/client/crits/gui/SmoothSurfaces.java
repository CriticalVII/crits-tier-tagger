package crits.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Reusable white coverage masks: at most eighteen quads per bordered surface. */
final class SmoothSurfaces {
    private static final java.util.Map<Integer, Identifier[]> MASKS = new java.util.HashMap<>();
    private SmoothSurfaces() { }
    static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int fill, Integer border) {
        int r = Math.min(32, Math.min(radius, Math.min(w, h) / 2));
        var masks = MASKS.computeIfAbsent(r, SmoothSurfaces::create);
        layer(g, masks[0], x, y, w, h, r, fill);
        if (border != null) layer(g, masks[1], x, y, w, h, r, border);
    }
    private static Identifier[] create(int r) {
        int size = (r * 2 + 1) * 4;
        Identifier[] ids = new Identifier[2];
        for (int layer = 0; layer < 2; layer++) {
            NativeImage image = new NativeImage(size, size, true);
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                double dx = Math.max(Math.abs(x + 0.5 - size / 2.0) - 2, 0);
                double dy = Math.max(Math.abs(y + 0.5 - size / 2.0) - 2, 0);
                double distance = Math.sqrt(dx * dx + dy * dy) - r * 4;
                double coverage = Math.clamp(0.5 - distance, 0, 1);
                if (layer == 1) coverage -= Math.clamp(0.5 - distance - 4, 0, 1);
                image.setPixel(x, y, ((int)Math.round(coverage * 255) << 24) | 0xFFFFFF);
            }
            ids[layer] = Identifier.fromNamespaceAndPath("afritiers_tagger", "rounded_mask_" + r + "_" + layer);
            Minecraft.getInstance().getTextureManager().register(ids[layer], new DynamicTexture(() -> "Crits GUI mask", image));
        }
        return ids;
    }
    private static void layer(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h, int r, int colour) {
        int size = (2 * r + 1) * 4;
        int[] xs = {x, x + r, x + w - r}, ys = {y, y + r, y + h - r};
        int[] ws = {r, w - 2 * r, r}, hs = {r, h - 2 * r, r};
        int[] uv = {0, r * 4, (r + 1) * 4}, lengths = {r * 4, 4, r * 4};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            if (ws[col] > 0 && hs[row] > 0) g.blit(RenderPipelines.GUI_TEXTURED, id, xs[col], ys[row], uv[col], uv[row],
                    ws[col], hs[row], lengths[col], lengths[row], size, size, colour);
    }
}
