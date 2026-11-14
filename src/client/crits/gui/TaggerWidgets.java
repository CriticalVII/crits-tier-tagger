package crits.gui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Pixel-aligned native widgets retain vanilla input, focus, narration and click sounds. */
final class TaggerWidgets {
    private TaggerWidgets() { }

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        surface(g, x, y, w, h, 8, GuiTheme.translucent(GuiTheme.background), GuiTheme.border);
    }

    static void backdrop(GuiGraphicsExtractor g, int width, int height) {
        var client = Minecraft.getInstance();
        if (client.level == null) client.gameRenderer.panorama().extractRenderState(g, width, height);
        if (GuiTheme.blur() && client.options.getMenuBackgroundBlurriness() >= 1) g.blurBeforeThisStratum();
        g.fill(0, 0, width, height, 0x50000000);
    }

    static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int colour) {
        roundedExact(g, x, y, w, h, GuiTheme.radius(radius), colour);
    }
    private static void roundedExact(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int colour) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        g.fill(x, y + r, x + w, y + h - r, colour);
        for (int row = 0; row < r; row++) {
            int inset = (int) Math.ceil(r - Math.sqrt(r * r - Math.pow(r - row - 0.5, 2)));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, colour);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, colour);
        }
    }

    static void surface(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int fill, int border) {
        surfaceExact(g, x, y, w, h, GuiTheme.radius(r), fill, border);
    }
    /** Gallery cards preview their own corners, independent of the active workspace. */
    static void surfaceExact(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int fill, int border) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        roundedExact(g, x, y, w, h, r, fill);
        if (r == 0) { g.outline(x, y, w, h, border); return; }
        // Draw only the border ring: an opaque undercoat would hide glass transparency.
        g.fill(x, y + r, x + 1, y + h - r, border);
        g.fill(x + w - 1, y + r, x + w, y + h - r, border);
        for (int row = 0; row < r; row++) {
            int outer = cornerInset(r, row);
            int inner = row == 0 ? w / 2 : 1 + cornerInset(r - 1, row - 1);
            if (row == 0) {
                g.fill(x + outer, y, x + w - outer, y + 1, border);
                g.fill(x + outer, y + h - 1, x + w - outer, y + h, border);
            } else {
                g.fill(x + outer, y + row, x + inner, y + row + 1, border);
                g.fill(x + w - inner, y + row, x + w - outer, y + row + 1, border);
                g.fill(x + outer, y + h - row - 1, x + inner, y + h - row, border);
                g.fill(x + w - inner, y + h - row - 1, x + w - outer, y + h - row, border);
            }
        }
    }
    private static int cornerInset(int radius, int row) {
        return (int) Math.ceil(radius - Math.sqrt(radius * radius - Math.pow(radius - row - 0.5, 2)));
    }

    static Action action(String text, int width, boolean primary, Button.OnPress action) {
        return new Action(text, width, 20, primary, action);
    }

    /** Vanilla StringWidget's active-text collector uses the global font when drawing. */
    static final class Label extends net.minecraft.client.gui.components.StringWidget {
        Label(int width, int height, Component message, net.minecraft.client.gui.Font font) {
            super(width, height, message, font);
        }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            var font = TaggerFonts.body();
            var text = font.width(getMessage()) > getWidth()
                    ? net.minecraft.client.gui.components.ComponentRenderUtils.clipText(getMessage(), font, getWidth()) : getMessage().getVisualOrderText();
            g.text(font, text, getX(), getY() + (getHeight() - 9) / 2, GuiTheme.text, false);
        }
    }

    static class Action extends Button {
        private final boolean primary;
        private float hover;
        private long lastFrame;
        Action(String text, int width, int height, boolean primary, OnPress action) {
            super(0, 0, width, height, Component.literal(text), action, DEFAULT_NARRATION);
            this.primary = primary;
        }
        final float hoverAmount() {
            long now = System.nanoTime();
            float target = isHoveredOrFocused() && active ? 1 : 0;
            if (!GuiTheme.animations() || lastFrame == 0) hover = target;
            else hover += (target - hover) * Math.min(1, (now - lastFrame) / 100_000_000F);
            lastFrame = now;
            return hover;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            extractBackground(g);
            var font = TaggerFonts.body();
            g.text(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, active ? primary ? GuiTheme.accent : GuiTheme.text : GuiTheme.muted, false);
        }
        protected void extractBackground(GuiGraphicsExtractor g) {
            float over = hoverAmount();
            int fill = primary ? mix(GuiTheme.selection, GuiTheme.blend(GuiTheme.selection, GuiTheme.accent, 0.10F), over) : mix(GuiTheme.surface, GuiTheme.hover, over);
            if (!active) fill = GuiTheme.background;
            surface(g, getX(), getY(), width, height, 6, fill, isFocused() ? GuiTheme.accent : primary && active ? GuiTheme.accent : GuiTheme.border);
        }
    }

    static final class Tile extends Action {
        private final String title;
        private final boolean selected;
        private final net.minecraft.resources.Identifier sprite;
        Tile(String title, int width, int height, boolean selected, net.minecraft.resources.Identifier sprite, OnPress action) {
            super(title, width, height, false, action);
            this.title = title; this.selected = selected; this.sprite = sprite;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            float hover = hoverAmount();
            int lift = !GuiTheme.animations() ? 0 : Math.round(hover);
            surface(g, getX(), getY() + 1 - lift, width, height - 1, 3,
                    selected ? GuiTheme.selection : mix(GuiTheme.surface, GuiTheme.hover, hover),
                    selected || isFocused() ? GuiTheme.accent : GuiTheme.surface);
            var font = TaggerFonts.body();
            String text = font.plainSubstrByWidth(title, width - 12);
            g.text(font, text, getX() + (width - font.width(text)) / 2, getY() + 7 - lift, GuiTheme.text, false);
            int size = Math.min(36, height - 25);
            g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite,
                    getX() + (width - size) / 2, getY() + 21 - lift, size, size);
            if (selected) g.outline(getRight() - 11, getBottom() - 10 - lift, 5, 5, GuiTheme.text);
        }
        @Override public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            super.updateWidgetNarration(output);
            if (selected) output.add(net.minecraft.client.gui.narration.NarratedElementType.USAGE, Component.literal("Selected"));
        }
    }

    static final class Tab extends Action {
        private final boolean selected;
        Tab(String label, boolean selected, OnPress action) {
            super(label, 64, 18, selected, action);
            this.selected = selected;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            float hover = hoverAmount();
            if (selected || hover > 0) rounded(g, getX(), getY(), width, height, 6, selected ? GuiTheme.selection : mix(GuiTheme.sidebar, GuiTheme.hover, hover));
            if (isFocused()) g.outline(getX(), getY(), width, height, GuiTheme.accent);
            var font = TaggerFonts.body();
            g.text(font, getMessage(), getX() + 10, getY() + (height - 8) / 2, selected ? GuiTheme.text : GuiTheme.muted, false);
        }
    }

    static final class Input extends EditBox implements MenuTextInput {
        private Component hint;
        private boolean centred;
        private boolean invalid;
        void setInvalid(boolean value) { invalid = value; }
        Input(int width, int height, Component label) {
            super(TaggerFonts.body(), width, height, label);
            setBordered(false);
            setTextColor(GuiTheme.text);
            setTextShadow(false);
        }
        @Override public int menuTextY() { return getY() + Math.max(0, (height - 10) / 2); }
        @Override public void setCentered(boolean centred) { this.centred = centred; super.setCentered(centred); }
        @Override public void setHint(Component hint) { this.hint = hint; }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            surface(g, getX() - 4, getY() - 3, width + 6, height + 4, 3, GuiTheme.background, isFocused() ? GuiTheme.accent : GuiTheme.border);
            setTextColor(invalid ? 0xFFE08A85 : GuiTheme.text);
            super.extractWidgetRenderState(g, mouseX, mouseY, delta);
            if (visible && hint != null && getValue().isEmpty() && !isFocused()) {
                var font = TaggerFonts.body();
                String text = font.plainSubstrByWidth(hint.getString(), width - 8);
                g.text(font, Component.literal(text).withStyle(hint.getStyle()).withColor(GuiTheme.muted),
                        centred ? getX() + (width - font.width(text)) / 2 : getX(), menuTextY(), GuiTheme.muted, false);
            }
        }
    }

    static final class Toggle extends Action {
        private final String label, description;
        private final BooleanSupplier value;
        private Component icon;
        void setIcon(Component icon) { this.icon = icon; }
        Toggle(String label, String description, int width, boolean compact, BooleanSupplier value, Consumer<Boolean> change) {
            super(label + ": " + (value.getAsBoolean() ? "ON" : "OFF"), width, compact ? 26 : 34, false, button -> {
                change.accept(!value.getAsBoolean());
                button.setMessage(Component.literal(label + ": " + (value.getAsBoolean() ? "ON" : "OFF")));
            });
            this.label = label; this.description = description; this.value = value;
            setTooltip(TaggerFonts.tooltip(Component.literal(label + ": " + description)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            row(g, this, hoverAmount());
            var font = TaggerFonts.body();
            int labelY = getY() + (height < 34 ? 9 : 6);
            if (icon != null) g.text(font, icon, getX() + 7, labelY, GuiTheme.text, false);
            g.text(font, font.plainSubstrByWidth(label, width - (icon == null ? 48 : 64)), getX() + (icon == null ? 8 : 24), labelY, GuiTheme.text, false);
            if (height >= 34) g.text(font, font.plainSubstrByWidth(description, width - 16), getX() + 8, getY() + 21, GuiTheme.muted, false);
            int right = getRight() - 7;
            boolean on = value.getAsBoolean();
            int chipY = getY() + (height < 34 ? 7 : 5);
            rounded(g, right - 22, chipY + 1, 22, 11, 5, on ? GuiTheme.selection : GuiTheme.border);
            rounded(g, on ? right - 10 : right - 20, chipY + 3, 7, 7, 3, on ? GuiTheme.accent : GuiTheme.muted);
        }
    }

    static final class Mode extends Action {
        private final Supplier<Component> value;
        Mode(int width, boolean compact, Supplier<Component> value, OnPress action) {
            super("Ranking mode", width, compact ? 26 : 34, false, button -> {
                action.onPress(button);
                button.setMessage(Component.literal("Ranking mode: ").append(value.get()));
            });
            this.value = value;
            setMessage(Component.literal("Ranking mode: ").append(value.get()));
            setTooltip(TaggerFonts.tooltip(Component.literal("Choose from every mode. Strongest tier selects each player's best eligible ranking.")));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            row(g, this, hoverAmount());
            var font = TaggerFonts.body();
            if (height >= 34) g.text(font, "Ranking mode", getX() + 8, getY() + 5, GuiTheme.muted, false);
            int textY = getY() + (height < 34 ? 9 : 21);
            g.enableScissor(getX() + 7, textY - 2, getRight() - 19, getBottom());
            g.text(font, value.get(), getX() + 8, textY, GuiTheme.text, false);
            g.disableScissor();
            g.text(font, ">", getRight() - 13, textY, GuiTheme.muted, false);
        }
    }

    private static void row(GuiGraphicsExtractor g, Button widget, float hover) {
        surface(g, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 6,
                mix(GuiTheme.surface, GuiTheme.hover, hover), widget.isFocused() ? GuiTheme.accent : GuiTheme.border);
    }
    private static int mix(int from, int to, float amount) {
        int r = (int) (((from >> 16) & 255) * (1 - amount) + ((to >> 16) & 255) * amount);
        int g = (int) (((from >> 8) & 255) * (1 - amount) + ((to >> 8) & 255) * amount);
        int b = (int) ((from & 255) * (1 - amount) + (to & 255) * amount);
        int a = (int) ((from >>> 24) * (1 - amount) + (to >>> 24) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
