# 🛠️ Config Fifteenth

**Config Fifteenth** — это ультра-легковесная, быстрая и автономная библиотека конфигураций для модов **Minecraft (Fabric)**, написанная на Java 17. Она предоставляет удобный графический интерфейс в стиле *Ванильного Minecraft*, избавляя вас от необходимости писать громоздкий GUI-код вручную.

## 🚀 Главные особенности библиотеки

* **⚡ Нулевая задержка (Асинхронность):** Физическое сохранение JSON файлов на диск полностью изолировано в фоновом потоке. Игра больше никогда не дёрнется (0 фризов) при нажатии кнопки «Сохранить».
* **🧠 Умное кэширование метаданных:** Все аннотации, тултипы и лимиты парсятся рефлексией строго один раз при холодном старте, разгружая Render-поток игры до максимума.
* **📦 Ленивая инициализация виджетов:** Графические элементы интерфейса создаются «на лету» только во время скроллинга и мгновенно выгружаются из ОЗУ при закрытии меню.
* **🔄 Реактивные коллбэки «на лету»:** Поддержка живых триггеров. Игра может мгновенно реагировать на движение слайдера или клик тумблера ещё до того, как пользователь нажал кнопку «Сохранить».
* **🛡️ Всеядная архитектура:** Безупречная поддержка как классических статических (`static`) полей, так и полей объектов, а также вложенных категорий и спойлеров (`@Collapsible`).

---

## 📅 Спецификация аннотаций

Библиотека полностью управляется декларативными аннотациями над полями вашего класса конфигурации.

### Главные маркеры
* `@ConfigOptions.Config(name = "mod_id")` — Вешается над корневым классом конфигурации. Задаёт имя сохраняемого файла (`mod_id.json`).
* `@ConfigOptions.Category("Имя")` — Переключает текущий контекст сборщика на указанную вкладку/категорию настроек.
* `@ConfigOptions.Collapsible("Имя группы")` — Оборачивает вложенный статический класс в красивый раскрывающийся спойлер.

### Ограничения и валидация
* `@ConfigOptions.Comment("Описание")` — Добавляет к элементу всплывающую подсказку (Tooltip) в интерфейсе.
* `@ConfigOptions.RangeInt(min = 1, max = 100)` / `@ConfigOptions.RangeLong` — Задаёт границы для числовых значений и автоматически превращает поле ввода в удобный **Слайдер (Slider)**.
* `@ConfigOptions.RangeFloat(min = 0.0f, max = 1.0f)` / `@ConfigOptions.RangeDouble` — Аналогичные слайдеры для дробных чисел.
* `@ConfigOptions.MaxStringLength(value = 32)` — Ограничивает максимальное количество символов для ввода в текстовое поле.
* `@ConfigOptions.ValidateString(regex = "^[a-zA-Z0-9_]+$", errorKey = "Неверный формат строки!")` — Проверяет вводимый текст через **Регулярные выражения (Regex)**. В случае невалидного ввода подсвечивает поле красным цветом и блокирует кнопку «Сохранить».
* `@ConfigOptions.Color(alpha = true)` — Автоматически вешается над текстовой строкой и превращает её в полноценную интерактивную **палитру выбора RGBA/RGB цвета**.
* `@ConfigOptions.Name("Красивое имя")` — Позволяет задать элементам или константам перечислений (`Enum`) кастомное имя в GUI.

---

## 💻 Пример использования

Создайте класс конфигурации, реализующий интерфейс-маркер `ConfigData`:

```java
import github.loloweliim.configfifteenth.config.ConfigData;
import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;

@ConfigOptions.Config(name = "meowconfig")
public class MeowConfig implements ConfigData {

    @ConfigOptions.Category("Основные")
    @ConfigOptions.Comment("Включает или выключает отображение HUD элементов")
    public static boolean displayHud = true;

    @ConfigOptions.Comment("Координата X для рендеринга текста на экране")
    @ConfigOptions.RangeInt(min = 0, max = 1920)
    public static int hudX = 10;

    @ConfigOptions.Category("Графика")
    @ConfigOptions.Collapsible("Расширенные настройки")
    public static final AdvancedGraphics ADVANCED = new AdvancedGraphics();

    public static class AdvancedGraphics {
        @ConfigOptions.Comment("Дальность прорисовки эффектов частиц")
        @ConfigOptions.RangeInt(min = 1, max = 16)
        public static int effectRenderDistance = 8;

        @ConfigOptions.Comment("Использовать размытие в движении")
        public static boolean motionBlur = false;
    }
}
```

### Инициализация при старте мода
В главном классе вашего мода вызовите загрузку всего одной строчкой:

```java
public class Meow implements ModInitializer {
    public static MeowConfig CONFIG;

    @Override
    public void onInitialize() {
        // Читает JSON с диска или автоматически генерирует дефолтный файл
        CONFIG = ConfigManager.registerAndLoad(MeowConfig.class);
    }
}
```

---

## ⚡ Реактивная система коллбэков

Вы можете заставить код вашего мода мгновенно реагировать на любые изменения в GUI с помощью аннотации `@ConfigOptions.Callback`.

### 1. Живой коллбэк поля (Срабатывает «на лету» во время ввода)
Идеально подходит для мгновенного перемещения элементов по экрану при движении ползунка:

```java
// Привязывается строго к имени переменной "hudX"
@ConfigOptions.Callback(field = "hudX")
public static void onXCoordinateChanged(int newXValue) {
    // Этот код вызывается в ту же миллисекунду, когда игрок сдвинул слайдер!
    System.out.println("HUD перемещён на позицию: " + newXValue);
}
```

### 2. Глобальный коллбэк (Срабатывает при успешном сохранении)
Вызывается только тогда, когда пользователь нажал кнопку «Сохранить» или «Готово»:

```java
@ConfigOptions.Callback // Без указания поля field
public static void onConfigSaved() {
    // Идеально для обновления тяжелых систем, перезапуска потоков или проигрывания звуков
    MinecraftClient.getInstance().getSoundManager().play(CustomSounds.MEOW_EVENT);
}
```