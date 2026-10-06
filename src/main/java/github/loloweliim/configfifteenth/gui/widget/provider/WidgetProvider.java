package github.loloweliim.configfifteenth.gui.widget.provider;

import github.loloweliim.configfifteenth.gui.cache.CachedConfigField;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;

@FunctionalInterface
public interface WidgetProvider {
    ClickableWidget create (
            Screen screen,
            CachedConfigField cachedField,
            TextRenderer textRenderer,
            int width,
            int height
    );
}
