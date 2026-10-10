package github.loloweliim.configfifteenth.config;

import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Reflector {
    private static final Logger LOGGER = LoggerFactory.getLogger("Config_Fifteenth");

    @SuppressWarnings("unchecked")
    public static <T> @NotNull T createNewInstance (Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (T) constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать дефолтный инстанс для " + clazz.getName() + ". Убедитесь, что у класса есть пустой конструктор.", e);
        }
    }

    public static void applyStaticFieldsDeep (Class<?> clazz, Object loadedObject) {
        if (clazz == null || clazz.isEnum()) return;
        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);

                int modifiers = field.getModifiers();

                if (Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
                    Object cleanValue = field.get(loadedObject);
                    if (cleanValue != null) field.set(null, cleanValue);
                } else if (field.getType().isMemberClass() && Modifier.isStatic(field.getType().getModifiers()) && !field.getType().isEnum()) {
                    Object subObject = field.get(loadedObject);
                    if (subObject != null) applyStaticFieldsDeep(field.getType(), subObject);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось применить статические поля для класса {}", clazz.getName(), e);
        }
    }

    public static Class<?> getRootConfigClass (Class<?> configClass) {
        if (configClass ==  null) throw new IllegalArgumentException("Класс конфигурации не может быть null!");

        Class<?> currentClass = configClass;
        while (currentClass != null && !ConfigData.class.isAssignableFrom(currentClass)) currentClass = currentClass.getEnclosingClass();
        if (currentClass == null || !currentClass.isAnnotationPresent(ConfigOptions.Config.class)) {
            throw new IllegalArgumentException("Класс " + configClass.getSimpleName() + " или его родитель должен быть помечен аннотацией @Config!");
        }
        return currentClass;
    }
}
