package crits;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import crits.gui.TaggerSettingsScreen;

public final class ModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TaggerSettingsScreen::new;
    }
}
