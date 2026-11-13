package dev.crits.tiertagger.client.mixin;

import dev.crits.tiertagger.client.CritsTierTaggerClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Proxy transfers and same-world practice teleports need not establish a new connection. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void afritiers$respawn(ClientboundRespawnPacket packet, CallbackInfo callback) {
        CritsTierTaggerClient mod = CritsTierTaggerClient.instance();
        if (mod != null) mod.requestWorldRefresh();
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void afritiers$teleport(ClientboundPlayerPositionPacket packet, CallbackInfo callback) {
        CritsTierTaggerClient mod = CritsTierTaggerClient.instance();
        if (mod != null) mod.requestWorldRefresh();
    }
}
