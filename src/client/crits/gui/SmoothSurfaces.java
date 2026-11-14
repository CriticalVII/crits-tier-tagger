package crits.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Subpixel coverage at framebuffer resolution, scoped to this mod's GUI. */
final class SmoothSurfaces {
    private SmoothSurfaces() { }
    static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int fill, Integer border) {
        int scale = Math.max(1, (int) Minecraft.getInstance().getWindow().getGuiScale());
        int pw = w * scale, ph = h * scale, r = Math.min(radius * scale, Math.min(pw, ph) / 2);
        g.pose().pushMatrix();
        g.pose().translate(x, y).scale(1F / scale);
        if (ph > 2 * r) {
            g.fill(0, r, pw, ph - r, fill);
            if (border != null) {
                g.fill(0, r, scale, ph - r, border);
                g.fill(pw - scale, r, pw, ph - r, border);
            }
        }
        for (int row = 0; row < r; row++) {
            double outer = inset(r, row + 0.5);
            span(g, outer, pw - outer, row, fill);
            span(g, outer, pw - outer, ph - row - 1, fill);
            if (border != null) {
                double inner = row < scale ? pw / 2.0 : scale + inset(Math.max(0, r - scale), row + 0.5 - scale);
                if (row < scale) {
                    span(g, outer, pw - outer, row, border);
                    span(g, outer, pw - outer, ph - row - 1, border);
                } else {
                    span(g, outer, inner, row, border); span(g, pw - inner, pw - outer, row, border);
                    span(g, outer, inner, ph - row - 1, border); span(g, pw - inner, pw - outer, ph - row - 1, border);
                }
            }
        }
        g.pose().popMatrix();
    }
    private static double inset(int r, double row) {
        if (row >= r || r == 0) return 0;
        double dy = r - row;
        return r - Math.sqrt(Math.max(0, r * (double) r - dy * dy));
    }
    private static void span(GuiGraphicsExtractor g, double left, double right, int y, int colour) {
        if (right <= left) return;
        int first = (int) Math.floor(left), last = (int) Math.ceil(right) - 1;
        if (first == last) { pixel(g, first, y, colour, right - left); return; }
        pixel(g, first, y, colour, first + 1 - left);
        if (last > first + 1) g.fill(first + 1, y, last, y + 1, colour);
        pixel(g, last, y, colour, right - last);
    }
    private static void pixel(GuiGraphicsExtractor g, int x, int y, int colour, double coverage) {
        int alpha = (int) Math.round((colour >>> 24) * Math.clamp(coverage, 0, 1));
        if (alpha > 0) g.fill(x, y, x + 1, y + 1, alpha << 24 | colour & 0xFFFFFF);
    }
}
