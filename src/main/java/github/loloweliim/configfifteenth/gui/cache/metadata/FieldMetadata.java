package github.loloweliim.configfifteenth.gui.cache.metadata;

import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public record FieldMetadata (
        boolean isStatic,
        Text displayName,
        Tooltip tooltip,
        long minLong,
        long maxLong,
        double minDouble,
        double maxDouble,
        int maxStringLength,
        boolean isColor,
        boolean hasAlpha
) {

    public static FieldMetadata of (Field field) {
        field.setAccessible(true);
        boolean isStatic = Modifier.isStatic(field.getModifiers());

        Text displayName = field.isAnnotationPresent(ConfigOptions.Name.class)
                ? Text.literal(field.getAnnotation(ConfigOptions.Name.class).value())
                : Text.literal(field.getName());

        Tooltip tooltip = field.isAnnotationPresent(ConfigOptions.Comment.class)
                ? Tooltip.of(Text.literal(field.getAnnotation(ConfigOptions.Comment.class).value()))
                : null;

        long minLong = Long.MIN_VALUE;
        long maxLong = Long.MAX_VALUE;
        if (field.isAnnotationPresent(ConfigOptions.RangeInt.class)) {
            minLong = field.getAnnotation(ConfigOptions.RangeInt.class).min();
            maxLong = field.getAnnotation(ConfigOptions.RangeInt.class).max();
        } else if (field.isAnnotationPresent(ConfigOptions.RangeLong.class)) {
            minLong = field.getAnnotation(ConfigOptions.RangeLong.class).min();
            maxLong = field.getAnnotation(ConfigOptions.RangeLong.class).max();
        }

        double minDouble = Double.NEGATIVE_INFINITY;
        double maxDouble = Double.POSITIVE_INFINITY;
        if (field.isAnnotationPresent(ConfigOptions.RangeFloat.class)) {
            minDouble = field.getAnnotation(ConfigOptions.RangeFloat.class).min();
            maxDouble = field.getAnnotation(ConfigOptions.RangeFloat.class).max();
        } else if (field.isAnnotationPresent(ConfigOptions.RangeDouble.class)) {
            minDouble = field.getAnnotation(ConfigOptions.RangeDouble.class).min();
            maxDouble = field.getAnnotation(ConfigOptions.RangeDouble.class).max();
        }

        int maxStrLen = field.isAnnotationPresent(ConfigOptions.MaxStringLength.class)
                ? field.getAnnotation(ConfigOptions.MaxStringLength.class).value()
                : 256;

        boolean isColor = field.isAnnotationPresent(ConfigOptions.Color.class);
        boolean hasAlpha = isColor && field.getAnnotation(ConfigOptions.Color.class).alpha();

        return new FieldMetadata(
                isStatic, displayName, tooltip,
                minLong, maxLong, minDouble, maxDouble,
                maxStrLen, isColor, hasAlpha
        );
    }
}
