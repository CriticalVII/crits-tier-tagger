package za.co.afritiers.tagger.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import za.co.afritiers.tagger.config.TaggerConfig;
import za.co.afritiers.tagger.ranking.Tier;

public final class TagFormatter {
    private TagFormatter() { }

    public static Component decorate(String username, Component original, boolean tabList) {
        AfriTiersClient mod = AfriTiersClient.instance();
        if (mod == null || original == null) return original;
        TaggerConfig config = mod.activeConfig();
        if (!config.enabled || (tabList ? !config.tabList : !config.nameplates)) return original;
        // A renderer or another compatibility hook may reuse an already decorated component.
        boolean decorated = original.visit((style, text) -> ModeIcons.iconForFont(style.getFont()) != null
                ? java.util.Optional.of(true) : java.util.Optional.empty(),
                net.minecraft.network.chat.Style.EMPTY).orElse(false);
        if (decorated) return original;
        var rankings = mod.repository().state().rankings();
        String apiName = mod.rankingUsername(username);
        var selected = rankings.select(apiName, config.mode, config.includeRetired);
        return selected
                .<Component>map(tier -> Component.empty().append(tag(tier, config, tabList)).append(original))
                .orElse(original);
    }

    public static Component tag(Tier tier, TaggerConfig config) {
        return tag(tier, config, false);
    }

    public static Component tag(Tier tier, TaggerConfig config, boolean tabList) {
        return Component.empty()
                .append(ModeIcons.component(tier.mode(), tabList))
                .append(" ")
                .append(Component.literal(tier.displayLevel())
                        .withStyle(style -> style.withFont(FontDescription.DEFAULT).withColor(config.colour(tier.level()))))
                .append(" ")
                .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(tier.label()))));
    }
}
