package crits.gui;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Shared swatch and hex picker with an in-game tag preview. */
final class ColourPickerScreen extends TaggerScreen {
    private final AppearanceScreen owner;
    private final Consumer<String> changed;
    private int colour, x, y, w, h;
    private String error = "";
    ColourPickerScreen(AppearanceScreen owner, String label, int colour, Consumer<String> changed) {
        super(net.minecraft.client.Minecraft.getInstance(), TaggerFonts.body(), Component.literal(label));
        this.owner = owner; this.colour = colour; this.changed = changed;
    }
    private String hex() { return String.format(Locale.ROOT, "%06X", colour); }
    private void select(int value) { colour = value; error = ""; changed.accept(hex()); }
    @Override protected void init() {
        w = Math.min(340, width - 20); h = Math.min(220, height - 12); x = (width-w)/2; y = (height-h)/2;
        String[] palette = {"FFD700","C0C0C0","CD7F32","B58AE8","FFFFFF","151A22","E9A0AF","F0B9DD","D6AEED","8AABFF","88C0D0","8DDEC9","ACD39B","E6CF8D","E4B379","E16B64"};
        for (int i=0;i<palette.length;i++) {
            int rgb = Integer.parseInt(palette[i],16);
            var swatch = new TaggerWidgets.Action("Colour #"+palette[i], (w-32)/8, 20, false, b -> {select(rgb); rebuildWidgets();}) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    TaggerWidgets.rounded(g,getX(),getY(),getWidth(),getHeight(),3,GuiTheme.rgb(rgb));
                    if(isFocused())g.outline(getX(),getY(),getWidth(),getHeight(),GuiTheme.text);
                }
            };
            swatch.setPosition(x+12+i%8*((w-32)/8+1),y+42+i/8*24); addRenderableWidget(swatch);
        }
        var field=new TaggerWidgets.Input(w-32,20,Component.literal("Hex colour"));field.setMaxLength(7);field.setValue("#"+hex());
        field.setResponder(input->{try {String clean=crits.config.AppearanceConfig.hex(input,false);select(Integer.parseInt(clean,16));field.setInvalid(false);}catch(IllegalArgumentException invalid){field.setInvalid(true);error="Enter six hex digits";}});
        field.setPosition(x+16,y+110);addRenderableWidget(field);
        var reset=TaggerWidgets.action("Default",70,false,b->{changed.accept("");onClose();});reset.setPosition(x+12,y+h-28);addRenderableWidget(reset);
        var done=TaggerWidgets.action("Done",70,true,b->onClose());done.setPosition(x+w-82,y+h-28);addRenderableWidget(done);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        TaggerWidgets.panel(g,x,y,w,h);g.text(font,title,x+12,y+13,GuiTheme.text,false);
        g.text(font,"Hex colour",x+12,y+94,GuiTheme.muted,false);
        TaggerWidgets.surface(g,x+12,y+138,w-24,36,4,GuiTheme.surface,GuiTheme.border);
        g.text(font,"Live preview",x+20,y+143,GuiTheme.muted,false);
        var sample = crits.CritsTierTaggerClient.instance().config(); sample.customColour = hex();
        String level = title.getString().matches("[HL]T[1-5]") ? title.getString() : "HT1";
        var preview = Component.empty().append(crits.TagFormatter.tag(new crits.ranking.Tier("sword",level,false), sample))
            .append(minecraft.getUser().getName());
        g.centeredText(minecraft.font,preview,x+w/2,y+159,GuiTheme.text);
        if(!error.isEmpty())g.text(font,error,x+12,y+178,GuiTheme.text,false);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose(){minecraft.gui.setScreen(owner);}
    @Override public boolean isPauseScreen(){return false;}
}
