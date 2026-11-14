package crits.gui;

import crits.CritsTierTaggerClient;
import crits.config.AppearanceConfig;
import crits.config.TaggerConfig;
import crits.config.ThemePreset;
import crits.render.ModeIcons;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Searchable native theme gallery and independent tier/palette editors. Everything saves immediately. */
public final class AppearanceScreen extends TaggerScreen {
    private static final Gson GSON = new Gson();
    private static final String[] ACCENTS = {"8AABFF", "D6AEED", "F0B9DD", "E9A0AF", "E4B379", "E6CF8D", "ACD39B", "8DDEC9", "88C0D0", "FFFFFF"};
    private final TaggerSettingsScreen owner;
    private TaggerFrame frame;
    private String page = "Themes", filter = "", feedback = "";
    private int tierGroup = 1;
    private boolean refilter;
    private TaggerWidgets.Input search;
    private AbstractScrollArea scrollArea;
    private final java.util.Map<String, Double> scrollPositions = new java.util.HashMap<>();
    private int x, y, w, h;

    public AppearanceScreen(TaggerSettingsScreen owner) {
        super(net.minecraft.client.Minecraft.getInstance(), TaggerFonts.body(), Component.literal("Appearance settings"));
        this.owner = owner;
    }
    @Override protected void init() {
        frame = TaggerFrame.fit(width, height); x = frame.x(); y = frame.y(); w = frame.w(); h = frame.h();
        TaggerTabs.add(this::addRenderableWidget, owner, "Settings", frame);
        String[] pages = {"Themes", "Interface", "Tier colours", "Custom"};
        int columns = w < 330 ? 2 : 4;
        int cell = (w - 24 - (columns - 1) * 4) / columns;
        for (int i = 0; i < pages.length; i++) {
            String label = pages[i];
            var tab = new TaggerWidgets.Action(label, cell, 22, page.equals(label), pressed -> {
                if (page.equals(label)) return;
                rememberScroll(); page = label; feedback = ""; rebuildWidgets();
            });
            tab.setPosition(x + 12 + i % columns * (cell + 4), y + 42 + i / columns * 26); addRenderableWidget(tab);
        }
        int contentWidth = w - 34;
        var rows = LinearLayout.vertical().spacing(8);
        int scrollTop = columns == 2 ? 100 : 74;
        if (page.equals("Themes")) {
            search = new TaggerWidgets.Input(w - 32, 18, Component.literal("Search themes"));
            search.setMaxLength(40); search.setValue(filter); search.setHint(Component.literal("Search themes..."));
            search.setResponder(value -> { filter = value; refilter = true; });
            search.setPosition(x + 16, y + scrollTop + 2); addRenderableWidget(search);
            scrollTop += 28;
            themes(rows, contentWidth);
        } else if (page.equals("Interface")) interfaceOptions(rows, contentWidth);
        else if (page.equals("Tier colours")) tiers(rows, contentWidth);
        else custom(rows, contentWidth);
        var scroll = new ScrollableLayout(minecraft, rows, Math.max(32, h - scrollTop - 40), ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.setMinWidth(contentWidth); scroll.arrangeElements(); scroll.setX(x + 12); scroll.setY(y + scrollTop);
        scroll.visitWidgets(widget -> {
            addRenderableWidget(widget);
            if (widget instanceof AbstractScrollArea area) {
                scrollArea = area; area.setScrollAmount(scrollPositions.getOrDefault(page, 0D));
            }
        });
        var done = TaggerWidgets.action("Done", 60, true, pressed -> onClose());
        done.setPosition(x + w - 72, y + h - 28); addRenderableWidget(done);
    }

    private void themes(LinearLayout rows, int width) {
        int columns = width >= 600 ? 3 : width >= 300 ? 2 : 1;
        int cell = (width - 8 * (columns - 1)) / columns;
        int index = 0; LinearLayout row = null;
        var prefs = config().appearance;
        for (var preset : ThemePreset.values()) {
            if (!(preset.label + " " + preset.description).toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))) continue;
            if (index++ % columns == 0) { row = LinearLayout.horizontal().spacing(8); rows.addChild(row); }
            row.addChild(new ThemeCard(preset, cell, preset.id.equals(prefs.theme) && !prefs.customPalette, pressed -> {
                if (preset.id.equals(config().appearance.theme) && !config().appearance.customPalette) return;
                if (save(c -> {
                    c.appearance.theme = preset.id; c.appearance.accent = ""; c.appearance.customPalette = false;
                    c.appearance.opacity = 60;
                    // Font style is an independent preference.
                    if (preset == ThemePreset.GLASS) c.appearance.transparentGui = true;
                })) rebuildKeepingScroll();
            }));
        }
        if (index == 0) label(rows, width, "No matching themes. Try another name.");
    }

    private void interfaceOptions(LinearLayout rows, int width) {
        var prefs = config().appearance;
        rows.addChild(new TaggerWidgets.Toggle("Transparent GUI", "See the game or panorama through panels", width, false,
                () -> config().appearance.transparentGui, value -> {
                    if (save(c -> c.appearance.transparentGui = value)) rebuildKeepingScroll();
                }));
        var opacity = new PreferenceSlider("Panel opacity", width, prefs.opacity, 60, 100, "%", value -> save(c -> c.appearance.opacity = value));
        opacity.active = prefs.transparentGui;
        rows.addChild(opacity);
        rows.addChild(new TaggerWidgets.Toggle("Pixel Style", "Use Minecraft pixel text and stepped corners", width, false,
                () -> !config().appearance.smoothFont, value -> {
                    if (save(c -> c.appearance.smoothFont = !value)) rebuildKeepingScroll();
                }));
        label(rows, width, "Accent colour");
        colourRow(rows, width, "Accent", prefs.accent, hex(ThemePreset.from(prefs.theme).accent), (c, value) -> c.appearance.accent = value);
        var swatches = LinearLayout.horizontal().spacing(3);
        int swatchWidth = Math.min(22, (width - (ACCENTS.length - 1) * 3) / ACCENTS.length);
        for (var accent : ACCENTS) swatches.addChild(new Swatch(accent, swatchWidth, () -> {
            if (save(c -> c.appearance.accent = accent)) rebuildKeepingScroll();
        }));
        rows.addChild(swatches);
        rows.addChild(new PreferenceSlider("Corner radius", width, prefs.cornerRadius, 0, 8, "", value -> save(c -> c.appearance.cornerRadius = value)));
        rows.addChild(new TaggerWidgets.Toggle("Background blur", "Uses Minecraft's menu blur setting", width, false,
                () -> config().appearance.blur, value -> save(c -> c.appearance.blur = value)));
        rows.addChild(new TaggerWidgets.Toggle("Animations", "Subtle hover and heading movement", width, false,
                () -> config().appearance.animations, value -> save(c -> c.appearance.animations = value)));
        rows.addChild(TaggerWidgets.action("Reset appearance", width, false, pressed -> {
            if (save(c -> c.appearance = new AppearanceConfig())) rebuildKeepingScroll();
        }));
    }

    private void tiers(LinearLayout rows, int width) {
        label(rows, width, "Your tier palette");
        label(rows, width, "Used in profiles, previews, Tab and nametags.");
        for (int number = 1; number <= 5; number++) {
            int group = number;
            rows.addChild(TaggerWidgets.action("Tier " + number, width, tierGroup == number, pressed -> {
                tierGroup = tierGroup == group ? 0 : group; rebuildKeepingScroll();
            }));
            if (tierGroup != number) continue;
            for (String prefix : new String[]{"HT", "LT"}) {
                String key = prefix + number;
                colourRow(rows, width, key, config().tierColours.getOrDefault(key, ""), hex(config().tierColour(key)), (c, value) -> {
                    if (value.isEmpty()) c.tierColours.remove(key); else c.tierColours.put(key, value);
                });
            }
        }
        rows.addChild(new TierPreview(width));
        label(rows, width, "Colour sets");
        String[][] palettes = {
                {"Classic", "FFD700", "C0C0C0", "CD7F32", "B58AE8", "B58AE8"},
                {"Pastel", "F1D48A", "CAD8E6", "DDAA8D", "CAA8E8", "E7B3D4"},
                {"Ocean", "F4D58D", "ABD3EB", "6FB5C7", "8C9CE1", "BBA2DD"},
                {"Ember", "FFD17A", "D7C7B4", "E59B73", "C78DAB", "AC99D2"}
        };
        for (var palette : palettes) rows.addChild(TaggerWidgets.action(palette[0] + " colours", width, false, pressed -> {
            if (save(c -> { c.tierColours.clear(); for (int n = 1; n <= 5; n++) c.tierColours.put("" + n, palette[n]); })) rebuildKeepingScroll();
        }));
        colourRow(rows, width, "All tags override", config().customColour, "Tier colours", (c, value) -> c.customColour = value);
        label(rows, width, "Blank uses each tier's colour. Icons stay original.");
        rows.addChild(TaggerWidgets.action("Reset tier colours", width, false, pressed -> {
            if (save(c -> { c.tierColours.clear(); c.customColour = ""; })) rebuildKeepingScroll();
        }));
    }

    private void custom(LinearLayout rows, int width) {
        label(rows, width, "Build on " + ThemePreset.from(config().appearance.theme).label);
        label(rows, width, "Blank fields follow the base theme.");
        rows.addChild(new TaggerWidgets.Toggle("Custom palette", "Enable your surface and text colours", width, false,
                () -> config().appearance.customPalette, value -> save(c -> c.appearance.customPalette = value)));
        var p = ThemePreset.from(config().appearance.theme);
        String[] keys = {"background", "sidebar", "surface", "text", "muted", "border"};
        int[] colours = {p.background, p.sidebar, p.surface, p.text, p.muted, p.border};
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            colourRow(rows, width, Character.toUpperCase(key.charAt(0)) + key.substring(1),
                    config().appearance.palette.getOrDefault(key, ""), hex(colours[i]), (c, value) -> {
                        if (value.isEmpty()) c.appearance.palette.remove(key); else c.appearance.palette.put(key, value);
                    });
        }
        label(rows, width, "Keep text and panels clearly distinguishable.");
        rows.addChild(TaggerWidgets.action("Copy theme", width, false, pressed -> {
            minecraft.keyboardHandler.setClipboard(GSON.toJson(config().appearance)); feedback = "Theme copied · no account data included";
        }));
        rows.addChild(TaggerWidgets.action("Paste theme", width, false, pressed -> {
            try {
                String json = minecraft.keyboardHandler.getClipboard();
                if (json.length() > 4096 || !json.stripLeading().startsWith("{")) throw new IllegalArgumentException();
                var imported = GSON.fromJson(json, AppearanceConfig.class);
                if (imported == null) throw new IllegalArgumentException();
                imported.validate();
                if (save(c -> c.appearance = imported)) rebuildKeepingScroll();
            } catch (JsonParseException | IllegalArgumentException exception) { feedback = "Clipboard does not contain a valid theme"; }
        }));
        rows.addChild(TaggerWidgets.action("Clear custom palette", width, false, pressed -> {
            if (save(c -> { c.appearance.palette.clear(); c.appearance.customPalette = false; })) rebuildKeepingScroll();
        }));
    }

    private interface ColourChange { void accept(TaggerConfig config, String value); }
    private void colourRow(LinearLayout rows, int width, String title, String value, String fallback, ColourChange change) {
        var row = LinearLayout.horizontal().spacing(6);
        row.addChild(new TaggerWidgets.Label(width - 110, 26, Component.literal(title), font));
        String initial = value.isEmpty() ? fallback : value;
        int colour;
        try { colour = Integer.parseInt(initial.replace("#", ""), 16); } catch (NumberFormatException ignored) { colour = 0xFFD700; }
        final int selected = colour;
        var picker = new TaggerWidgets.Action("Choose " + title + " colour", 104, 24, false, pressed -> {
            rememberScroll();
            minecraft.gui.setScreen(new ColourPickerScreen(this, title, selected, chosen -> save(c -> change.accept(c, chosen))));
        }) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
                extractBackground(g);
                g.text(TaggerFonts.body(), "Choose colour", getX() + 20, getY() + 8, GuiTheme.text, false);
                TaggerWidgets.rounded(g, getX() + 5, getY() + 7, 10, 10, 2, GuiTheme.rgb(selected));
            }
        };
        row.addChild(picker); rows.addChild(row);
    }
    private void label(LinearLayout rows, int width, String text) {
        var label = new TaggerWidgets.Label(width, 16, Component.literal(text), font);
        label.setTooltip(TaggerFonts.tooltip(Component.literal(text))); rows.addChild(label);
    }
    private static TaggerConfig config() { return CritsTierTaggerClient.instance().config(); }
    private void rememberScroll() { if (scrollArea != null) scrollPositions.put(page, scrollArea.scrollAmount()); }
    private void rebuildKeepingScroll() { rememberScroll(); rebuildWidgets(); }
    private boolean save(Consumer<TaggerConfig> edit) {
        try {
            var next = config(); edit.accept(next); CritsTierTaggerClient.instance().saveGuiConfig(next);
            feedback = "Saved automatically"; return true;
        } catch (java.io.IOException | IllegalArgumentException exception) {
            feedback = "Could not save · check values and config permissions"; return false;
        }
    }
    private static String hex(int rgb) { return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF); }
    @Override public void tick() {
        if (refilter) {
            int cursor = search.getCursorPosition(); boolean focused = search.isFocused();
            refilter = false; scrollPositions.put(page, 0D); rebuildWidgets(); search.setFocused(focused); search.setCursorPosition(cursor);
            if (focused) setFocused(search);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        frame.draw(g, "Settings");
        g.text(font, font.plainSubstrByWidth(feedback.isEmpty() ? "Changes save automatically" : feedback, w - 96), x + 12, y + h - 21, GuiTheme.muted, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    @Override public void onClose() { minecraft.gui.setScreen(owner); }
    @Override public boolean isPauseScreen() { return false; }

    private static final class ThemeCard extends TaggerWidgets.Action {
        private final ThemePreset preset;
        private final boolean selected;
        ThemeCard(ThemePreset preset, int width, boolean selected, OnPress action) {
            super(preset.label, width, width >= 175 ? 62 : 76, false, action); this.preset = preset; this.selected = selected;
            setMessage(Component.literal(preset.label + (selected ? " · Selected" : "")));
            setTooltip(TaggerFonts.tooltip(Component.literal("Apply " + preset.label)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            int fill = GuiTheme.blend(GuiTheme.rgb(preset.surface), GuiTheme.rgb(preset.text), hoverAmount() * 0.06F);
            if (selected) fill = GuiTheme.blend(fill, GuiTheme.rgb(preset.accent), 0.10F);
            TaggerWidgets.surfaceExact(g, getX(), getY(), width, height, preset == ThemePreset.BLOCK ? 0 : 8, fill,
                    GuiTheme.rgb(selected || isFocused() ? preset.accent : preset.border));
            int px = getX() + 8, py = getY() + 8;
            // Draw the actual preset colours rather than a static screenshot of another theme.
            TaggerWidgets.rounded(g, px, py, 70, 45, 3, GuiTheme.rgb(preset.background));
            g.fill(px + 1, py + 1, px + 16, py + 44, GuiTheme.rgb(preset.sidebar));
            for (int n = 0; n < 5; n++) g.fill(px + 4, py + 9 + n * 6, px + 13, py + 11 + n * 6, GuiTheme.rgb(n == 0 ? preset.accent : preset.muted));
            g.fill(px + 21, py + 5, px + 48, py + 8, GuiTheme.rgb(preset.text));
            for (int n = 0; n < 6; n++) {
                int cx = px + 21 + n % 3 * 15, cy = py + 14 + n / 3 * 14;
                TaggerWidgets.rounded(g, cx, cy, 12, 11, 2, GuiTheme.rgb(preset.surface));
                g.fill(cx, cy + 9, cx + 12, cy + 11, GuiTheme.rgb(preset.accent));
            }
            var font = TaggerFonts.body();
            int textX = width >= 175 ? px + 78 : px;
            int textY = width >= 175 ? py + 3 : py + 49;
            g.text(font, font.plainSubstrByWidth(preset.label, getRight() - textX - 7), textX, textY, GuiTheme.rgb(preset.text), false);
            if (width >= 175) {
                TaggerWidgets.rounded(g, textX, py + 21, 12, 12, 6, GuiTheme.rgb(preset.accent));
                g.text(font, selected ? "Selected" : preset == ThemePreset.GLASS ? "Transparent" : preset == ThemePreset.LATTE ? "Light" : "Dark", textX + 17, py + 23, GuiTheme.rgb(preset.muted), false);
            }
        }
    }
    private static final class Swatch extends TaggerWidgets.Action {
        private final String colour;
        Swatch(String colour, int width, Runnable select) {
            super("Accent #" + colour, width, 22, false, pressed -> select.run()); this.colour = colour;
            setTooltip(TaggerFonts.tooltip(Component.literal("Accent #" + colour)));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            TaggerWidgets.surface(g, getX(), getY(), width, height, 6, GuiTheme.rgb(Integer.parseInt(colour, 16)), isFocused() ? GuiTheme.text : GuiTheme.border);
        }
    }
    private static final class PreferenceSlider extends AbstractSliderButton {
        private final String label, suffix;
        private final int min, max;
        private final java.util.function.IntConsumer changed;
        PreferenceSlider(String label, int width, int initial, int min, int max, String suffix, java.util.function.IntConsumer changed) {
            super(0, 0, width, 34, Component.empty(), (initial - min) / (double) (max - min));
            this.label = label; this.suffix = suffix; this.min = min; this.max = max; this.changed = changed; updateMessage();
        }
        private int setting() { return (int) Math.round(min + value * (max - min)); }
        @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            if (active && (event.isLeft() || event.isRight())) {
                value = (Math.clamp(setting() + (event.isRight() ? 1 : -1), min, max) - min) / (double)(max - min);
                applyValue(); updateMessage(); return true;
            }
            return super.keyPressed(event);
        }
        @Override protected void updateMessage() { setMessage(Component.literal(label + ": " + setting() + suffix)); }
        @Override protected void applyValue() { value = (setting() - min) / (double)(max - min); changed.accept(setting()); }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            TaggerWidgets.surface(g, getX(), getY(), width, height, 6, GuiTheme.surface, isFocused() ? GuiTheme.accent : GuiTheme.border);
            g.text(TaggerFonts.body(), getMessage(), getX() + 8, getY() + 5, active ? GuiTheme.text : GuiTheme.muted, false);
            int track = width - 20, position = (int) (track * value);
            if (max - min <= 12) for (int tick = min; tick <= max; tick++) {
                int tx = getX() + 10 + (int)Math.round(track * (tick - min) / (double)(max - min));
                g.fill(tx, getY() + 21, tx + 1, getY() + 29, GuiTheme.muted);
            }
            TaggerWidgets.rounded(g, getX() + 10, getY() + 24, track, 3, 1, GuiTheme.border);
            TaggerWidgets.rounded(g, getX() + 10, getY() + 24, position, 3, 1, GuiTheme.accent);
            TaggerWidgets.rounded(g, getX() + 7 + position, getY() + 22, 7, 7, 3, GuiTheme.accent);
        }
    }
    private static final class TierPreview extends AbstractWidget {
        TierPreview(int width) { super(0, 0, width, 39, Component.literal("Tier palette preview: Sword HT1, HT2, HT3, HT4, HT5")); }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            TaggerWidgets.surface(g, getX(), getY(), width, height, 6, GuiTheme.surface, GuiTheme.border);
            var config = config();
            for (int number = 1; number <= 5; number++) {
                int left = getX() + (number - 1) * width / 5 + width / 10;
                if (isHovered() && mouseX >= getX() + (number-1)*width/5 && mouseX < getX() + number*width/5)
                    TierHover.show(Component.literal("Sword: HT" + number + " · Points: "
                        + (CritsTierTaggerClient.instance().selectedProvider() == crits.provider.TierlistProvider.AFRITIERS
                        ? new crits.ranking.Tier("sword", "HT"+number, false).points() : "not supplied")), mouseX, mouseY);
                g.centeredText(TaggerFonts.body(), ModeIcons.component("sword"), left, getY() + 5, GuiTheme.text);
                TaggerWidgets.tierText(g, TaggerFonts.body(), "HT" + number, left - 10, getY() + 23, GuiTheme.rgb(config.tierColour("HT" + number)));
            }
        }
    }
}
