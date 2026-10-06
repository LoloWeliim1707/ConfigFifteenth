package github.loloweliim.configfifteenth.util;

import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

public class StringValidator {
    private final Pattern pattern;
    private final String errorMsg;

    public StringValidator (Field field) {
        if (field != null && field.isAnnotationPresent(ConfigOptions.ValidateString.class)) {
            ConfigOptions.ValidateString annotation = field.getAnnotation(ConfigOptions.ValidateString.class);
            String regex = annotation.regex();

            this.pattern = (regex != null && !regex.isEmpty()) ? Pattern.compile(regex) : null;
            this.errorMsg = annotation.errorMsg().isEmpty() ? "Неверный формат текста!" : annotation.errorMsg();
        } else {
            this.pattern = null;
            this.errorMsg = "Неверный формат текста!";
        }
    }

    public ValidationResult validate (String text) {
        if (text == null || text.trim().isEmpty()) return new ValidationResult(false, "Строка не может быть пустой!");
        if (this.pattern != null && !this.pattern.matcher(text).matches()) return new ValidationResult(false, this.errorMsg);

        return ValidationResult.VALID;
    }

    public static class ValidationResult {
        public static final ValidationResult VALID = new ValidationResult(true, "");

        private final boolean isValid;
        private final String errorMessage;

        public ValidationResult (boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid () { return isValid; }
        public Text getAsText () { return Text.literal("§c" + this.errorMessage); }
    }
}
