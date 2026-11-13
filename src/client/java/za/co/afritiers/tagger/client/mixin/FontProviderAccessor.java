package za.co.afritiers.tagger.client.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Read-only access lets our screens use a separate font without changing Minecraft's HUD font. */
@Mixin(Font.class)
public interface FontProviderAccessor {
    @Invoker("getGlyphSource") net.minecraft.client.gui.GlyphSource crits$getGlyphSource(net.minecraft.network.chat.FontDescription font);
    @Accessor("provider") Font.Provider crits$getProvider();
}
