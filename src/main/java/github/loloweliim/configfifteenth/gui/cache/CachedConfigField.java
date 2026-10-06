package github.loloweliim.configfifteenth.gui.cache;

import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import github.loloweliim.configfifteenth.gui.cache.metadata.FieldMetadata;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CachedConfigField {
    private final Field field;
    private final String category;

    private final boolean isCollapsible;
    private final String collapsibleTitle;

    private final List<CachedConfigField> subFields = new ArrayList<>();

    private final FieldMetadata metadata;

    public CachedConfigField (@NotNull Field field, String activeCategory) {
        this.field = field;
        field.setAccessible(true);

        if (field.isAnnotationPresent(ConfigOptions.Category.class)) {
            this.category = field.getAnnotation(ConfigOptions.Category.class).value();
        } else {
            this.category = activeCategory;
        }

        this.isCollapsible = field.isAnnotationPresent(ConfigOptions.Collapsible.class);
        if (this.isCollapsible) {
            this.collapsibleTitle = field.getAnnotation(ConfigOptions.Collapsible.class).value();

            Class<?> subClass = field.getType();
            for (Field subField : subClass.getDeclaredFields()) {
                if (subField.getName().startsWith("_comment_")) continue;
                this.subFields.add(new CachedConfigField(subField, this.category));
            }
        } else {
            this.collapsibleTitle = "";
        }
        this.metadata = FieldMetadata.of(field);
    }

    public Field getField () { return field; }
    public String getCategory () { return category; }
    public boolean isCollapsible () { return isCollapsible; }
    public String getCollapsibleTitle () { return collapsibleTitle; }
    public List<CachedConfigField> getSubFields () { return this.subFields; }
    public FieldMetadata getMetadata () { return metadata; }
}