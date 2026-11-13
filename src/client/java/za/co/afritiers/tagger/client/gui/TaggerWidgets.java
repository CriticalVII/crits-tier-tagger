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
    static final int TEXT = 0xFFF1F3F4, MUTED = 0xFFA3AAAF, FAINT = 0xFF727B82,
            GREEN = 0xFF9BCBD8, LINE = 0xFF2D3236, INK = 0xFF141719, SURFACE = 0xFF191D20;
    private TaggerWidgets() { }

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x40000000);
        g.fill(x, y, x + w, y + h, 0xFA111416);
        g.outline(x, y, w, h, LINE);
    }

    static void backdrop(GuiGraphicsExtractor g, int width, int height) {
        g.fill(0, 0, width, height, 0xDA090B0D);
        for (int x = 12; x < width; x += 16) for (int y = 12; y < height; y += 16)
            g.fill(x, y, x + 1, y + 1, 0x182E373D);
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
            var font = Minecraft.getInstance().font;
            g.text(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, active ? primary ? INK : TEXT : FAINT, false);
        }
        protected void extractBackground(GuiGraphicsExtractor g) {
            float over = hoverAmount();
            int fill = primary ? mix(0xFFE6EBED, 0xFFFFFFFF, over) : mix(SURFACE, 0xFF272D31, over);
            if (!active) fill = 0xFF15191C;
            g.fill(getX(), getY(), getRight(), getBottom(), fill);
            g.outline(getX(), getY(), width, height, primary && active ? 0xFFF3F5F6 : LINE);
            if (isFocused()) g.outline(getX(), getY(), width, height, GREEN);
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
            if (selected || hover > 0) g.fill(getX(), getY(), getRight(), getBottom(), selected ? 0xFF242A2E : mix(0xFF121415, 0xFF202629, hover));
            if (selected || isFocused()) {
                if (rail) g.fill(getX(), getY() + 5, getX() + 2, getBottom() - 5, GREEN);
                else g.fill(getX() + 5, getBottom() - 1, getRight() - 5, getBottom(), GREEN);
            }
            var font = Minecraft.getInstance().font;
            if (rail) g.text(font, getMessage(), getX() + 10, getY() + (height - 8) / 2, selected ? TEXT : MUTED, false);
            else g.text(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + 4, selected ? TEXT : MUTED, false);
        }
    }

    static final class Input extends EditBox {
        Input(int width, int height, Component label) {
            super(Minecraft.getInstance().font, width, height, label);
            setBordered(false);
            setTextColor(TEXT);
        }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            g.fill(getX() - 4, getY() - 3, getRight() + 2, getBottom(), 0xFF0E1113);
            g.outline(getX() - 4, getY() - 3, width + 6, height + 4, isFocused() ? GREEN : LINE);
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
            setTooltip(Tooltip.create(Component.literal(label + ": " + description)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            row(g, this, hoverAmount());
            var font = Minecraft.getInstance().font;
            int labelY = getY() + (height < 34 ? 9 : 6);
            if (icon != null) g.text(font, icon, getX() + 7, labelY, TEXT);
            g.text(font, font.plainSubstrByWidth(label, width - (icon == null ? 48 : 64)), getX() + (icon == null ? 8 : 24), labelY, TEXT);
            if (height >= 34) g.text(font, font.plainSubstrByWidth(description, width - 16), getX() + 8, getY() + 21, MUTED);
            int right = getRight() - 7;
            boolean on = value.getAsBoolean();
            int chipY = getY() + (height < 34 ? 7 : 5);
            g.fill(right - 27, chipY, right, chipY + 13, on ? 0xFFB6D7DF : 0xFF30373C);
            String state = on ? "ON" : "OFF";
            g.text(font, state, right - 13 - font.width(state) / 2, chipY + 3, on ? INK : MUTED, false);
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
            setTooltip(Tooltip.create(Component.literal("Choose from every mode. Strongest tier selects each player's best eligible ranking.")));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            row(g, this, hoverAmount());
            var font = Minecraft.getInstance().font;
            if (height >= 34) g.text(font, "Ranking mode", getX() + 8, getY() + 5, MUTED);
            int textY = getY() + (height < 34 ? 9 : 21);
            g.enableScissor(getX() + 7, textY - 2, getRight() - 19, getBottom());
            g.text(font, value.get(), getX() + 8, textY, TEXT);
            g.disableScissor();
            g.text(font, ">", getRight() - 13, textY, MUTED, false);
        }
    }

    private static void row(GuiGraphicsExtractor g, Button widget, float hover) {
        g.fill(widget.getX(), widget.getY(), widget.getRight(), widget.getBottom(), mix(SURFACE, 0xFF242B30, hover));
        g.outline(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), LINE);
        if (widget.isFocused()) g.outline(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), GREEN);
    }
    private static int mix(int from, int to, float amount) {
        int r = (int) (((from >> 16) & 255) * (1 - amount) + ((to >> 16) & 255) * amount);
        int g = (int) (((from >> 8) & 255) * (1 - amount) + ((to >> 8) & 255) * amount);
        int b = (int) ((from & 255) * (1 - amount) + (to & 255) * amount);
        int a = (int) ((from >>> 24) * (1 - amount) + (to >>> 24) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
