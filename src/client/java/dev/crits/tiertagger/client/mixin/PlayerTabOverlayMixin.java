package dev.crits.tiertagger.client.mixin;

import dev.crits.tiertagger.client.TagFormatter;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void afritiers$decorateName(PlayerInfo info, CallbackInfoReturnable<Component> callback) {
        callback.setReturnValue(TagFormatter.decorate(info.getProfile().name(), callback.getReturnValue(), true));
    }
}
