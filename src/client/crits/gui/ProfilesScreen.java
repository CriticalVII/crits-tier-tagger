package crits.gui;

import crits.CritsTierTaggerClient;
import crits.render.ModeIcons;
import crits.provider.TierlistProvider;
import crits.ranking.ModeIcon;
import crits.ranking.PlayerStats;
import crits.ranking.Rankings;
import crits.ranking.Tier;
import crits.repository.RankingRepository;
import java.util.Comparator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A centred searchable profile, overall position and compact mode badges. */
public final class ProfilesScreen extends TaggerScreen {
    private final TaggerSettingsScreen owner;
    private String input = "", requested = "", apiName = "", feedback = "";
    private Rankings snapshot = Rankings.EMPTY;
    private RankingRepository.Phase phase;
    private EditBox searchField;
    private Button searchButton;
    private int x, y, w, h;
    private int contentOffset;
    private TierlistProvider provider;
    private TaggerFrame frame;

    public ProfilesScreen(TaggerSettingsScreen owner) {
        super(net.minecraft.client.Minecraft.getInstance(), TaggerFonts.body(), Component.literal("Crit's profiles"));
        this.owner = owner;
        provider = CritsTierTaggerClient.instance().defaultProfileProvider();
        requested = net.minecraft.client.Minecraft.getInstance().getUser().getName();
    }

    public ProfilesScreen(TaggerSettingsScreen owner, String username) {
        this(owner);
        if (username == null || !username.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Invalid player username");
        input = username;
        requested = username;
    }

    public ProfilesScreen(TaggerSettingsScreen owner, String username, TierlistProvider provider) {
        this(owner, username); this.provider = provider;
    }

    void selectProvider(TierlistProvider selected) {
        try {
            var config = CritsTierTaggerClient.instance().config(); config.profileTierlist = selected.id();
            CritsTierTaggerClient.instance().saveGuiConfig(config);
            provider = selected; feedback = "";
        } catch (java.io.IOException exception) { feedback = "Could not save profile tierlist. Check config permissions."; }
    }

    @Override protected void init() {
        var mod = CritsTierTaggerClient.instance();
        mod.beginPlayerSearch(provider);
        snapshot = mod.playerSearchRepository().state().rankings();
        phase = mod.playerSearchRepository().state().phase();
        apiName = requested.isEmpty() ? "" : mod.rankingUsername(requested, snapshot, provider);
        frame = TaggerFrame.fit(width, height);
        w = frame.w(); h = frame.h(); x = frame.x(); y = frame.y();
        contentOffset = (w < 440 ? 24 : 0);
        TaggerTabs.add(this::addRenderableWidget, owner, "Profiles", frame);
        var source = TierlistLogo.button(provider, provider.label() + " >", w < 440 ? w - 24 : 112, 20, false,
                pressed -> minecraft.gui.setScreen(new ProfileProviderScreen(this, provider)));
        source.setTooltip(TaggerFonts.tooltip(Component.literal("Profile tierlist. This does not change in-game tags.")));
        source.setPosition(x + 12, y + 42); addRenderableWidget(source);
        searchButton = TaggerWidgets.action("Search", 60, true, pressed -> search());
        searchButton.setPosition(x + w - 148, y + 42 + contentOffset);
        searchField = new TaggerWidgets.Input(w < 440 ? w - 166 : w - 288, 20, Component.literal("Player username"));
        searchField.setPosition(x + (w < 440 ? 12 : 132), y + 42 + contentOffset);
        searchField.setMaxLength(16);
        searchField.setCentered(true);
        searchField.setBordered(false);
        searchField.setTextColor(GuiTheme.text);
        searchField.setHint(Component.literal("Player username"));
        searchField.setValue(input);
        searchField.setResponder(value -> { input = value; searchButton.active = validInput(); });
        searchField.setTooltip(TaggerFonts.tooltip(Component.literal(provider == TierlistProvider.AFRITIERS
                ? "Search a username or saved alias. Verified paid-account name changes are resolved automatically."
                : "Resolve a username to its verified Minecraft UUID, then search " + provider.label() + ".")));
        addRenderableWidget(searchField); addRenderableWidget(searchButton);
        Button self = TaggerWidgets.action("My profile", 70, false, pressed -> {
            input = "";
            requested = minecraft.getUser().getName();
            feedback = "";
            mod.requestPlayerSearch(requested);
            rebuildWidgets();
        });
        self.setPosition(x + w - 82, y + 42 + contentOffset); addRenderableWidget(self);
        searchButton.active = validInput();
        if (requested.isEmpty()) setInitialFocus(searchField);

        addProfile(snapshot.profile(apiName).orElse(null));
        Button refresh = TaggerWidgets.action("Refresh", 64, false, pressed -> feedback = mod.playerSearchRepository().refresh());
        refresh.setTooltip(TaggerFonts.tooltip(Component.literal("Refresh player stats. API cooldowns still apply.")));
        refresh.setPosition(x + 12, y + h - 30); addRenderableWidget(refresh);
        Button back = TaggerWidgets.action("Back", 80, false, pressed -> onClose());
        back.setPosition(x + w - 92, y + h - 30); addRenderableWidget(back);
    }

    private boolean validInput() { return input.trim().matches("[A-Za-z0-9_]{1,16}"); }

    private void search() {
        if (!validInput()) return;
        requested = input.trim();
        feedback = "";
        CritsTierTaggerClient.instance().requestPlayerSearch(requested);
        rebuildWidgets();
    }

    private void addProfile(PlayerStats profile) {
        int contentWidth = Math.min(360, w - 24);
        LinearLayout rows = LinearLayout.vertical().spacing(4);
        rows.addChild(new IdentityHeader(contentWidth, profile));
        if (profile == null) {
            rows.addChild(new ProfileLine(contentWidth, Component.literal(lookupMessage()).withColor(GuiTheme.muted)));
        } else {
            rows.addChild(new TaggerWidgets.Label(contentWidth, 12, Component.literal("POSITION").withColor(GuiTheme.muted), font));
            rows.addChild(new PositionBar(contentWidth, profile));
            rows.addChild(new TaggerWidgets.Label(contentWidth, 12, Component.literal("TIERS").withColor(GuiTheme.muted), font));
            var tiers = snapshot.players().getOrDefault(apiName, java.util.List.of()).stream()
                    .sorted(Comparator.comparing(Tier::retired).thenComparingInt(Tier::strength).thenComparing(Tier::mode)).toList();
            if (!tiers.isEmpty()) rows.addChild(new TierStrip(contentWidth, tiers));
            if (tiers.isEmpty()) rows.addChild(new ProfileLine(contentWidth, Component.literal("No recorded tiers").withColor(GuiTheme.muted)));
        }
        var scroll = new ScrollableLayout(minecraft, rows, Math.max(32, h - 128 - contentOffset), ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.setMinWidth(contentWidth);
        scroll.arrangeElements(); scroll.setX(x + (w - contentWidth) / 2); scroll.setY(y + 78 + contentOffset);
        scroll.visitWidgets(this::addRenderableWidget);
    }

    private String lookupMessage() {
        var mod = CritsTierTaggerClient.instance();
        return switch (mod.playerLookupStatus(requested, provider, mod.playerSearchRepository())) {
            case LOADING -> "Loading player stats...";
            case ERROR -> "Stats unavailable — retrying automatically";
            default -> "No ranked player found";
        };
    }

    private java.util.function.Supplier<net.minecraft.world.entity.player.PlayerSkin> skinSource() {
        var online = minecraft.getConnection() == null ? null : minecraft.getConnection().getListedOnlinePlayers().stream()
                .filter(info -> info.getProfile().name().equalsIgnoreCase(requested)).findFirst().orElse(null);
        return online != null ? online::getSkin : requested.equalsIgnoreCase(minecraft.getUser().getName())
                ? minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), true) : DefaultPlayerSkin::getDefaultSkin;
    }

    @Override public void tick() {
        var mod = CritsTierTaggerClient.instance();
        var state = mod.playerSearchRepository().state();
        if (!requested.isEmpty()) mod.requestPlayerSearch(requested);
        String resolved = requested.isEmpty() ? "" : mod.rankingUsername(requested, state.rankings(), provider);
        boolean changed = !snapshot.profile(apiName).equals(state.rankings().profile(resolved))
                || !java.util.Objects.equals(snapshot.players().get(apiName), state.rankings().players().get(resolved));
        if (changed || phase != state.phase() || !apiName.equals(resolved)) rebuildWidgets();
        else snapshot = state.rankings();
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) && searchField.isFocused()) {
            search(); return true;
        }
        return super.keyPressed(event);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        frame.draw(g, "Profiles");
        var state = CritsTierTaggerClient.instance().playerSearchRepository().state();
        var profile = snapshot.profile(apiName);
        String note = !feedback.isEmpty() ? feedback : state.phase() == RankingRepository.Phase.ERROR ? state.message()
                : snapshot.stale() ? "Source out of date · placements may change"
                : profile.isPresent() ? provider == TierlistProvider.AFRITIERS ? "Points count active tiers; retired tiers earn 0."
                        : provider.label() + " supplies the displayed points and overall rank."
                : "Searching " + provider.label() + (provider == TierlistProvider.AFRITIERS ? " by username." : " by verified account UUID.");
        g.text(font, font.plainSubstrByWidth(note, w - 24), x + 12, y + h - 45, GuiTheme.muted, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override public void removed() { CritsTierTaggerClient.instance().endPlayerSearch(); }
    @Override public void onClose() { minecraft.gui.setScreen(owner); }
    @Override public boolean isPauseScreen() { return false; }

    private final class ProfileLine extends AbstractWidget {
        ProfileLine(int width, Component text) {
            super(0, 0, width, 18, text);
            setTooltip(TaggerFonts.tooltip(text));
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            if (isHovered()) TaggerWidgets.rounded(g, getX(), getY(), width, height, 2, GuiTheme.hover);
            g.enableScissor(getX(), getY() - 2, getRight(), getBottom());
            g.text(font, getMessage(), getX() + 4, getY() + 4, GuiTheme.text, false);
            g.disableScissor();
        }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class IdentityHeader extends AbstractWidget {
        private final PlayerStats profile;
        private final java.util.function.Supplier<net.minecraft.world.entity.player.PlayerSkin> skin = skinSource();
        IdentityHeader(int width, PlayerStats profile) {
            super(0, 0, width, 102, Component.literal((requested.equalsIgnoreCase(minecraft.getUser().getName()) ? "YOUR PROFILE · " : "")
                    + requested + (profile != null && !requested.equalsIgnoreCase(profile.name()) ? " · API name: " + profile.name() : "")
                    + (profile == null ? "" : " · Region: " + profile.region()
                    + (profile.combatTitle().isBlank() ? "" : " · " + profile.combatTitle()))));
            this.profile = profile;
            setTooltip(TaggerFonts.tooltip(Component.literal("Player: " + requested + (profile != null && !requested.equalsIgnoreCase(profile.name())
                    ? "\nAPI display name: " + profile.name() : "") + "\nUses your skin or an available online skin; otherwise a default skin.")));
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            int centre = getX() + width / 2;
            TaggerWidgets.surface(g, centre - 22, getY(), 44, 44, 22, GuiTheme.surface, GuiTheme.accent);
            PlayerFaceExtractor.extractRenderState(g, skin.get(), centre - 14, getY() + 8, 28);
            String displayName = profile != null && requested.equalsIgnoreCase(profile.name()) ? profile.name() : requested;
            g.text(font, TaggerFrame.heading(displayName), centre - font.width(TaggerFrame.heading(displayName)) / 2,
                    getY() + 49, GuiTheme.text, false);
            String title = profile != null && !profile.combatTitle().isBlank() ? profile.combatTitle() : provider.label() + " profile";
            int pillWidth = Math.min(width - 12, font.width(title) + 20);
            TaggerWidgets.rounded(g, centre - pillWidth / 2, getY() + 65, pillWidth, 17, 8, GuiTheme.selection);
            g.text(font, font.plainSubstrByWidth(title, pillWidth - 12), centre - font.width(font.plainSubstrByWidth(title, pillWidth - 12)) / 2,
                    getY() + 69, GuiTheme.accent, false);
            String region = profile == null ? "" : profile.region();
            g.text(font, region, centre - font.width(region) / 2, getY() + 91, GuiTheme.muted, false);
        }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
        }
    }

    private final class PositionBar extends AbstractWidget {
        private final PlayerStats profile;
        PositionBar(int width, PlayerStats profile) {
            super(0, 0, width, 26, Component.literal("Overall rank: " + (profile.position() == 0 ? "Not supplied" : "#" + profile.position())
                    + " · Points: " + (profile.points() < 0 ? "Not supplied" : profile.points())
                    + (profile.combatTitle().isBlank() ? "" : " (" + profile.combatTitle() + ")")));
            this.profile = profile; setTooltip(TaggerFonts.tooltip(getMessage()));
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            TaggerWidgets.surface(g, getX(), getY(), width, height, 4, GuiTheme.surface, isFocused() ? GuiTheme.accent : GuiTheme.border);
            String rank = profile.position() == 0 ? "—" : "#" + profile.position();
            int rankWidth = Math.min(width / 2, Math.max(43, font.width(rank) + 16));
            TaggerWidgets.rounded(g, getX() + 1, getY() + 1, rankWidth, height - 2, 3, GuiTheme.selection);
            g.text(font, rank, getX() + (rankWidth - font.width(rank)) / 2, getY() + 9, GuiTheme.accent, false);
            String text = "Overall  ·  " + (profile.points() < 0 ? "Points unavailable" : profile.points() + " points");
            g.text(font, font.plainSubstrByWidth(text, width - rankWidth - 18), getX() + rankWidth + 10, getY() + 9, GuiTheme.text, false);
        }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
        }
    }

    private final class TierStrip extends AbstractWidget {
        private final java.util.List<Tier> tiers;
        private final int columns;
        TierStrip(int width, java.util.List<Tier> tiers) {
            super(0, 0, width, 48 * ((tiers.size() + columnsFor(width) - 1) / columnsFor(width)) + 2,
                    Component.literal(tiers.stream().map(tier -> provider.modeLabel(tier.mode()) + ": " + tier.displayLevel())
                    .collect(java.util.stream.Collectors.joining(" · "))));
            this.tiers = java.util.List.copyOf(tiers);
            this.columns = columnsFor(width);
        }
        private static int columnsFor(int width) { return Math.max(1, Math.min(8, (width - 12) / 36)); }
        private int badgeX(int index) {
            int count = Math.min(columns, tiers.size() - (index / columns) * columns);
            float cell = (width - 12F) / columns;
            return getX() + Math.round((width - count * cell) / 2 + (index % columns) * cell + (cell - 34) / 2);
        }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            TaggerWidgets.surface(g, getX(), getY(), width, height, 4, GuiTheme.surface, isFocused() ? GuiTheme.accent : GuiTheme.border);
            int hovered = -1;
            if (isHovered()) for (int index = 0; index < tiers.size(); index++) {
                int left = badgeX(index), top = getY() + (index / columns) * 48;
                if (mouseX >= left && mouseX < left + 34 && mouseY >= top && mouseY < top + 48) { hovered = index; break; }
            }
            if (hovered >= 0) TierHover.show(
                    Component.literal(provider.modeLabel(tiers.get(hovered).mode()) + ": " + tiers.get(hovered).displayLevel() + " · Points: " + (provider == TierlistProvider.AFRITIERS ? tiers.get(hovered).points() : "not supplied")), mouseX, mouseY);
            var palette = CritsTierTaggerClient.instance().config();
            for (int index = 0; index < tiers.size(); index++) {
                Tier tier = tiers.get(index); int left = badgeX(index);
                int top = getY() + (index / columns) * 48;
                int colour = 0xFF000000 | palette.tierColour(tier.displayLevel());
                TaggerWidgets.surface(g, left + 4, top + 5, 26, 26, 13, GuiTheme.background, hovered == index ? colour : GuiTheme.border);
                GuiIcons.draw(g, ModeIcons.sprite(ModeIcon.forMode(tier.mode())).spriteId(), left + 9, top + 10, 16);
                String level = tier.displayLevel();
                TaggerWidgets.tierText(g, font, level, left + (34 - font.width(level)) / 2, top + 35, colour);
            }
        }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
        }
    }
}
