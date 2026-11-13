package crits.render;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4fc;

/** Marks only our Outfit glyphs for linear GUI sampling, preserving metrics and hit areas. */
public final class SmoothTextSource implements GlyphSource {
    private static final FontDescription BODY = new FontDescription.Resource(Identifier.fromNamespaceAndPath("afritiers_tagger", "ui_body"));
    private static final FontDescription HEADING = new FontDescription.Resource(Identifier.fromNamespaceAndPath("afritiers_tagger", "ui"));
    private final GlyphSource original;
    private final it.unimi.dsi.fastutil.ints.Int2ObjectMap<Cached> glyphs = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
    private record Cached(BakedGlyph original, BakedGlyph smooth) { }

    public SmoothTextSource(GlyphSource original) { this.original = original; }
    public static boolean isUiFont(FontDescription font) { return BODY.equals(font) || HEADING.equals(font); }
    public static boolean isSmooth(TextRenderable glyph) { return glyph instanceof SmoothGlyph; }

    @Override public BakedGlyph getGlyph(int codepoint) {
        BakedGlyph current = original.getGlyph(codepoint);
        Cached cached = glyphs.get(codepoint);
        // FontSet sources survive resource reloads; refresh wrappers when their baked glyph changes.
        if (cached == null || cached.original != current) {
            cached = new Cached(current, wrap(current));
            glyphs.put(codepoint, cached);
        }
        return cached.smooth;
    }

    @Override public BakedGlyph getRandomGlyph(RandomSource random, int width) { return wrap(original.getRandomGlyph(random, width)); }

    private static BakedGlyph wrap(BakedGlyph original) {
        return new BakedGlyph() {
            @Override public GlyphInfo info() { return original.info(); }
            @Override public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor,
                    Style style, float boldOffset, float shadowOffset) {
                var glyph = original.createGlyph(x, y, color, shadowColor, style, boldOffset, shadowOffset);
                return glyph == null ? null : new SmoothGlyph(glyph);
            }
        };
    }

    private record SmoothGlyph(TextRenderable.Styled original) implements TextRenderable.Styled {
        @Override public Style style() { return original.style(); }
        @Override public float left() { return original.left(); }
        @Override public float top() { return original.top(); }
        @Override public float right() { return original.right(); }
        @Override public float bottom() { return original.bottom(); }
        @Override public float activeLeft() { return original.activeLeft(); }
        @Override public float activeTop() { return original.activeTop(); }
        @Override public float activeRight() { return original.activeRight(); }
        @Override public float activeBottom() { return original.activeBottom(); }
        @Override public void render(Matrix4fc pose, VertexConsumer buffer, int light, boolean flat) { original.render(pose, buffer, light, flat); }
        @Override public RenderType renderType(Font.DisplayMode mode) { return original.renderType(mode); }
        @Override public GpuTextureView textureView() { return original.textureView(); }
        @Override public RenderPipeline guiPipeline() { return original.guiPipeline(); }
    }
}
