package github.loloweliim.configfifteenth.gui.widget;

import github.loloweliim.configfifteenth.config.CallbackManager;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.Locale;

public class SliderWidget extends net.minecraft.client.gui.widget.SliderWidget {
    private final Field field;
    private final Object configInstance;
    private final boolean isDiscrete;

    private long minLong, maxLong;
    private double minDouble, maxDouble;

    public SliderWidget (int x, int y, int width, int height, Field field, Object configInstance, long min, long max) {
        super(x, y, width, height, Text.empty(), getInitialProgressLong(field, configInstance, min, max));
        this.field = field;
        this.configInstance = configInstance;
        this.minLong = min;
        this.maxLong = max;
        this.isDiscrete = true;
        this.updateMessage();
    }

    public SliderWidget (int x, int y, int width, int height, Field field, Object configInstance, double min, double max) {
        super(x, y, width, height, Text.empty(), getInitialProgressDouble(field, configInstance, min, max));
        this.field = field;
        this.configInstance = configInstance;
        this.minDouble = min;
        this.maxDouble = max;
        this.isDiscrete = false;
        this.updateMessage();
    }

    private static double getInitialProgressLong (Field field, Object instance, long min, long max) {
        try {
            long current = field.getLong(instance);
            if (max == min) return 0.0;
            return (double) (current - min) / (double) (max - min);
        } catch (IllegalAccessException e) {
            return 0.0;
        }
    }

    private static double getInitialProgressDouble (Field field, Object instance, double min, double max) {
        try {
            double current = field.getDouble(instance);
            if (max == min) return 0.0;
            return (current - min) / (max - min);
        } catch (IllegalAccessException e) {
            return 0.0;
        }
    }

    @Override
    protected void applyValue () {
        try {
            if (isDiscrete) {
                long realValue = minLong + Math.round(this.value * (maxLong - minLong));
                if (field.getType() == int.class || field.getType() == Integer.class) {
                    field.setInt(configInstance, (int) realValue);
                } else {
                    field.setLong(configInstance, realValue);
                }
            } else {
                double realValue = minDouble + (this.value * (maxDouble - minDouble));
                if (field.getType() == float.class || field.getType() == Float.class) {
                    field.setFloat(configInstance, (float) realValue);
                } else {
                    field.setDouble(configInstance, realValue);
                }
            }
            Object newValue = field.get(configInstance);
            CallbackManager.triggerFieldChangeCallback(configInstance, field.getName(), newValue);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void updateMessage () {
        try {
            Object current = field.get(configInstance);
            if (!isDiscrete && current instanceof Double) {
                current = String.format(Locale.US, "%.2f", (Double) current);
            } else if (!isDiscrete && current instanceof Float) {
                current = String.format(Locale.US, "%.2f", (Float) current);
            }
            this.setMessage(Text.literal(field.getName() + ": " + current));
        } catch (IllegalAccessException e) {
            this.setMessage(Text.literal(field.getName()));
        }
    }
}