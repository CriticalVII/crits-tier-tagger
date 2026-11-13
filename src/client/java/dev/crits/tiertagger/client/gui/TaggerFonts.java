package dev.crits.tiertagger.client.gui;

import dev.crits.tiertagger.client.mixin.FontProviderAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/** Screen-local Outfit text; explicit icon fonts still use their original glyph sources. */
final class TaggerFonts {
    private static final FontDescription BODY = new FontDescription.Resource(Identifier.fromNamespaceAndPath("afritiers_tagger", "ui_body"));
    private static Font base, body;
    private TaggerFonts() { }
    static net.minecraft.client.gui.components.Tooltip tooltip(net.minecraft.network.chat.Component text) {
        return net.minecraft.client.gui.components.Tooltip.create(text.copy().withStyle(style -> style.withFont(BODY)));
    }
    static Font body() {
        Font original = Minecraft.getInstance().font;
        if (body == null || base != original) {
            base = original;
            var delegate = ((FontProviderAccessor) (Object) original).crits$getProvider();
            body = new Font(new Font.Provider() {
                @Override public GlyphSource glyphs(FontDescription font) {
                    if (FontDescription.DEFAULT.equals(font)) return ((FontProviderAccessor) (Object) original).crits$getGlyphSource(BODY);
                    return delegate.glyphs(font);
                }
                @Override public EffectGlyph effect() { return delegate.effect(); }
            });
        }
        return body;
    }
}
