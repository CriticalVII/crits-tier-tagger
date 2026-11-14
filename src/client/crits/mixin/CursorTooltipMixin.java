package crits.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cursor positioning for our own menus; vanilla keyboard placement is preserved. */
@Mixin(WidgetTooltipHolder.class)
public abstract class CursorTooltipMixin {
    @Inject(method = "refreshTooltipForNextRenderPass", at = @At("HEAD"), cancellable = true)
    private void crits$hideControls(net.minecraft.client.gui.GuiGraphicsExtractor g, int x, int y, boolean hovered, boolean focused, ScreenRectangle bounds, org.spongepowered.asm.mixin.injection.callback.CallbackInfo result) {
        if (Minecraft.getInstance().gui.screen() instanceof crits.gui.TaggerScreen) result.cancel();
    }
    @Inject(method = "createTooltipPositioner", at = @At("HEAD"), cancellable = true)
    private void crits$cursor(ScreenRectangle bounds, boolean hovered, boolean focused, CallbackInfoReturnable<ClientTooltipPositioner> result) {
        if (hovered && Minecraft.getInstance().gui.screen() instanceof crits.gui.TaggerScreen)
            result.setReturnValue(DefaultTooltipPositioner.INSTANCE);
    }
}
