package github.loloweliim.configfifteenth.gui.widget.provider;

import github.loloweliim.configfifteenth.config.CallbackManager;
import github.loloweliim.configfifteenth.config.ConfigManager;
import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import github.loloweliim.configfifteenth.gui.cache.CachedConfigField;
import github.loloweliim.configfifteenth.gui.cache.metadata.FieldMetadata;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.lang.reflect.Field;

public class EnumWidgetProvider implements WidgetProvider {

    @Override
    public ClickableWidget create (Screen screen, CachedConfigField cachedField, TextRenderer textRenderer, int width, int height) {
        Field field = cachedField.getField();
        FieldMetadata meta = cachedField.getMetadata();

        Object targetInstance = meta.isStatic() ? null : ConfigManager.getConfig(field.getDeclaringClass());

        try {
            Class<?> type = field.getType();
            Object[] enumConstants = type.getEnumConstants();
            Object currentValue = field.get(targetInstance);

            CyclingButtonWidget<Object> enumButton = CyclingButtonWidget.builder(value -> {
                try {
                    String name = value.toString();
                    Field enumField = type.getField(name);
                    if (enumField.isAnnotationPresent(ConfigOptions.Name.class)) return Text.literal(enumField.getAnnotation(ConfigOptions.Name.class).value());
                } catch (Exception ignored) {}
                return Text.literal(value.toString());
            })
            .values(enumConstants)
            .initially(currentValue)
            .build(0, 0, width, height, meta.displayName(), (button, newValue) -> {
                try {
                    field.set(targetInstance, newValue);
                    Object rootConfig = ConfigManager.getConfig(field.getDeclaringClass());
                    CallbackManager.triggerFieldChangeCallback(rootConfig, field.getName(), newValue);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
            if (meta.tooltip() != null) enumButton.setTooltip(meta.tooltip());
            return enumButton;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}