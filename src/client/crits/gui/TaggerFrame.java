package crits.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/** An inset, translucent workspace with persistent side navigation. */
record TaggerFrame(int left, int top, int width, int height, int rail, long appearedAt) {
    static TaggerFrame fit(int screenWidth, int screenHeight) {
        int inset = screenWidth < 400 || screenHeight < 300 ? 4 : 10;
        return new TaggerFrame(inset, inset, screenWidth - inset * 2, screenHeight - inset * 2,
                Math.max(76, Math.min(128, screenWidth * 19 / 100)), System.nanoTime());
    }
    int x() { return left + rail; }
    int y() { return top; }
    int w() { return width - rail; }
    int h() { return height; }
    static Component heading(String text) {
        if (!GuiTheme.smoothFont()) return Component.literal(text);
        return Component.literal(text).withStyle(style -> style.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("afritiers_tagger", "ui"))));
    }
    void draw(GuiGraphicsExtractor g, String page) {
        var client = Minecraft.getInstance(); var font = TaggerFonts.body();
        TaggerWidgets.surface(g, left, top, width, height, 10, GuiTheme.translucent(GuiTheme.background), GuiTheme.border);
        g.enableScissor(left + 1, top + 1, x(), top + height - 1);
        TaggerWidgets.rounded(g, left + 1, top + 1, rail + 10, height - 2, 9, GuiTheme.translucent(GuiTheme.sidebar));
        g.disableScissor();
        g.fill(x() - 1, top, x(), top + height, GuiTheme.border);
        if (font.width("Crit's Tier Tagger") <= rail - 20) {
            g.text(font, "Crit's Tier Tagger", left + 10, top + 16, GuiTheme.text, false);
        } else {
            g.text(font, "Crit's", left + 10, top + 12, GuiTheme.text, false);
            g.text(font, "Tier Tagger", left + 10, top + 25, GuiTheme.text, false);
        }
        float reveal = !GuiTheme.animations() ? 1 : Math.min(1, (System.nanoTime() - appearedAt) / 180_000_000F);
        int headingColor = ((int) (255 * (0.85F + 0.15F * reveal)) << 24) | (GuiTheme.text & 0xFFFFFF);
        g.text(font, heading(page), x() + 14, top + 11, headingColor, false);
        g.fill(x(), top + 32, left + width, top + 33, GuiTheme.border);
    }
}
