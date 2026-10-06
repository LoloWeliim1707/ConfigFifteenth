package github.loloweliim.configfifteenth.util;

import github.loloweliim.configfifteenth.config.CallbackManager;
import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class MathValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Config_Fifteenth");

    @FunctionalInterface
    private interface ValueSetter { void set () throws IllegalAccessException; }

    public static <T> boolean validate (T configInstance) {
        boolean wasModified = false;
        Class<?> clazz = configInstance.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Class<?> type = field.getType();

            try {
                if (field.getAnnotations().length == 0) {
                    continue;
                }

                if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
                    long currentVal, min, max;
                    if (type == int.class || type == Integer.class) {
                        ConfigOptions.RangeInt r = field.getAnnotation(ConfigOptions.RangeInt.class);
                        if (r == null) continue;
                        currentVal = field.getInt(configInstance);
                        min = r.min();
                        max = r.max();
                        final long targetMin = min;
                        final long targetMax = max;
                        wasModified = checkAndLogLongBounds(configInstance, field.getName(), currentVal, min, max,
                                () -> setLongValue(field, configInstance, type, targetMin),
                                () -> setLongValue(field, configInstance, type, targetMax)) || wasModified;
                    } else {
                        ConfigOptions.RangeLong r = field.getAnnotation(ConfigOptions.RangeLong.class);
                        if (r == null) continue;
                        currentVal = field.getLong(configInstance);
                        min = r.min();
                        max = r.max();
                        final long targetMin = min;
                        final long targetMax = max;
                        wasModified = checkAndLogLongBounds(configInstance, field.getName(), currentVal, min, max,
                                () -> setLongValue(field, configInstance, type, targetMin),
                                () -> setLongValue(field, configInstance, type, targetMax)) || wasModified;
                    }
                }
                else if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
                    double currentVal, min, max;
                    if (type == float.class || type == Float.class) {
                        ConfigOptions.RangeFloat r = field.getAnnotation(ConfigOptions.RangeFloat.class);
                        if (r == null) continue;
                        currentVal = field.getFloat(configInstance);
                        min = r.min();
                        max = r.max();
                        final double targetMin = min;
                        final double targetMax = max;
                        wasModified = checkAndLogDoubleBounds(configInstance, field.getName(), currentVal, min, max,
                                () -> setDoubleValue(field, configInstance, type, targetMin),
                                () -> setDoubleValue(field, configInstance, type, targetMax)) || wasModified;
                    } else {
                        ConfigOptions.RangeDouble r = field.getAnnotation(ConfigOptions.RangeDouble.class);
                        if (r == null) continue;
                        currentVal = field.getDouble(configInstance);
                        min = r.min();
                        max = r.max();
                        final double targetMin = min;
                        final double targetMax = max;
                        wasModified = checkAndLogDoubleBounds(configInstance, field.getName(), currentVal, min, max,
                                () -> setDoubleValue(field, configInstance, type, targetMin),
                                () -> setDoubleValue(field, configInstance, type, targetMax)) || wasModified;
                    }
                }
            }
            catch (Exception e) {
                LOGGER.error("Не удалось валидировать поле '{}' из-за ошибки в данных.", field.getName(), e);
            }
        }
        return wasModified;
    }

    private static void setLongValue (Field field, Object instance, Class<?> type, long val) throws IllegalAccessException {
        if (type == int.class || type == Integer.class) field.setInt(instance, (int) val);
        else field.setLong(instance, val);
    }

    private static void setDoubleValue (Field field, Object instance, Class<?> type, double val) throws IllegalAccessException {
        if (type == float.class || type == Float.class) field.setFloat(instance, (float) val);
        else field.setDouble(instance, val);
    }

    private static boolean checkAndLogLongBounds (Object configInstance, String fieldName,
                                                  long currentVal, long min, long max,
                                                  ValueSetter onMinExceeded, ValueSetter onMaxExceeded
    ) throws IllegalAccessException {
        if (currentVal < min) {
            LOGGER.warn("Поле '{}' ({}) меньше минимума ({}).", fieldName, currentVal, min);
            onMinExceeded.set();
            CallbackManager.triggerFieldChangeCallback(configInstance, fieldName, min);
            return true;
        } else if (currentVal > max) {
            LOGGER.warn("Поле '{}' ({}) больше максимума ({}).", fieldName, currentVal, max);
            onMaxExceeded.set();
            CallbackManager.triggerFieldChangeCallback(configInstance, fieldName, max);
            return true;
        }
        return false;
    }

    private static boolean checkAndLogDoubleBounds (Object configInstance, String fieldName,
                                                    double currentVal, double min, double max,
                                                    ValueSetter onMinExceeded, ValueSetter onMaxExceeded
    ) throws IllegalAccessException {
        if (currentVal < min) {
            LOGGER.warn("Поле '{}' ({}) меньше минимума ({}).", fieldName, currentVal, min);
            onMinExceeded.set();
            CallbackManager.triggerFieldChangeCallback(configInstance, fieldName, min);
            return true;
        } else if (currentVal > max) {
            LOGGER.warn("Поле '{}' ({}) больше максимума ({}).", fieldName, currentVal, max);
            onMaxExceeded.set();
            CallbackManager.triggerFieldChangeCallback(configInstance, fieldName, max);
            return true;
        }
        return false;
    }
}