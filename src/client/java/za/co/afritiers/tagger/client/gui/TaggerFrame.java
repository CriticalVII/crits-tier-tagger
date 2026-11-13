package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/** A shared charcoal workbench, with a pinned rail on wide screens and a compact top navigation. */
record TaggerFrame(int left, int top, int width, int height, int rail, long appearedAt) {
    static TaggerFrame fit(int screenWidth, int screenHeight) {
        return new TaggerFrame(0, 0, screenWidth, screenHeight,
                screenWidth >= 440 && screenHeight >= 280 ? Math.max(78, Math.min(118, screenWidth * 14 / 100)) : 0, System.nanoTime());
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
        var client = Minecraft.getInstance(); var font = TaggerFonts.body();
        TaggerWidgets.backdrop(g, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        if (rail > 0) {
            g.fill(left, top, x(), top + height, 0xFF0B0B0B);
            g.fill(x() - 1, top, x(), top + height, TaggerWidgets.LINE);
            TaggerWidgets.rounded(g, left + 9, top + 10, 17, 17, 3, 0xFF252525);
            g.text(font, "C", left + 13, top + 15, TaggerWidgets.TEXT, false);
            g.text(font, "Crit's", left + 31, top + 15, TaggerWidgets.TEXT, false);
            g.text(font, "Tier tagger", left + 10, top + 45, TaggerWidgets.MUTED, false);
            g.text(font, "Players", left + 10, top + 123, TaggerWidgets.MUTED, false);
            g.fill(left + 10, top + height - 42, x() - 10, top + height - 41, TaggerWidgets.LINE);
            g.text(font, "My account", left + 10, top + height - 31, TaggerWidgets.MUTED, false);
            g.text(font, font.plainSubstrByWidth(client.getUser().getName(), rail - 20), left + 10, top + height - 17, TaggerWidgets.TEXT, false);
        } else {
            g.text(font, "crit's", x() + w() - font.width("crit's") - 14, top + 17, TaggerWidgets.MUTED, false);
        }
        float reveal = client.options.screenEffectScale().get() == 0 ? 1 : Math.min(1, (System.nanoTime() - appearedAt) / 180_000_000F);
        int headingColor = ((int) (255 * (0.85F + 0.15F * reveal)) << 24) | (TaggerWidgets.TEXT & 0xFFFFFF);
        g.text(font, heading(page), x() + 14, top + 11, headingColor, false);
        g.fill(x(), top + 32 + offset(), left + width, top + 33 + offset(), TaggerWidgets.LINE);
    }
}
