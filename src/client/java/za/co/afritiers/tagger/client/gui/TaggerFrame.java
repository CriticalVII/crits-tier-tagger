package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/** A black workspace with an ungrouped navigation rail at every GUI scale. */
record TaggerFrame(int left, int top, int width, int height, int rail, long appearedAt) {
    static TaggerFrame fit(int screenWidth, int screenHeight) {
        return new TaggerFrame(0, 0, screenWidth, screenHeight,
                Math.max(72, Math.min(118, screenWidth * 14 / 100)), System.nanoTime());
    }
    int x() { return left + rail; }
    int y() { return top; }
    int w() { return width - rail; }
    int h() { return height; }
    int offset() { return 0; }
    static Component heading(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("afritiers_tagger", "ui"))));
    }
    void draw(GuiGraphicsExtractor g, String page) {
        var client = Minecraft.getInstance(); var font = TaggerFonts.body();
        TaggerWidgets.backdrop(g, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
            g.fill(left, top, x(), top + height, 0xFF0B0B0B);
            g.fill(x() - 1, top, x(), top + height, TaggerWidgets.LINE);
            if (font.width("Crit's Tier Tagger") <= rail - 20) {
                g.text(font, "Crit's Tier Tagger", left + 10, top + 16, TaggerWidgets.TEXT, false);
            } else {
                g.text(font, "Crit's", left + 10, top + 12, TaggerWidgets.TEXT, false);
                g.text(font, "Tier Tagger", left + 10, top + 25, TaggerWidgets.TEXT, false);
            }
        float reveal = client.options.screenEffectScale().get() == 0 ? 1 : Math.min(1, (System.nanoTime() - appearedAt) / 180_000_000F);
        int headingColor = ((int) (255 * (0.85F + 0.15F * reveal)) << 24) | (TaggerWidgets.TEXT & 0xFFFFFF);
        g.text(font, heading(page), x() + 14, top + 11, headingColor, false);
        g.fill(x(), top + 32 + offset(), left + width, top + 33 + offset(), TaggerWidgets.LINE);
    }
}
