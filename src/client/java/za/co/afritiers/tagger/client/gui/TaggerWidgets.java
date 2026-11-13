package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Pixel-aligned native widgets retain vanilla input, focus, narration and click sounds. */
final class TaggerWidgets {
    static final int TEXT = 0xFFF2F2F2, MUTED = 0xFFA0A0A0, FAINT = 0xFF727272,
            GREEN = 0xFF38EA83, CYAN = 0xFF03C9E2, BLUE = 0xFF356DFF,
            LINE = 0xFF282828, INK = 0xFF101010, SURFACE = 0xFF232323;
    private TaggerWidgets() { }

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        surface(g, x, y, w, h, 4, 0xFF0E0E0E, LINE);
    }

    static void backdrop(GuiGraphicsExtractor g, int width, int height) {
        g.fill(0, 0, width, height, 0xFF0A0A0A);
    }

    static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int colour) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        g.fill(x, y + r, x + w, y + h - r, colour);
        for (int row = 0; row < r; row++) {
            int inset = (int) Math.ceil(r - Math.sqrt(r * r - Math.pow(r - row - 0.5, 2)));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, colour);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, colour);
        }
    }

    static void surface(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int fill, int border) {
        rounded(g, x, y, w, h, r, border);
        rounded(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    static Action action(String text, int width, boolean primary, Button.OnPress action) {
        return new Action(text, width, 20, primary, action);
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
            if (Minecraft.getInstance().options.screenEffectScale().get() == 0 || lastFrame == 0) hover = target;
            else hover += (target - hover) * Math.min(1, (now - lastFrame) / 100_000_000F);
            lastFrame = now;
            return hover;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            extractBackground(g);
            var font = TaggerFonts.body();
            g.text(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, active ? primary ? GREEN : TEXT : FAINT, false);
        }
        protected void extractBackground(GuiGraphicsExtractor g) {
            float over = hoverAmount();
            int fill = primary ? mix(0xFF112A1C, 0xFF173C27, over) : mix(0xFF151515, 0xFF272727, over);
            if (!active) fill = 0xFF101010;
            surface(g, getX(), getY(), width, height, 3, fill, isFocused() ? CYAN : primary && active ? 0xFF1C422B : LINE);
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
            int lift = Minecraft.getInstance().options.screenEffectScale().get() == 0 ? 0 : Math.round(hover);
            surface(g, getX(), getY() + 1 - lift, width, height - 1, 3,
                    selected ? 0xFF10152D : mix(0xFF242424, 0xFF2D2D2D, hover),
                    selected || isFocused() ? BLUE : 0xFF242424);
            var font = TaggerFonts.body();
            String text = font.plainSubstrByWidth(title, width - 12);
            g.text(font, text, getX() + (width - font.width(text)) / 2, getY() + 7 - lift, TEXT, false);
            int size = Math.min(36, height - 25);
            g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite,
                    getX() + (width - size) / 2, getY() + 21 - lift, size, size);
            if (selected) g.outline(getRight() - 11, getBottom() - 10 - lift, 5, 5, TEXT);
        }
        @Override public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            super.updateWidgetNarration(output);
            if (selected) output.add(net.minecraft.client.gui.narration.NarratedElementType.USAGE, Component.literal("Selected"));
        }
    }

    static final class Tab extends Action {
        private final boolean selected;
        private boolean rail;
        void setRail(boolean value) { rail = value; }
        Tab(String label, boolean selected, OnPress action) {
            super(label, 64, 18, selected, action);
            this.selected = selected;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            float hover = hoverAmount();
            if (selected || hover > 0) rounded(g, getX(), getY(), width, height, 3, selected ? 0xFF252525 : mix(0xFF0B0B0B, 0xFF1C1C1C, hover));
            if (selected || isFocused()) {
                if (rail) { if (isFocused()) g.outline(getX(), getY(), width, height, CYAN); }
                else g.fill(getX() + 5, getBottom() - 1, getRight() - 5, getBottom(), CYAN);
            }
            var font = TaggerFonts.body();
            if (rail) g.text(font, getMessage(), getX() + 10, getY() + (height - 8) / 2, selected ? TEXT : MUTED, false);
            else g.text(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + 4, selected ? TEXT : MUTED, false);
        }
    }

    static final class Input extends EditBox {
        Input(int width, int height, Component label) {
            super(TaggerFonts.body(), width, height, label);
            setBordered(false);
            setTextColor(TEXT);
        }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            surface(g, getX() - 4, getY() - 3, width + 6, height + 4, 3, 0xFF111111, isFocused() ? CYAN : LINE);
            super.extractWidgetRenderState(g, mouseX, mouseY, delta);
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
            if (icon != null) g.text(font, icon, getX() + 7, labelY, TEXT);
            g.text(font, font.plainSubstrByWidth(label, width - (icon == null ? 48 : 64)), getX() + (icon == null ? 8 : 24), labelY, TEXT);
            if (height >= 34) g.text(font, font.plainSubstrByWidth(description, width - 16), getX() + 8, getY() + 21, MUTED);
            int right = getRight() - 7;
            boolean on = value.getAsBoolean();
            int chipY = getY() + (height < 34 ? 7 : 5);
            rounded(g, right - 22, chipY + 1, 22, 11, 5, on ? 0xFF17572E : 0xFF414141);
            rounded(g, on ? right - 10 : right - 20, chipY + 3, 7, 7, 3, on ? GREEN : 0xFFBBBBBB);
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
            if (height >= 34) g.text(font, "Ranking mode", getX() + 8, getY() + 5, MUTED);
            int textY = getY() + (height < 34 ? 9 : 21);
            g.enableScissor(getX() + 7, textY - 2, getRight() - 19, getBottom());
            g.text(font, value.get(), getX() + 8, textY, TEXT);
            g.disableScissor();
            g.text(font, ">", getRight() - 13, textY, MUTED, false);
        }
    }

    private static void row(GuiGraphicsExtractor g, Button widget, float hover) {
        surface(g, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 3,
                mix(0xFF151515, 0xFF202020, hover), widget.isFocused() ? CYAN : LINE);
    }
    private static int mix(int from, int to, float amount) {
        int r = (int) (((from >> 16) & 255) * (1 - amount) + ((to >> 16) & 255) * amount);
        int g = (int) (((from >> 8) & 255) * (1 - amount) + ((to >> 8) & 255) * amount);
        int b = (int) ((from & 255) * (1 - amount) + (to & 255) * amount);
        int a = (int) ((from >>> 24) * (1 - amount) + (to >>> 24) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
