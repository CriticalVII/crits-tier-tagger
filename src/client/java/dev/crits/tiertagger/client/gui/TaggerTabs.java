package dev.crits.tiertagger.client.gui;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;

/** Shared navigation keeps all configuration tabs aligned at every GUI scale. */
final class TaggerTabs {
    private TaggerTabs() { }
    static void add(Consumer<Button> add, TaggerSettingsScreen owner, String active, TaggerFrame frame) {
        String[] labels = {"Settings", "Profiles", "Waitlists"};
        for (int index = 0; index < labels.length; index++) {
            String label = labels[index];
            var tab = new TaggerWidgets.Tab(label, label.equals(active), pressed -> {
                if (label.equals(active)) return;
                Minecraft client = Minecraft.getInstance();
                client.gui.setScreen(switch (label) {
                    case "Profiles" -> new ProfilesScreen(owner);
                    case "Waitlists" -> new WaitlistsScreen(owner);
                    default -> owner;
                });
            });
            tab.setWidth(frame.rail() - 16); tab.setHeight(22);
            tab.setPosition(frame.left() + 8, frame.top() + 46 + index * 26);
            add.accept(tab);
        }
    }
}
