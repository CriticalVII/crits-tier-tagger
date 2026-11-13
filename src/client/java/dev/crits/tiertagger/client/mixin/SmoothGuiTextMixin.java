package dev.crits.tiertagger.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.crits.tiertagger.client.render.SmoothTextSource;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Select a sampler per GUI glyph; vanilla text, HUD labels and sprites stay pixel-sharp. */
@Mixin(GlyphRenderState.class)
public abstract class SmoothGuiTextMixin {
    @Shadow public abstract TextRenderable renderable();

    @Inject(method = "textureSetup", at = @At("HEAD"), cancellable = true)
    private void crits$smoothOwnText(CallbackInfoReturnable<TextureSetup> callback) {
        var glyph = renderable();
        if (SmoothTextSource.isSmooth(glyph)) callback.setReturnValue(TextureSetup.singleTextureWithLightmap(
                glyph.textureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)));
    }
}
