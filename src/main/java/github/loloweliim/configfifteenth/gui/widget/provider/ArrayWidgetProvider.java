package github.loloweliim.configfifteenth.gui.widget.provider;

import github.loloweliim.configfifteenth.config.ConfigData;
import github.loloweliim.configfifteenth.config.ConfigManager;
import github.loloweliim.configfifteenth.gui.ConfigGuiScreen;
import github.loloweliim.configfifteenth.gui.cache.CachedConfigField;
import github.loloweliim.configfifteenth.gui.cache.metadata.FieldMetadata;
import github.loloweliim.configfifteenth.gui.widget.ArrayEditScreen;
import github.loloweliim.configfifteenth.gui.widget.WidgetFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class ArrayWidgetProvider implements WidgetProvider {

    @Override
    public ClickableWidget create (Screen screen, @NotNull CachedConfigField cachedField, TextRenderer textRenderer, int width, int height) {
        Field field = cachedField.getField();
        FieldMetadata meta = cachedField.getMetadata();

        Object targetInstance = meta.isStatic() ? null : ConfigManager.getConfig(field.getDeclaringClass());
        ConfigData rootConfig = ConfigManager.getConfig(field.getDeclaringClass());

        int elementCount = WidgetFactory.getElementCount(field);

        String baseName = meta.displayName().getString();
        Text buttonText = Text.literal(baseName + " [" + elementCount + " эл.]");

        ButtonWidget arrayButton = ButtonWidget.builder(buttonText, button -> {
            Identifier backgroundTexture = null;
            if (screen instanceof  ConfigGuiScreen) backgroundTexture = ((ConfigGuiScreen) screen).getCustomBackgroundTexture();

            MinecraftClient.getInstance().setScreen(new ArrayEditScreen(
                    screen,
                    field,
                    targetInstance,
                    backgroundTexture,
                    buttonText,
                    () -> {
                        ConfigManager.save(rootConfig);
                        if (screen instanceof ConfigGuiScreen) ((ConfigGuiScreen) screen).getConfigCache().clearWidgetCache();

                        int newCount = WidgetFactory.getElementCount(field);
                        button.setMessage(Text.literal(baseName + " [" + newCount + " эл.]"));

                        if (screen != null) screen.init(MinecraftClient.getInstance(), screen.width, screen.height);
                    }
            ));
        }).dimensions(0, 0, width,  height).build();
        if (meta.tooltip() != null) arrayButton.setTooltip(meta.tooltip());
        return arrayButton;
    }
}