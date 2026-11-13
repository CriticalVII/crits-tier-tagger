package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/** A shared charcoal workbench, with a pinned rail on wide screens and a compact top navigation. */
record TaggerFrame(int left, int top, int width, int height, int rail, long appearedAt) {
    static TaggerFrame fit(int screenWidth, int screenHeight) {
        int width = Math.min(720, screenWidth - 20), height = Math.min(430, screenHeight - 16);
        return new TaggerFrame((screenWidth - width) / 2, (screenHeight - height) / 2, width, height,
                width >= 500 && height >= 300 ? 104 : 0, System.nanoTime());
    }
    int x() { return left + rail; }
    int y() { return top; }
    int w() { return width - rail; }
    int h() { return height; }
    int offset() { return rail == 0 ? 23 : 0; }
    static Component heading(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("afritiers_tagger", "ui"))));
    }
    void draw(GuiGraphicsExtractor g, String page) {
        var client = Minecraft.getInstance(); var font = client.font;
        TaggerWidgets.backdrop(g, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        TaggerWidgets.panel(g, left, top, width, height);
        if (rail > 0) {
            g.fill(left + 1, top + 1, x(), top + height - 1, 0xFF121415);
            g.fill(x() - 1, top, x(), top + height, TaggerWidgets.LINE);
            g.text(font, heading("crit's"), left + 12, top + 15, TaggerWidgets.TEXT, false);
            g.text(font, "TIER TAGGER", left + 12, top + 36, TaggerWidgets.MUTED, false);
            g.fill(left + 12, top + 56, x() - 12, top + 57, TaggerWidgets.LINE);
            g.text(font, "WORKSPACE", left + 12, top + 71, TaggerWidgets.FAINT, false);
            g.fill(left + 12, top + height - 47, x() - 12, top + height - 46, TaggerWidgets.LINE);
            g.fill(left + 12, top + height - 29, left + 15, top + height - 26, TaggerWidgets.GREEN);
            g.text(font, "Auto saved", left + 21, top + height - 31, TaggerWidgets.MUTED, false);
        }
        float reveal = client.options.screenEffectScale().get() == 0 ? 1 : Math.min(1, (System.nanoTime() - appearedAt) / 180_000_000F);
        int headingColor = ((int) (255 * (0.65F + 0.35F * reveal)) << 24) | (TaggerWidgets.TEXT & 0xFFFFFF);
        g.text(font, heading(page), x() + 14, top + 11, headingColor, false);
        g.fill(x() + 12, top + 32 + offset(), left + width - 12, top + 33 + offset(), TaggerWidgets.LINE);
        g.fill(x() + 12, top + height - 36, left + width - 12, top + height - 35, TaggerWidgets.LINE);
    }
}
