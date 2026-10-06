package github.loloweliim.configfifteenth.config;

import github.loloweliim.configfifteenth.config.cache.CallbackCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

public class CallbackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Config_Fifteenth");

    public static <T> void triggerConfigReloadCallbacks (T configInstance) {
        if (configInstance == null) return;
        List<Method> methods = CallbackCache.getGlobalCallbacks(configInstance.getClass());
        if (methods != null) {
            for (Method method : methods) {
                try {
                    method.invoke(configInstance);
                } catch (Exception e) {
                    LOGGER.error("Не удалось вызвать глобальный коллбэк в методе '{}'", method.getName(), e);
                }
            }
        }
    }

    public static <T> void triggerFieldChangeCallback (T configInstance, String fieldName, Object newValue) {
        if (configInstance == null || fieldName == null) return;
        List<Method> methods = CallbackCache.getFieldCallbacks(configInstance.getClass(), fieldName);
        if (methods != null) {
            for (Method method : methods) {
                try {
                    int paramCount = method.getParameterCount();
                    Object[] args = new Object[paramCount];
                    if (paramCount > 0) args[paramCount - 1] = newValue;
                    method.invoke(configInstance, args);
                } catch (Exception e) {
                    LOGGER.error("Ошибка вызова коллбэка для поля '{}' в методе '{}'", fieldName, method.getName(), e);
                }
            }
        }
    }
}