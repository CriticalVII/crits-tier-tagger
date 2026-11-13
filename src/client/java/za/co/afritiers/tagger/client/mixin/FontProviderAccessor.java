package za.co.afritiers.tagger.client.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access lets our screens use a separate font without changing Minecraft's HUD font. */
@Mixin(Font.class)
public interface FontProviderAccessor {
    @Accessor("provider") Font.Provider crits$getProvider();
}
