package github.loloweliim.configfifteenth.gui.widget.provider;

import github.loloweliim.configfifteenth.config.ConfigManager;
import github.loloweliim.configfifteenth.gui.cache.CachedConfigField;
import github.loloweliim.configfifteenth.gui.cache.metadata.FieldMetadata;
import github.loloweliim.configfifteenth.gui.widget.SliderWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class IntegerWidgetProvider implements WidgetProvider {

    @Override
    public ClickableWidget create (Screen screen, @NotNull CachedConfigField cachedField, TextRenderer textRenderer, int width, int height) {
        Field field = cachedField.getField();
        FieldMetadata meta = cachedField.getMetadata();

        Object targetInstance = meta.isStatic() ? null : ConfigManager.getConfig(field.getDeclaringClass());

        SliderWidget slider = new SliderWidget(0, 0, width, height, field, targetInstance, meta.minLong(), meta.maxLong());

        if (meta.tooltip() != null) {
            slider.setTooltip(meta.tooltip());
        }
        return slider;
    }
}