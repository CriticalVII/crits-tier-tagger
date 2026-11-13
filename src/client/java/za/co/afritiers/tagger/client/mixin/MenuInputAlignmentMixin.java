package za.co.afritiers.tagger.client.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import za.co.afritiers.tagger.client.MenuTextInput;

/** Keep native caret, selection, click handling and IME positioning on the same baseline. */
@Mixin(EditBox.class)
public abstract class MenuInputAlignmentMixin {
    @Shadow private int textY;
    @Inject(method = "updateTextPosition", at = @At("RETURN"))
    private void crits$centreMenuText(CallbackInfo callback) {
        if ((Object) this instanceof MenuTextInput input) textY = input.menuTextY();
    }
}
