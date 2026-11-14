package crits.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** One themed tier hover card, drawn above the widgets after their render pass. */
final class TierHover {
    private static Component text;
    private static int mouseX, mouseY;
    private TierHover() { }
    static void clear() { text = null; }
    static void show(Component value, int x, int y) { text = value; mouseX = x; mouseY = y; }
    static void draw(GuiGraphicsExtractor g, int width, int height) {
        if (text == null) return;
        var font = TaggerFonts.body();
        var lines = font.split(text, Math.max(40, Math.min(280, width - 24)));
        int w = lines.stream().mapToInt(font::width).max().orElse(0) + 16, h = lines.size() * 12 + 12;
        int x = Math.max(4, Math.min(width - w - 4, mouseX + 12));
        int y = mouseY + 16 + h <= height - 4 ? mouseY + 16 : Math.max(4, mouseY - h - 8);
        g.nextStratum();
        TaggerWidgets.opaqueSurface(g, x, y, w, h, GuiTheme.radius(6), GuiTheme.tooltipFill(), GuiTheme.border);
        for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), x + 8, y + 6 + i * 12, GuiTheme.text, false);
    }
}
