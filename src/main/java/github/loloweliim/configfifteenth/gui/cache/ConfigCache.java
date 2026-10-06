package github.loloweliim.configfifteenth.gui.cache;

import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import github.loloweliim.configfifteenth.gui.widget.WidgetFactory;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigCache {
    private final List<CachedConfigField> cachedFields = new ArrayList<>();
    private final List<String> explicitCategories = new ArrayList<>();
    private boolean hasAnyExplicitCategories = false;

    private final Map<CachedConfigField, ClickableWidget> widgetMap = new HashMap<>();

    public ConfigCache (Object configInstance) {
        String activeCategory = "Другое";
        for (Field field : configInstance.getClass().getDeclaredFields()) {
            if (field.getName().startsWith("_comment_")) continue;
            if (field.isAnnotationPresent(ConfigOptions.Category.class)) {
                String categoryName = field.getAnnotation(ConfigOptions.Category.class).value();
                this.hasAnyExplicitCategories = true;
                if (!this.explicitCategories.contains(categoryName)) this.explicitCategories.add(categoryName);
                activeCategory = categoryName;
            }
            this.cachedFields.add(new CachedConfigField(field, activeCategory));
        }
        if (this.hasAnyExplicitCategories) this.explicitCategories.add("Другое");
    }

    public ClickableWidget getOrCreateWidget (CachedConfigField cachedField, Screen screen, TextRenderer textRenderer, int width) {
        if (this.widgetMap.containsKey(cachedField)) {
            return this.widgetMap.get(cachedField);
        }

        ClickableWidget newWidget = WidgetFactory.createWidget(screen, cachedField, textRenderer, width);
        if (newWidget != null) {
            this.widgetMap.put(cachedField, newWidget);
        }
        return newWidget;
    }

    public List<CachedConfigField> getCachedFields () { return cachedFields; }
    public List<String> getExplicitCategories () { return explicitCategories; }
    public boolean hasAnyExplicitCategories () { return hasAnyExplicitCategories; }
    public void clearWidgetCache() { this.widgetMap.clear(); }
}