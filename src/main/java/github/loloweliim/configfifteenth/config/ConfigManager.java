package github.loloweliim.configfifteenth.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import github.loloweliim.configfifteenth.util.CommentAdapter;
import github.loloweliim.configfifteenth.util.MathValidator;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Config_Fifteenth");
    private static final Map<Class<?>, ConfigData> REGISTERED_CONFIGS = new HashMap<>();

    private static final Gson SAVE_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Object.class, new CommentAdapter<>())
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE)
            .setPrettyPrinting()
            .create();

    private static final Gson READ_GSON = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE)
            .create();

    @SuppressWarnings("unchecked")
    public static <T extends ConfigData> T registerAndLoad (Class<T> configClass) {
        Class<?> rootClass = getRootConfigClass(configClass);

        if (REGISTERED_CONFIGS.containsKey(rootClass)) return (T) REGISTERED_CONFIGS.get(rootClass);

        T configInstance = (T) loadFromDiskAndApply(rootClass);
        REGISTERED_CONFIGS.put(rootClass, configInstance);
        return configInstance;
    }

    public static void reload (Class<?> configClass) {
        Class<?> rootClass = getRootConfigClass(configClass);
        ConfigData configInstance = loadFromDiskAndApply(rootClass);
        REGISTERED_CONFIGS.put(rootClass, configInstance);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ConfigData> T getConfig (Class<?> configClass) {
        if (configClass == null) throw new IllegalArgumentException("Класс конфигурации не может быть null!");

        Class<?> currentClass = configClass;
        while (currentClass != null && !ConfigData.class.isAssignableFrom(currentClass)) {
            currentClass = currentClass.getEnclosingClass();
        }
        if (currentClass == null) {
            throw new IllegalArgumentException("Класс " + configClass.getSimpleName() + " или его внешние классы не реализуют интерфейс ConfigData!");
        }
        return (T) REGISTERED_CONFIGS.get(currentClass);
    }

    public static void save (ConfigData configInstance) {
        if (configInstance == null) return;
        MathValidator.validate(configInstance);

        Class<?> configClass = configInstance.getClass();
        if (!configClass.isAnnotationPresent(ConfigOptions.Config.class)) return;
        String configFileName = configClass.getAnnotation(ConfigOptions.Config.class).name() + ".json";

        Path configDir = FabricLoader.getInstance().getConfigDir();
        File configFile = configDir.resolve(configFileName).toFile();

        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(configDir);
                try (FileWriter writer = new FileWriter(configFile)) {
                    SAVE_GSON.toJson(configInstance, writer);
                }
            } catch (Exception e) {
                LOGGER.error("Критическая ошибка асинхронной записи конфига {}", configFileName, e);
            }
        }).exceptionally(throwable -> {
            LOGGER.error("Фоновый поток сохранения завершился с ошибкой", throwable);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> @NotNull T createNewInstance (Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (T) constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать дефолтный инстанс для " + clazz.getName() + ". Убедитесь, что у класса есть пустой конструктор.", e);
        }
    }

    private static void applyStaticFieldsDeep (Class<?> clazz, Object loadedObject) {
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

    private static ConfigData loadFromDiskAndApply (Class<?> rootClass) {
        String configFileName = rootClass.getAnnotation(ConfigOptions.Config.class).name() + ".json";
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File configFile = configDir.resolve(configFileName).toFile();

        ConfigData configInstance = null;

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                configInstance = (ConfigData) READ_GSON.fromJson(reader, rootClass);
            } catch (Exception e) {
                LOGGER.error("Ошибка при чтении конфига {}, создаю дефолтный.", configFileName, e);
            }
        }
        if (configInstance == null) {
            configInstance = createNewInstance(rootClass);
            save(configInstance);
        }
        configInstance.validatePostLoad();
        applyStaticFieldsDeep(rootClass, configInstance);
        return configInstance;
    }

    private static Class<?> getRootConfigClass (Class<?> configClass) {
        if (configClass ==  null) throw new IllegalArgumentException("Класс конфигурации не может быть null!");

        Class<?> currentClass = configClass;
        while (currentClass != null && !ConfigData.class.isAssignableFrom(currentClass)) currentClass = currentClass.getEnclosingClass();
        if (currentClass == null || !currentClass.isAnnotationPresent(ConfigOptions.Config.class)) {
            throw new IllegalArgumentException("Класс " + configClass.getSimpleName() + " или его родитель должен быть помечен аннотацией @Config!");
        }
        return currentClass;
    }
}

//TODO:
// Если посмотреть на популярные аналоги (например, Cloth Config API, YACL (Yet Another Config Lib) или MidnightConfig),
// то у них есть ряд крутых возможностей, которые мы еще не реализовали, но которые очень бы пригодились разработчикам модов [Cloth Config API]:
// 1. Кнопка сброса значения по умолчанию (Reset to Default)Как у аналогов:
//     * Рядом с каждым ползунком, текстовым полем или кнопкой горит маленькая круглая стрелочка ↺.
//       Если игрок на неё нажимает, виджет мгновенно сбрасывается к значению, которое разработчик прописал в коде изначально как дефолтное.
//     * Зачем: Если игрок запутался в настройках или выставил плохой цвет/громкость, он может сбросить эту настройку в один клик,
//       не сбрасывая весь остальной конфиг.
// 3. Полноценный поиск по настройкам (Search Bar)Как у аналогов:
//     * Вверху экрана настроек добавляется небольшая строка поиска.
//       Игрок пишет туда, например, Volume, и список моментально фильтрует виджеты, оставляя только те,
//       в названии или описании (@Comment) которых есть это слово.
//     * Зачем: Незаменимо для огромных модов (технологических или магических),
//       где количество настроек исчисляется десятками.
// 4. Разделители текста и декоративные заголовки (@Header)Как у аналогов:
//     * Аннотация, которая не создает виджет ввода, а просто рисует красивую серую текстовую строчку-разделитель
//       (например, === Настройки рендеринга ===) прямо посреди списка ползунков.
//     * Зачем: Помогает визуально разделять блоки настроек внутри одной и той же категории.
// 6. Можно добавить поддержку json5. (рассмотреть вариант XML)
//     * Зачем: Чтобы комменты комментами были а не полями с припиской _comment_.
// 8. Автоматический перезапуск коллбэков при изменении файла на диске (File Watcher)
//     * Это абсолютный топ для дебага модов. Разработчик меняет значения x или y в файле meow_config.json через Notepad++,
//       нажимает Ctrl+S, и HUD в Майнкрафте смещается сам, без перезапуска игры и без открытия меню!
//     * Как реализовать: В фоновом потоке запускается стандартный Java WatchService, который следит за папкой конфигов.
//       Как только файл мода обновился, библиотека «тихо» вызывает ConfigManager.load() и пинает CallbackManager [1.25].