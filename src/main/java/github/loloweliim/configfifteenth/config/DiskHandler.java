package github.loloweliim.configfifteenth.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import github.loloweliim.configfifteenth.util.CommentAdapter;
import github.loloweliim.configfifteenth.util.MathValidator;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class DiskHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Config_Fifteenth");

    private static final Gson SAVE_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Object.class, new CommentAdapter<>())
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE)
            .setPrettyPrinting()
            .create();

    private static final Gson READ_GSON = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE)
            .create();

    public static ConfigData load (Class<?> rootClass) {
        String configFileName = rootClass.getAnnotation(ConfigOptions.Config.class).name() + ".json";
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve(configFileName);

        if (Files.exists(configPath)) {
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                return (ConfigData) READ_GSON.fromJson(reader, rootClass);
            } catch (Exception e) {
                LOGGER.error("Ошибка при чтении конфига {}, создаю дефолтный. Бэкап сохранен в .broken", configFileName, e);
                try {
                    Files.move(configPath, configPath.resolveSibling(configFileName + ".broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ioException) {
                    LOGGER.error("Не удалось создать бэкап поврежденного конфига", ioException);
                }
            }
        }
        ConfigData configInstance = Reflector.createNewInstance(rootClass);
        save(configInstance);
        return configInstance;
    }

    public static void save (ConfigData configInstance) {
        if (configInstance == null) return;
        MathValidator.validate(configInstance);

        Class<?> configClass = configInstance.getClass();
        if (!configClass.isAnnotationPresent(ConfigOptions.Config.class)) return;
        String configFileName = configClass.getAnnotation(ConfigOptions.Config.class).name() + ".json";

        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve(configFileName);
        Path tempPath = configDir.resolve(configFileName + ".tmp");

        CompletableFuture.runAsync(() -> {
            synchronized (configFileName.intern()) {
                try {
                    Files.createDirectories(configDir);
                    try (BufferedWriter writer = Files.newBufferedWriter(tempPath)) {
                        SAVE_GSON.toJson(configInstance, writer);
                    }
                    Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception e) {
                    LOGGER.error("Критическая ошибка асинхронной записи конфига {}", configFileName, e);
                    try {
                        Files.deleteIfExists(tempPath);
                    } catch (IOException ignored) {}
                }
            }
        }).exceptionally(throwable -> {
            LOGGER.error("Фоновый поток сохранения завершился с ошибкой", throwable);
            return null;
        });
    }
}
