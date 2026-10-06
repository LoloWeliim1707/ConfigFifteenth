package github.loloweliim.configfifteenth.util;

import com.google.gson.*;
import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class CommentAdapter<T> implements JsonSerializer<T> {
    private static final Gson CLEAN_GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public JsonElement serialize (T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        Class<?> clazz = src.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                Object fieldValue = field.get(src);
                if (field.isAnnotationPresent(ConfigOptions.Comment.class)) {
                    ConfigOptions.Comment comment = field.getAnnotation(ConfigOptions.Comment.class);
                    jsonObject.addProperty("_comment_" + fieldName, comment.value());
                }
                jsonObject.add(fieldName, CLEAN_GSON.toJsonTree(fieldValue));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return jsonObject;
    }
}
