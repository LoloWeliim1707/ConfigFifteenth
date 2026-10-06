package github.loloweliim.configfifteenth.gui.widget;


import github.loloweliim.configfifteenth.config.ConfigManager;
import github.loloweliim.configfifteenth.gui.cache.CachedConfigField;
import github.loloweliim.configfifteenth.gui.widget.provider.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class WidgetFactory {
    private static final List<ConfigRegistryEntry> REGISTRY = new ArrayList<>();

    static {
        register(type -> List.class.isAssignableFrom(type) || type == String[].class, new ArrayWidgetProvider());
        register(type -> type == boolean.class || type == Boolean.class, new BooleanWidgetProvider());
        register(type -> type == double.class || type == Double.class, new DoubleWidgetProvider());
        register(type -> type == int.class || type == Integer.class, new IntegerWidgetProvider());
        register(String.class::isAssignableFrom, new StringWidgetProvider());
        register(Class::isEnum, new EnumWidgetProvider());
    }

    private static void register (Predicate<Class<?>> typeChecker, WidgetProvider provider) {
        REGISTRY.add(new ConfigRegistryEntry(typeChecker, provider));
    }

    public static ClickableWidget createWidget (Screen screen, CachedConfigField cachedField, TextRenderer textRenderer, int width) {
        Class<?> type = cachedField.getField().getType();
        WidgetProvider provider = null;

        for (ConfigRegistryEntry entry : REGISTRY) {
            if (entry.typeChecker().test(type)) {
                provider = entry.provider();
                break;
            }
        }
        if (provider == null) return null;
        return provider.create(screen, cachedField, textRenderer, width, 20);
    }

    public static int getElementCount (Field field) {
        try {
            field.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(field.getModifiers());

            Object targetInstance = isStatic ? null : ConfigManager.getConfig(field.getDeclaringClass());
            Object value = field.get(targetInstance);

            if (value instanceof List<?>) {
                return ((List<?>) value).size();
            } else if (value instanceof Object[]) {
                return ((Object[]) value).length;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private record ConfigRegistryEntry(Predicate<Class<?>> typeChecker, WidgetProvider provider) {}
}