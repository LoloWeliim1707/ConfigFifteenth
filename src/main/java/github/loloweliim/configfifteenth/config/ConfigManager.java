package github.loloweliim.configfifteenth.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Map<Class<?>, ConfigData> REGISTERED_CONFIGS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends ConfigData> T registerAndLoad (Class<T> configClass) {
        Class<?> rootClass = Reflector.getRootConfigClass(configClass);

        return (T) REGISTERED_CONFIGS.computeIfAbsent(rootClass, clazz -> {
            ConfigData instance = DiskHandler.load(clazz);
            instance.validatePostLoad();
            Reflector.applyStaticFieldsDeep(clazz, instance);
            return instance;
        });
    }

    public static void reload (Class<?> configClass) {
        Class<?> rootClass = Reflector.getRootConfigClass(configClass);
        ConfigData configInstance = DiskHandler.load(rootClass);
        configInstance.validatePostLoad();
        Reflector.applyStaticFieldsDeep(rootClass, configInstance);
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
        DiskHandler.save(configInstance);
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