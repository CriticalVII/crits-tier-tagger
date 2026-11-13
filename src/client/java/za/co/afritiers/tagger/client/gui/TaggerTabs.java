package za.co.afritiers.tagger.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;

/** Shared navigation keeps all configuration tabs aligned at every GUI scale. */
final class TaggerTabs {
    private TaggerTabs() { }
    static void add(Consumer<Button> add, TaggerSettingsScreen owner, String active, TaggerFrame frame) {
        String[] labels = {"Settings", "Tierlists", "Profiles", "Waitlists"};
        for (int index = 0; index < labels.length; index++) {
            String label = labels[index];
            var tab = new TaggerWidgets.Tab(label, label.equals(active), pressed -> {
                if (label.equals(active)) return;
                Minecraft client = Minecraft.getInstance();
                client.gui.setScreen(switch (label) {
                    case "Profiles" -> new ProfilesScreen(owner);
                    case "Tierlists" -> new TierlistsScreen(owner);
                    case "Waitlists" -> new WaitlistsScreen(owner);
                    default -> owner;
                });
            });
            boolean rail = frame.rail() > 0;
            int tabWidth = rail ? frame.rail() - 16 : (frame.w() - 42) / 4;
            tab.setWidth(tabWidth); tab.setHeight(rail ? 26 : 18); tab.setRail(rail);
            tab.setPosition(rail ? frame.left() + 8 : frame.x() + 12 + index * (tabWidth + 6),
                    rail ? frame.top() + 90 + index * 32 : frame.top() + 35);
            add.accept(tab);
        }
    }
}
