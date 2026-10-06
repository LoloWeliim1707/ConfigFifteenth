package github.loloweliim.configfifteenth.config.cache;

import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallbackCache {
    //private static final Logger LOGGER = LoggerFactory.getLogger(CallbackCache.class.getName());
    private static final Map<Class<?>, Map<String, List<Method>>> fieldCallbackMap = new HashMap<>();
    private static final Map<Class<?>, List<Method>> globalCallbackMap = new HashMap<>();

    public static void ensureCache (Class<?> clazz) {
        if (fieldCallbackMap.containsKey(clazz)) return;

        Map<String, List<Method>> fieldMap = new HashMap<>();
        List<Method> globalList = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(ConfigOptions.Callback.class)) continue;
            method.setAccessible(true);
            ConfigOptions.Callback annotation = method.getAnnotation(ConfigOptions.Callback.class);
            String targetField = annotation.field();

            if (targetField.isEmpty()) {
                globalList.add(method);
            } else {
                fieldMap.computeIfAbsent(targetField, k -> new ArrayList<>()).add(method);
            }
        }
        fieldCallbackMap.put(clazz, fieldMap);
        globalCallbackMap.put(clazz, globalList);
        //LOGGER.info("[CONFIG-CACHE] Успешно запечены методы @Callback для класса: {}", clazz.getSimpleName());
    }

    public static List<Method> getFieldCallbacks (Class<?> clazz, String fieldName) {
        ensureCache(clazz);
        Map<String, List<Method>> fieldMap = fieldCallbackMap.get(clazz);
        return fieldMap != null ? fieldMap.get(fieldName) : null;
    }

    public static List<Method> getGlobalCallbacks (Class<?> clazz) {
        ensureCache(clazz);
        return globalCallbackMap.get(clazz);
    }

//    public static void clear () {
//        fieldCallbackMap.clear();
//        globalCallbackMap.clear();
//    }
}
