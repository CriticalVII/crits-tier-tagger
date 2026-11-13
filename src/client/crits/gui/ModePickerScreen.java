package crits.gui;

import crits.CritsTierTaggerClient;
import crits.render.ModeIcons;
import crits.ranking.ModeIcon;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Provider-supported modes remain selectable before rankings have downloaded. */
public final class ModePickerScreen extends Screen {
    private final TaggerSettingsScreen owner;
    private final String selected;
    private int x, y, w, h;

    ModePickerScreen(TaggerSettingsScreen owner, String selected) {
        super(net.minecraft.client.Minecraft.getInstance(), TaggerFonts.body(), Component.literal("Choose a mode"));
        this.owner = owner;
        this.selected = selected;
    }

    @Override protected void init() {
        w = Math.min(520, width - 20); h = Math.min(348, height - 16);
        x = (width - w) / 2; y = (height - h) / 2;
        var available = CritsTierTaggerClient.instance().repository().state().rankings().modes();
        var provider = CritsTierTaggerClient.instance().selectedProvider();
        Set<String> modes = new LinkedHashSet<>();
        modes.add("strongest");
        modes.addAll(provider.modes());
        modes.addAll(available);
        // Alias IDs refer to one icon/mode, so a service cannot produce duplicate choices.
        var seen = new java.util.HashSet<Object>();
        modes.removeIf(mode -> !seen.add(ModeIcon.find(mode).<Object>map(icon -> icon).orElse(mode)));
        int columns = w >= 480 ? 4 : w >= 360 ? 3 : 2;
        int cellWidth = (w - 36 - (columns - 1) * 6) / columns;
        LinearLayout list = LinearLayout.vertical().spacing(6);
        LinearLayout row = null;
        int index = 0;
        for (String mode : modes) {
            if (index++ % columns == 0) { row = LinearLayout.horizontal().spacing(6); list.addChild(row); }
            String name = provider.modeLabel(mode);
            int buttonWidth = cellWidth;
            var button = new TaggerWidgets.Tile(name, buttonWidth, 60, isSelected(mode),
                    ModeIcons.sprite(ModeIcon.forMode(mode)).spriteId(), pressed -> {
                owner.selectMode(mode); onClose();
            });
            button.setMessage(Component.empty().append(ModeIcons.component(mode)).append(" " + name));
            button.setTooltip(TaggerFonts.tooltip(Component.literal(mode.equals("strongest")
                    ? "Each player's best eligible tier across all modes" : "Display " + name + " tiers only")));
            row.addChild(button);
        }
        var scroll = new ScrollableLayout(minecraft, list, Math.max(32, h - 102), ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.setMinWidth(w - 24); scroll.arrangeElements(); scroll.setX(x + 12); scroll.setY(y + 58);
        scroll.visitWidgets(this::addRenderableWidget);
        var back = TaggerWidgets.action("Back", 80, false, pressed -> onClose());
        back.setPosition(x + w - 92, y + h - 30); addRenderableWidget(back);
    }

    private boolean isSelected(String mode) {
        return selected.equals(mode) || !selected.equals("strongest") && ModeIcon.find(selected).isPresent()
                && ModeIcon.find(selected).equals(ModeIcon.find(mode));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        TaggerWidgets.backdrop(g, width, height);
        TaggerWidgets.panel(g, x, y, w, h);
        g.text(font, TaggerFrame.heading("Choose a mode"), x + 12, y + 12, TaggerWidgets.TEXT, false);
        g.fill(x + 12, y + 29, x + w - 12, y + 30, TaggerWidgets.LINE);
        g.text(font, "One mode, or everyone's strongest tier.", x + 12, y + 34, TaggerWidgets.MUTED, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    @Override public void onClose() { minecraft.gui.setScreen(owner); }
    @Override public boolean isPauseScreen() { return false; }
}
