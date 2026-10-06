package github.loloweliim.configfifteenth.gui.widget.provider;

import github.loloweliim.configfifteenth.config.CallbackManager;
import github.loloweliim.configfifteenth.config.ConfigManager;
import github.loloweliim.configfifteenth.gui.cache.CachedConfigField;
import github.loloweliim.configfifteenth.gui.cache.metadata.FieldMetadata;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.lang.reflect.Field;

public class BooleanWidgetProvider implements WidgetProvider {

    @Override
    public ClickableWidget create (Screen screen, CachedConfigField cachedField, TextRenderer textRenderer, int width, int height) {
        Field field = cachedField.getField();
        FieldMetadata meta = cachedField.getMetadata();

        Object targetInstance = meta.isStatic() ? null : ConfigManager.getConfig(field.getDeclaringClass());

        boolean initialValue = true;
        try {
            initialValue = field.getBoolean(targetInstance);
        }  catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        CyclingButtonWidget<Boolean> toggleButton = CyclingButtonWidget.onOffBuilder(initialValue)
                .build(0, 0, width, height, meta.displayName(), (button, value) -> {
                    try {
                        field.setBoolean(targetInstance, value);
                        Object rootConfig = ConfigManager.getConfig(field.getDeclaringClass());
                        CallbackManager.triggerFieldChangeCallback(rootConfig, field.getName(), value);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });
        if (meta.tooltip() != null) {
            toggleButton.setTooltip(meta.tooltip());
        }
        return toggleButton;
    }
}