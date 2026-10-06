package github.loloweliim.configfifteenth.gui.widget.provider;

import github.loloweliim.configfifteenth.config.CallbackManager;
import github.loloweliim.configfifteenth.config.ConfigManager;
import github.loloweliim.configfifteenth.gui.ConfigGuiScreen;
import github.loloweliim.configfifteenth.gui.cache.CachedConfigField;
import github.loloweliim.configfifteenth.gui.cache.metadata.FieldMetadata;
import github.loloweliim.configfifteenth.gui.widget.ColorWidget;
import github.loloweliim.configfifteenth.util.StringValidator;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.lang.reflect.Field;

public class StringWidgetProvider implements WidgetProvider {
    private static final int TEXT_FIELD_COLOR_NORMAL = 0xE0E0E0;
    private static final int TEXT_FIELD_COLOR_ERROR  = 0xFF5555;

    @Override
    public ClickableWidget create (Screen screen, CachedConfigField cachedField, TextRenderer textRenderer, int width, int height) {
        Field field = cachedField.getField();
        FieldMetadata meta = cachedField.getMetadata();

        Object targetInstance = meta.isStatic() ? null : ConfigManager.getConfig(field.getDeclaringClass());

        if (meta.isColor()) {
            ColorWidget colorWidget = new ColorWidget(textRenderer, 0, 0, width, 16, field, targetInstance, meta.hasAlpha());
            if (meta.tooltip() != null) colorWidget.setTooltip(meta.tooltip());
            return colorWidget;
        }

        String currentValue;
        try {
            String rawValue = (String) field.get(targetInstance);
            currentValue = (rawValue != null) ? rawValue : "";
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            currentValue = "";
        }

        TextFieldWidget textField = new TextFieldWidget(textRenderer, 0, 0, width - 3, 16, meta.displayName());

        textField.setMaxLength(meta.maxStringLength());
        textField.setEditable(true);
        textField.active = true;

        final StringValidator validator = new StringValidator(field);
        final Tooltip defaultTooltip = meta.tooltip();

        textField.setChangedListener(text -> {
            StringValidator.ValidationResult result = validator.validate(text);

            textField.setEditableColor(result.isValid() ? TEXT_FIELD_COLOR_NORMAL : TEXT_FIELD_COLOR_ERROR);
            textField.setTooltip(result.isValid() ? defaultTooltip : Tooltip.of(result.getAsText()));

            if (screen instanceof ConfigGuiScreen) ((ConfigGuiScreen) screen).setFieldValidity(field, result.isValid());
            if (result.isValid()) {
                try {
                    field.set(targetInstance, text);
                    Object rootConfig = ConfigManager.getConfig(field.getDeclaringClass());
                    CallbackManager.triggerFieldChangeCallback(rootConfig, field.getName(), text);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        textField.setText(currentValue);
        textField.setPlaceholder(Text.of("Введите " + meta.displayName().getString() + "..."));

        if (meta.tooltip() != null && textField.getTooltip() == null) textField.setTooltip(meta.tooltip());
        return textField;
    }
}