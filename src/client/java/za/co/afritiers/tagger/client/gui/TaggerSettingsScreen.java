package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import za.co.afritiers.tagger.client.AfriTiersClient;
import za.co.afritiers.tagger.client.ModeIcons;
import za.co.afritiers.tagger.client.TagFormatter;
import za.co.afritiers.tagger.config.TaggerConfig;
import za.co.afritiers.tagger.ranking.Tier;
import za.co.afritiers.tagger.provider.TierlistProvider;

/** Translucent native settings with immediate persistence and a pinned reference preview. */
public final class TaggerSettingsScreen extends Screen {
    private final Screen parent;
    private TaggerConfig draft;
    private Component preview = Component.empty();
    private EditBox colourField;
    private String feedback = "", validationError = "";
    private int previewTier = 0;
    private int panelX, panelY, panelWidth, panelHeight;
    private boolean compact;
    private int navigationOffset;
    private String draftProvider;
    private TaggerFrame frame;

    public TaggerSettingsScreen(Screen parent) {
        super(net.minecraft.client.Minecraft.getInstance(), TaggerFonts.body(), Component.translatable("afritiers_tagger.title"));
        this.parent = parent;
        draft = AfriTiersClient.instance().config();
        draftProvider = draft.tierlist;
    }

    @Override protected void init() {
        AfriTiersClient.instance().loadSelectedTierlist();
        if (!draftProvider.equals(AfriTiersClient.instance().config().tierlist)) {
            draft = AfriTiersClient.instance().config(); draftProvider = draft.tierlist;
        }
        frame = TaggerFrame.fit(width, height);
        panelWidth = frame.w(); panelHeight = frame.h(); panelX = frame.x(); panelY = frame.y();
        compact = panelHeight < 300;
        navigationOffset = frame.offset();
        int contentWidth = panelWidth - 34;
        boolean columns = panelWidth >= 320;
        int columnWidth = columns ? (contentWidth - 12) / 2 : contentWidth;
        LinearLayout display = section("Visibility", columnWidth);
        display.addChild(new TaggerWidgets.Toggle("Enabled", "Show tier tags", columnWidth, compact, () -> draft.enabled, value -> { draft.enabled = value; persist(); }));
        display.addChild(new TaggerWidgets.Toggle("Nameplates", "Above players", columnWidth, compact, () -> draft.nameplates, value -> { draft.nameplates = value; persist(); }));
        display.addChild(new TaggerWidgets.Toggle("Tab list", "In the player list", columnWidth, compact, () -> draft.tabList, value -> { draft.tabList = value; persist(); }));
        LinearLayout appearance = section("Rankings & colour", columnWidth);
        appearance.addChild(new TaggerWidgets.Mode(columnWidth, compact, this::modeText,
                pressed -> minecraft.gui.setScreen(new ModePickerScreen(this, draft.mode))));
        appearance.addChild(new TaggerWidgets.Toggle("Retired tiers", "Include past tiers", columnWidth, compact, () -> draft.includeRetired, value -> { draft.includeRetired = value; persist(); }));
        LinearLayout colour = compact ? LinearLayout.horizontal().spacing(4) : LinearLayout.vertical().spacing(3);
        colour.addChild(new TaggerWidgets.Label(compact ? 43 : columnWidth, compact ? 20 : 12,
                Component.literal(compact ? "Colour" : "Colour override").withColor(TaggerWidgets.MUTED), font));
        EditBox field = new TaggerWidgets.Input(compact ? columnWidth - 47 : columnWidth, 20, Component.literal("Colour override; blank uses tier colours"));
        colourField = field;
        field.setMaxLength(7);
        field.setBordered(false);
        field.setValue(draft.customColour);
        field.setHint(Component.literal(compact ? "Tier colours" : "Default tier colours").withColor(TaggerWidgets.MUTED));
        field.setTextColor(0xFFE8ECE9);
        field.setResponder(value -> { draft.customColour = value; updatePreview(); if (validationError.isEmpty()) persist(); });
        field.setTooltip(TaggerFonts.tooltip(Component.literal("Optional six-digit hex colour, for example FFD700. Leave blank for gold, silver, bronze and purple. Icons keep their own colours.")));
        colour.addChild(field);
        appearance.addChild(colour);
        LinearLayout controls = columns ? LinearLayout.horizontal().spacing(12) : LinearLayout.vertical().spacing(12);
        controls.addChild(display);
        controls.addChild(appearance);
        var settings = LinearLayout.vertical().spacing(10);
        settings.addChild(tierlistChoices(contentWidth));
        settings.addChild(controls);
        ScrollableLayout scroll = new ScrollableLayout(minecraft, settings, Math.max(32, panelHeight - (compact ? 128 : 174) - navigationOffset), ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.setMinWidth(contentWidth);
        scroll.arrangeElements();
        scroll.setX(panelX + 12);
        scroll.setY(panelY + (compact ? 78 : 104) + navigationOffset);
        scroll.visitWidgets(this::addRenderableWidget);

        Button previous = new TaggerWidgets.Action("<", 16, 16, false, pressed -> cyclePreview(-1));
        previous.setTooltip(TaggerFonts.tooltip(Component.literal("Previous preview tier")));
        previous.setPosition(panelX + panelWidth - 50, panelY + (compact ? 38 : 48) + navigationOffset);
        addRenderableWidget(previous);
        Button next = new TaggerWidgets.Action(">", 16, 16, false, pressed -> cyclePreview(1));
        next.setTooltip(TaggerFonts.tooltip(Component.literal("Next preview tier")));
        next.setPosition(panelX + panelWidth - 30, panelY + (compact ? 38 : 48) + navigationOffset);
        addRenderableWidget(next);
        Button refresh = TaggerWidgets.action("Refresh", 52, false, pressed -> feedback = AfriTiersClient.instance().repository().refresh());
        refresh.setTooltip(TaggerFonts.tooltip(Component.literal("Refresh rankings using your saved settings.")));
        refresh.setPosition(panelX + panelWidth - 122, panelY + panelHeight - (compact ? 49 : 69));
        addRenderableWidget(refresh);
        Button details = TaggerWidgets.action("Details", 52, false, pressed -> minecraft.gui.setScreen(new ConnectionDetailsScreen(this)));
        details.setTooltip(TaggerFonts.tooltip(Component.literal("Connection, freshness and username matching details")));
        details.setPosition(panelX + panelWidth - 64, panelY + panelHeight - (compact ? 49 : 69));
        addRenderableWidget(details);
        Button reset = TaggerWidgets.action("Reset", 52, false, pressed -> {
            draft = new TaggerConfig();
            updatePreview();
            persist();
            rebuildWidgets();
        });
        reset.setTooltip(TaggerFonts.tooltip(Component.literal("Restore and save default settings")));
        reset.setPosition(panelX + 12, panelY + panelHeight - (compact ? 27 : 30));
        addRenderableWidget(reset);
        Button done = TaggerWidgets.action("Done", 80, true, pressed -> onClose());
        done.setPosition(panelX + panelWidth - 92, panelY + panelHeight - (compact ? 27 : 30));
        addRenderableWidget(done);
        addTabs();
        updatePreview();
    }

    private void addTabs() {
        TaggerTabs.add(this::addRenderableWidget, this, "Settings", frame);
    }

    private LinearLayout tierlistChoices(int width) {
        var choices = LinearLayout.vertical().spacing(4);
        choices.addChild(new TaggerWidgets.Label(width, 14, Component.literal("In-game tierlist").withColor(TaggerWidgets.TEXT), font));
        int columns = width >= 320 ? 4 : 2;
        int cellWidth = (width - (columns - 1) * 6) / columns;
        LinearLayout row = null;
        int index = 0;
        for (var provider : TierlistProvider.values()) {
            if (index++ % columns == 0) { row = LinearLayout.horizontal().spacing(6); choices.addChild(row); }
            var button = TierlistLogo.button(provider, provider.label(), cellWidth, compact ? 22 : 28,
                    provider == AfriTiersClient.instance().selectedProvider(), pressed -> {
                        try {
                            AfriTiersClient.instance().selectTierlist(provider);
                            feedback = "Saved · now showing " + provider.label();
                            rebuildWidgets();
                        } catch (java.io.IOException exception) { feedback = "Could not save. Check config permissions."; }
                    });
            button.setTooltip(TaggerFonts.tooltip(Component.literal(provider.description() + "\nUsed for Tab and nametags. Profiles has its own selector.")));
            row.addChild(button);
        }
        return choices;
    }

    private LinearLayout section(String heading, int sectionWidth) {
        LinearLayout layout = LinearLayout.vertical().spacing(3);
        layout.addChild(new TaggerWidgets.Label(sectionWidth, 18, Component.literal(heading).withColor(TaggerWidgets.TEXT), font));
        return layout;
    }

    void selectMode(String mode) {
        draft.mode = mode;
        persist();
        updatePreview();
    }

    private Component modeText() {
        return Component.empty().append(ModeIcons.component(draft.mode))
                .append(" " + AfriTiersClient.instance().selectedProvider().modeLabel(draft.mode));
    }

    private void cyclePreview(int direction) {
        previewTier = Math.floorMod(previewTier + direction, Tier.ORDER.size());
        updatePreview();
    }

    private void updatePreview() {
        String previousError = validationError;
        TaggerConfig sample = draft.copy();
        validationError = "";
        try { sample.validate(); }
        catch (IllegalArgumentException exception) { validationError = exception.getMessage(); sample.customColour = ""; }
        preview = Component.empty().append(TagFormatter.tag(new Tier("sword",
                Tier.ORDER.get(previewTier), false), sample)).append(minecraft.getUser().getName());
        if (colourField != null) {
            colourField.setTextColor(validationError.isEmpty() ? TaggerWidgets.TEXT : 0xFFE0BE83);
            if (!previousError.equals(validationError)) colourField.setTooltip(TaggerFonts.tooltip(Component.literal(validationError.isEmpty()
                    ? "Leave blank for tier colours, or enter a six-digit hex colour." : validationError)));
        }
    }

    private void persist() {
        TaggerConfig edited = AfriTiersClient.instance().config();
        edited.enabled = draft.enabled;
        edited.nameplates = draft.nameplates;
        edited.tabList = draft.tabList;
        edited.includeRetired = draft.includeRetired;
        edited.mode = draft.mode;
        edited.tierlistModes.put(edited.tierlist, draft.mode);
        edited.customColour = draft.customColour;
        // An unfinished colour entry must not prevent independent toggles from saving.
        if (!validationError.isEmpty()) edited.customColour = AfriTiersClient.instance().config().customColour;
        try { AfriTiersClient.instance().saveConfig(edited); feedback = "Saved automatically"; }
        catch (java.io.IOException | IllegalArgumentException exception) {
            feedback = exception instanceof IllegalArgumentException ? exception.getMessage() : "Could not save. Check config folder permissions.";
        }
    }

    @Override public void tick() { updatePreview(); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        frame.draw(g, "Settings");
        int x = panelX + 12, y = panelY + (compact ? 34 : 42) + navigationOffset, w = panelWidth - 24;
        int previewHeight = compact ? 36 : 50;
        TaggerWidgets.surface(g, x, y, w, previewHeight, 3, 0xFF111111, TaggerWidgets.LINE);
        g.text(font, "Live preview", x + 10, y + (compact ? 6 : 8), TaggerWidgets.MUTED, false);
        g.centeredText(minecraft.font, preview, x + w / 2, y + (compact ? 22 : 29), TaggerWidgets.TEXT);
        var state = AfriTiersClient.instance().repository().state();
        int statusColor = switch (state.phase()) {
            case READY -> TaggerWidgets.GREEN;
            case STALE, LOADING -> 0xFFD1BB82;
            case ERROR -> 0xFFE1A094;
            default -> TaggerWidgets.MUTED;
        };
        String summary = switch (state.phase()) {
            case READY -> "Up to date | " + state.rankings().players().size() + (state.rankings().players().size() == 1 ? " player" : " players");
            case STALE -> "Source out of date";
            case LOADING -> "Updating rankings";
            case ERROR -> "Connection issue";
            case DISABLED -> "Tagger disabled";
            case DISCONNECTED -> "Not loaded";
        };
        int statusY = panelY + panelHeight - (compact ? 44 : 62);
        if (compact && !validationError.isEmpty()) summary = validationError;
        else if (compact && !feedback.isEmpty()) summary = feedback;
        g.fill(x, statusY + 2, x + 3, statusY + 5, statusColor);
        g.text(font, font.plainSubstrByWidth(summary, panelWidth - 152), x + 9, statusY, TaggerWidgets.MUTED, false);
        String note = !validationError.isEmpty() ? validationError : !feedback.isEmpty() ? feedback : "Settings save automatically";
        if (!compact) g.text(font, font.plainSubstrByWidth(note, panelWidth - 24), x, panelY + panelHeight - 43,
                validationError.isEmpty() ? TaggerWidgets.MUTED : 0xFFE0BE83, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
