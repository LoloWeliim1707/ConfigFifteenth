package github.loloweliim.configfifteenth.gui.widget;

import com.google.common.collect.ImmutableList;
import github.loloweliim.configfifteenth.gui.ConfigGuiScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class CollapsibleEntry extends ConfigGuiScreen.ConfigEntry {
    private final String title;
    private final List<ConfigGuiScreen.ConfigEntry> subEntries = new ArrayList<>();
    private final ButtonWidget toggleButton;
    private final ConfigGuiScreen screen;

    private final List<Element> childrenList;
    private final List<Selectable> selectableList;

    public CollapsibleEntry (ConfigGuiScreen screen, String title) {
        super(ButtonWidget.builder(Text.empty(), b -> {}).build());

        this.screen = screen;
        this.title = title;
        this.toggleButton = ButtonWidget.builder(Text.literal("[+] " + title), button -> {
            boolean nowExpanded = this.screen.toggleGroupExpansion(this.title);
            button.setMessage(Text.literal((nowExpanded ? "[-] " : "[+] ") + this.title));
        this.screen.rebuildSettingsList();
        }).dimensions(0, 0, 200, 20).build();

        this.childrenList = ImmutableList.of(this.toggleButton);
        this.selectableList = ImmutableList.of(this.toggleButton);
    }

    public void setExpandedState (boolean expanded) { this.toggleButton.setMessage(Text.literal((expanded ? "[-] " : "[+] ") + this.title)); }
    public void addSubEntry (ConfigGuiScreen.ConfigEntry entry) { this.subEntries.add(entry); }
    public List<ConfigGuiScreen.ConfigEntry> getSubEntries () { return this.subEntries; }

    @Override
    public void render (DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        this.toggleButton.setX(x + (entryWidth / 2) - (this.toggleButton.getWidth() / 2));
        this.toggleButton.setY(y);
        this.toggleButton.render(context, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean mouseClicked (double mouseX, double mouseY, int button) {
        if (this.toggleButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public List<Element> children () { return this.childrenList; }
    @Override
    public List<Selectable> selectableChildren () { return this.selectableList; }
}