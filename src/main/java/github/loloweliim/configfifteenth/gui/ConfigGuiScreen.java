package github.loloweliim.configfifteenth.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import github.loloweliim.configfifteenth.config.ConfigData;
import github.loloweliim.configfifteenth.config.ConfigManager;
import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import github.loloweliim.configfifteenth.gui.cache.ConfigCache;
import github.loloweliim.configfifteenth.config.CallbackManager;
import github.loloweliim.configfifteenth.util.ListBuilder;
import github.loloweliim.configfifteenth.util.MathValidator;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

public class ConfigGuiScreen extends Screen {
    private final Screen parent;
    private final Object configInstance;

    private String currentCategory = "";
    private Identifier customBackgroundTexture = null;
    private ConfigListWidget listWidget;

    private final List<String> expandedGroups = new ArrayList<>();
    private final ConfigCache configCache;

    private final Set<Field> invalidFields = new HashSet<>();
    private ButtonWidget mainSaveButton;

    private static final int CELL_WIDTH_STANDARD    = 150;
    private static final int CELL_WIDTH_ARRAY_FIELD = 140;
    private static final int BUTTON_DELETE_WIDTH    = 20;
    private static final int WIDGET_SPACING         = 5;
    private static final int TEXT_FIELD_PADDING_Y   = 2;

    public ConfigGuiScreen (Screen parent, String title, Object configInstance) {
        super(Text.literal(title));
        this.parent = parent;
        this.configInstance = configInstance;

        Class<?> configClass = configInstance.getClass();

        if (configClass.isAnnotationPresent(ConfigOptions.Background.class)) {
            this.customBackgroundTexture = new Identifier(configClass.getAnnotation(ConfigOptions.Background.class).value());
        }
        this.configCache = new ConfigCache(configInstance);

        if (!this.configCache.hasAnyExplicitCategories()) {
            this.currentCategory = "Другое";
        }
    }

    public void rebuildSettingsList () {
        if (this.listWidget == null) return;
        this.listWidget.clearAllWidgetEntries();
        ListBuilder.build(this.listWidget, this.configInstance, this.currentCategory, this.expandedGroups, this, this.configCache);
    }

    public boolean toggleGroupExpansion (String groupTitle) {
        if (this.expandedGroups.contains(groupTitle)) {
            this.expandedGroups.remove(groupTitle);
            return false;
        } else {
            this.expandedGroups.add(groupTitle);
            return true;
        }
    }

    public void setFieldValidity (Field field, boolean isValid) {
        if (isValid) {
            this.invalidFields.remove(field);
        } else {
            this.invalidFields.add(field);
        }

        if (this.mainSaveButton != null) {
            this.mainSaveButton.active = this.invalidFields.isEmpty();
        }
    }

    public void clearValidationErrors () {
        this.invalidFields.clear();
        if (this.mainSaveButton != null) this.mainSaveButton.active = true;
    }

    @Override
    protected void init () {
        this.clearChildren();
        if (this.currentCategory.isEmpty()) {
            initCategorySelectionScreen();
        } else {
            initSettingsScreen();
        }
    }

    @Override
    public void render (DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.customBackgroundTexture != null) {
            RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
            context.drawTexture(this.customBackgroundTexture, 0, 0, 0, 0, this.width, this.height, 32, 32);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            context.draw();
        } else if (this.client != null && this.client.world == null) {
            this.renderBackground(context);
        }

        if (this.listWidget != null) this.listWidget.render(context, mouseX, mouseY, delta);

        boolean hasCategories = this.configCache.hasAnyExplicitCategories();
        Text titleText = (this.currentCategory.isEmpty() || !hasCategories) ? this.title : Text.literal(this.currentCategory);
        context.drawCenteredTextWithShadow(this.textRenderer, titleText, this.width / 2, 12, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.expandedGroups.clear();
        this.clearValidationErrors();

        if (this.configCache != null) this.configCache.clearWidgetCache();
        if (this.configInstance != null) ConfigManager.reload(this.configInstance.getClass());
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private void initCategorySelectionScreen () {
        int btnWidth = CELL_WIDTH_STANDARD;
        int btnHeight = 20;
        int spacingX = 10;
        int spacingY = 4;
        int startX = this.width / 2 - btnWidth - (spacingX / 2);
        int startY = 30;

        List<String> categories = this.configCache.getExplicitCategories();
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            int column = i % 2;
            int row = i / 2;
            int x = startX + column * (btnWidth + spacingX);
            int y = startY + row * (btnHeight + spacingY);

            this.addDrawableChild(ButtonWidget.builder(Text.literal(category), button -> {
                this.currentCategory = category;
                this.clearAndInit();
            }).dimensions(x, y, btnWidth, btnHeight).build());
        }

        this.listWidget = null;

        int doneButtonX = this.width / 2 - 100;
        int bottomY = this.height - 28;
        int doneButtonWidth = 200;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), button -> {
            MathValidator.validate(configInstance);
            ConfigManager.save((ConfigData) configInstance);
            CallbackManager.triggerConfigReloadCallbacks(configInstance);
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(doneButtonX, bottomY, doneButtonWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("📁"), button -> {
            Util.getOperatingSystem().open(FabricLoader.getInstance().getConfigDir().toFile());

            this.clearAndInit();
        }).dimensions(doneButtonX + doneButtonWidth + 4, bottomY, 20, 20).build());
    }

    private void initSettingsScreen () {
        this.listWidget = new ConfigListWidget(this.client, this.width, this.height, 32, this.height - 42, 24);
        this.addSelectableChild(this.listWidget);
        this.rebuildSettingsList();

        int bottomBtnWidth = CELL_WIDTH_STANDARD;
        int bottomXStart = this.width / 2 - bottomBtnWidth - WIDGET_SPACING;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Отменить"), button -> {
            if (this.configInstance != null) ConfigManager.reload(this.configInstance.getClass());

            if (this.configCache != null) this.configCache.clearWidgetCache();
            this.clearValidationErrors();

            this.listWidget = null;
            this.expandedGroups.clear();

            assert this.configCache != null;
            boolean hasCategories = this.configCache.hasAnyExplicitCategories();

            this.currentCategory = hasCategories ? "" : "Другое";
            if (hasCategories) this.clearAndInit();
            else if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(bottomXStart + 2, this.height - 28, bottomBtnWidth, 20).build());

        this.mainSaveButton = ButtonWidget.builder(Text.literal("Сохранить"), button -> {
            MathValidator.validate(configInstance);
            ConfigManager.save((ConfigData) configInstance);
            CallbackManager.triggerConfigReloadCallbacks(configInstance);
            this.listWidget = null;
            this.expandedGroups.clear();

            boolean hasCategories = this.configCache.hasAnyExplicitCategories();

            this.currentCategory = hasCategories ? "" : "Другое";
            if (hasCategories) this.clearAndInit();
            else if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(bottomXStart + bottomBtnWidth + 10 + 2, this.height - 28, bottomBtnWidth, 20).build();

        this.mainSaveButton.active = this.invalidFields.isEmpty();
        this.addDrawableChild(this.mainSaveButton);
    }

    public Identifier getCustomBackgroundTexture () { return this.customBackgroundTexture; }
    public TextRenderer getTextRenderer () { return this.textRenderer; }
    public ConfigCache getConfigCache () { return this.configCache; }

    public static class ConfigEntry extends ElementListWidget.Entry<ConfigEntry> {
        private final ClickableWidget leftWidget;
        private final ClickableWidget rightWidget;
        private boolean isArrayRow = false;

        private final List<Element> childrenList;
        private final List<Selectable> selectableList;

        private Element focusedElement = null;

        public ConfigEntry (ClickableWidget widget) {
            ClickableWidget safeWidget = widget != null ? widget : ButtonWidget.builder(Text.empty(), b -> {
            }).dimensions(0, 0, 0, 0).build();
            if (widget == null) safeWidget.visible = false;

            this.leftWidget = safeWidget;
            this.rightWidget = null;

            this.childrenList = ImmutableList.of(safeWidget);
            this.selectableList = ImmutableList.of(safeWidget);
        }

        public ConfigEntry (ClickableWidget leftWidget, ClickableWidget rightWidget) {
            ClickableWidget safeLeft = leftWidget != null ? leftWidget : ButtonWidget.builder(Text.empty(), b -> {
            }).dimensions(0, 0, 0, 0).build();
            if (leftWidget == null) safeLeft.visible = false;

            ClickableWidget safeRight = rightWidget != null ? rightWidget : ButtonWidget.builder(Text.empty(), b -> {
            }).dimensions(0, 0, 0, 0).build();
            if (rightWidget == null) safeRight.visible = false;

            this.leftWidget = safeLeft;
            this.rightWidget = rightWidget == null ? null : safeRight;

            this.isArrayRow = leftWidget instanceof TextFieldWidget && rightWidget instanceof ButtonWidget && rightWidget.getWidth() == BUTTON_DELETE_WIDTH;

            this.childrenList = ImmutableList.of(safeLeft, safeRight);
            this.selectableList = ImmutableList.of(safeLeft, safeRight);
        }

        @Override
        public void render (DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int centerX = x + (entryWidth / 2);
            int spacing = 10;

            if (this.rightWidget == null) {
                renderSingleWidget(context, y, centerX, tickDelta, mouseX, mouseY);
            } else if (this.isArrayRow) {
                renderArrayRow(context, y, centerX, mouseX, mouseY, tickDelta);
            } else {
                renderStandardGrid(context, y, centerX, spacing, mouseX, mouseY, tickDelta);
            }
        }

        private void renderSingleWidget (DrawContext context, int y, int centerX, float delta, int mouseX, int mouseY) {
            if (this.leftWidget == null || !this.leftWidget.visible) return;

            int targetX = (this.leftWidget.getWidth() >= 310)
                    ? centerX - (this.leftWidget.getWidth() / 2)
                    : (centerX - CELL_WIDTH_STANDARD - WIDGET_SPACING) + ((CELL_WIDTH_STANDARD - this.leftWidget.getWidth()) / 2);

            int targetY = (this.leftWidget instanceof TextFieldWidget) ? y + TEXT_FIELD_PADDING_Y : y;
            drawWidget(context, this.leftWidget, targetX, targetY, mouseX, mouseY, delta);
        }

        private void renderArrayRow (DrawContext context, int y, int centerX, int mouseX, int mouseY, float delta) {
            int totalWidth = CELL_WIDTH_ARRAY_FIELD + WIDGET_SPACING + BUTTON_DELETE_WIDTH;
            int startX = centerX - (totalWidth / 2);

            drawWidget(context, this.leftWidget, startX, y + TEXT_FIELD_PADDING_Y, mouseX, mouseY, delta);
            drawWidget(context, this.rightWidget, startX + CELL_WIDTH_ARRAY_FIELD + WIDGET_SPACING, y, mouseX, mouseY, delta);
        }

        private void renderStandardGrid (DrawContext context, int y, int centerX, int spacing, int mouseX, int mouseY, float delta) {
            if (this.leftWidget != null && this.leftWidget.visible) {
                int cellX = centerX - CELL_WIDTH_STANDARD - (spacing / 2);
                int paddingX = (this.leftWidget.getWidth() < CELL_WIDTH_STANDARD) ? (CELL_WIDTH_STANDARD - this.leftWidget.getWidth()) / 2 : 0;
                int targetY = (this.leftWidget instanceof TextFieldWidget) ? y + TEXT_FIELD_PADDING_Y : y;

                drawWidget(context, this.leftWidget, cellX + paddingX, targetY, mouseX, mouseY, delta);
            }

            if (this.rightWidget != null && this.rightWidget.visible) {
                int cellX = centerX + (spacing / 2);
                int paddingX = (this.rightWidget.getWidth() < CELL_WIDTH_STANDARD) ? (CELL_WIDTH_STANDARD - this.rightWidget.getWidth()) / 2 : 0;
                int targetY = (this.rightWidget instanceof TextFieldWidget) ? y + TEXT_FIELD_PADDING_Y : y;

                drawWidget(context, this.rightWidget, cellX + paddingX, targetY, mouseX, mouseY, delta);
            }
        }

        private void drawWidget (DrawContext context, @NotNull ClickableWidget widget, int x, int y, int mouseX, int mouseY, float delta) {
            widget.setX(x);
            widget.setY(y);
            widget.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked (double mouseX, double mouseY, int button) {
            if (this.leftWidget != null && this.leftWidget.visible && this.leftWidget.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(this.leftWidget);
                return true;
            }
            if (this.rightWidget != null && this.rightWidget.visible && this.rightWidget.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(this.rightWidget);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged (double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            Element focusedElement = this.getFocused();
            if (focusedElement != null && focusedElement.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public void setFocused (Element focused) {
            this.focusedElement = focused;
            if (this.leftWidget != null) this.leftWidget.setFocused(this.leftWidget == focused);
            if (this.rightWidget != null) this.rightWidget.setFocused(this.rightWidget == focused);
        }

        @Override
        public Element getFocused () { return this.focusedElement; }

        @Override
        public boolean keyPressed (int keyCode, int scanCode, int modifiers) {
            if (this.focusedElement != null && this.focusedElement.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped (char chr, int modifiers) {
            if (this.focusedElement != null && this.focusedElement.charTyped(chr, modifiers)) {
                return true;
            }
            return super.charTyped(chr, modifiers);
        }

        @Override
        public List<Element> children () { return this.childrenList; }
        @Override
        public List<Selectable> selectableChildren () { return this.selectableList; }

    }

    public static class ConfigListWidget extends ElementListWidget<ConfigEntry> {
        public ConfigListWidget (MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);
        }

        public void addWidgetEntry (ConfigEntry entry) { this.addEntry(entry); }
        public void clearAllWidgetEntries () { this.clearEntries(); }

        @Override
        public int getRowWidth () { return 310; }
        @Override
        protected int getScrollbarPositionX () { return this.width / 2 + 160; }
        @Override
        protected int getMaxPosition () { return super.getMaxPosition() + 10; }

        @Override
        public void render (DrawContext context, int mouseX, int mouseY, float delta) {
            int availableHeight = this.bottom - this.top;
            if (this.getMaxPosition() <= availableHeight) {
                this.setScrollAmount(0);
            }

            this.updateScrollingState(mouseX, mouseY, 0);
            this.enableScissor(context);
            this.renderList(context, mouseX, mouseY, delta);
            context.disableScissor();

            if (this.getMaxPosition() > availableHeight) {
                int scrollbarX = this.getScrollbarPositionX();
                int maxScroll = this.getMaxScroll();

                if (maxScroll > 0) {
                    int scrollbarHeight = (int) ((float) (availableHeight * availableHeight) / (float) this.getMaxPosition());
                    scrollbarHeight = MathHelper.clamp(scrollbarHeight, 32, availableHeight - 8);
                    int scrollbarY = (int) this.getScrollAmount() * (availableHeight - scrollbarHeight) / maxScroll + this.top;

                    if (scrollbarY < this.top) scrollbarY = this.top;

                    context.fill(scrollbarX, this.top, scrollbarX + 6, this.bottom, 0xFF000000);
                    context.fill(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0xFF808080);
                    context.fill(scrollbarX, scrollbarY, scrollbarX + 5, scrollbarY + scrollbarHeight - 1, 0xFFC0C0C0);
                }
            }
        }

        @Override
        protected void renderList (DrawContext context, int mouseX, int mouseY, float delta) {
            int itemHeight = this.itemHeight;
            int listTop = this.top;

            for (int i = 0; i < this.getEntryCount(); i++) {
                ConfigEntry entry = this.getEntry(i);
                int entryY = listTop + 4 - (int) this.getScrollAmount() + (i * itemHeight);

                if (entryY + itemHeight < this.top || entryY > this.bottom) {
                    continue;
                }

                int entryX = this.getRowLeft();
                boolean isHovered = this.isMouseOver(mouseX, mouseY) && entry.isMouseOver(mouseX, mouseY);
                entry.render(context, i, entryY, entryX, this.getRowWidth(), itemHeight, mouseX, mouseY, isHovered, delta);
            }
        }
    }
}