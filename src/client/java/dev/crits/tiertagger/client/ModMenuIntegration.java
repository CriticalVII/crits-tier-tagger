package dev.crits.tiertagger.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.crits.tiertagger.client.gui.TaggerSettingsScreen;

public final class ModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TaggerSettingsScreen::new;
    }
}
