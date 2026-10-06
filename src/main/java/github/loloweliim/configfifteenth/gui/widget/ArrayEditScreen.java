package github.loloweliim.configfifteenth.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import github.loloweliim.configfifteenth.config.annotation.ConfigOptions;
import github.loloweliim.configfifteenth.gui.ConfigGuiScreen;
import github.loloweliim.configfifteenth.util.StringValidator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArrayEditScreen extends Screen {
    private final Screen parent;
    private final Field field;
    private final Object configInstance;
    private final Identifier customBackgroundTexture;
    private final StringValidator stringValidator;
    private final Runnable onSave;
    private Text listDisplayName;

    private ConfigGuiScreen.ConfigListWidget listWidget;
    private final List<String> elements = new ArrayList<>();
    private final List<TextFieldWidget> textFieldsBuffer = new ArrayList<>();
    private final Set<Integer> invalidIndexes = new HashSet<>();
    private ButtonWidget saveButton;

    private static final int TEXT_FIELD_COLOR_NORMAL = 0xE0E0E0;
    private static final int TEXT_FIELD_COLOR_ERROR  = 0xFF5555;

    public ArrayEditScreen (Screen parent, Field field, Object configInstance, Identifier customBackgroundTexture, Text listDisplayName, Runnable onSave) {
        super(Text.literal("Редактирование списка"));
        this.parent = parent;
        this.field = field;
        this.configInstance = configInstance;
        this.customBackgroundTexture = customBackgroundTexture;
        this.listDisplayName = listDisplayName;
        this.onSave = onSave;

        this.stringValidator = new StringValidator(field);
        String displayName = field.isAnnotationPresent(ConfigOptions.Name.class)
                ? field.getAnnotation(ConfigOptions.Name.class).value()
                : field.getName();
        try {
            Object value = field.get(configInstance);
            if (value instanceof List<?>) {
                for (Object obj : (List<?>) value) elements.add(obj != null ? obj.toString() : "");
            } else if (value instanceof String[]) {
                for (String str : (String[]) value) elements.add(str != null ? str : "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.listDisplayName = Text.literal(displayName + " [" + this.elements.size() + " эл.]");
    }

    public void setRowValidity (int index, boolean isValid) {
        if (isValid) {
            this.invalidIndexes.remove(index);
        } else {
            this.invalidIndexes.add(index);
        }
        if (this.saveButton != null) {
            this.saveButton.active = this.invalidIndexes.isEmpty();
        }
    }

    @Override
    protected void init () {
        this.listWidget = new ConfigGuiScreen.ConfigListWidget(this.client, this.width, this.height, 32, this.height - 32, 24);
        this.addDrawableChild(this.listWidget);
        this.rebuildList();

        int btnWidth = 90;
        int spacing = 5;
        int startX = (this.width - (btnWidth * 3 + spacing * 2)) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Добавить"), button -> {
            this.readTextFieldsIntoElements();
            this.elements.add("");
            this.rebuildList();

            if (this.saveButton != null) {
                this.saveButton.active = this.invalidIndexes.isEmpty();
            }
        }).dimensions(startX, this.height - 26, btnWidth, 20).build());

        this.saveButton = ButtonWidget.builder(Text.literal("Сохранить"), button -> {
            this.saveData();
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(startX + btnWidth + spacing, this.height - 26, btnWidth, 20).build();

        this.saveButton.active = this.invalidIndexes.isEmpty();
        this.addDrawableChild(this.saveButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Отмена"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(startX + (btnWidth + spacing) * 2, this.height - 26, btnWidth, 20).build());
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

        if (this.listWidget != null) {
            this.listWidget.render(context, mouseX, mouseY, delta);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        if (this.listDisplayName != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.listDisplayName, this.width / 2, 20, 0xA0A0A0);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void rebuildList () {
        if (this.listWidget == null) return;
        this.listWidget.clearAllWidgetEntries();
        this.textFieldsBuffer.clear();
        this.invalidIndexes.clear();

        var textRenderer = MinecraftClient.getInstance().textRenderer;

        for (int i = 0; i < elements.size(); i++) {
            final int index = i;

            TextFieldWidget textField = new TextFieldWidget(textRenderer, 0, 0, 140, 16, Text.empty());
            textField.setText(elements.get(index));
            textField.setDrawsBackground(true);
            textField.setEditable(true);
            textField.active = true;
            this.textFieldsBuffer.add(textField);

            StringValidator.ValidationResult initialResult = this.stringValidator.validate(elements.get(index));
            textField.setEditableColor(initialResult.isValid() ? TEXT_FIELD_COLOR_NORMAL :  TEXT_FIELD_COLOR_ERROR);
            if (!initialResult.isValid()) {
                this.invalidIndexes.add(index);
                textField.setTooltip(Tooltip.of(initialResult.getAsText()));
            }


            textField.setChangedListener(text -> {
                StringValidator.ValidationResult result = this.stringValidator.validate(text);

                textField.setEditableColor(result.isValid() ? TEXT_FIELD_COLOR_NORMAL : TEXT_FIELD_COLOR_ERROR);
                this.setRowValidity(index, result.isValid());
                textField.setTooltip(result.isValid() ? null : Tooltip.of(result.getAsText()));

                elements.set(index, text);
            });

            ButtonWidget deleteBtn = ButtonWidget.builder(Text.literal("X"), button -> {
                this.readTextFieldsIntoElements();
                this.invalidIndexes.remove(index);
                elements.remove(index);
                this.rebuildList();

                if (this.saveButton != null) this.saveButton.active = this.invalidIndexes.isEmpty();
            }).dimensions(0, 0, 20, 20).build();

            this.listWidget.addWidgetEntry(new ConfigGuiScreen.ConfigEntry(textField, deleteBtn));
        }
    }

    private void saveData () {
        try {
            this.readTextFieldsIntoElements();
            this.elements.removeIf(String::isBlank);

            field.set(configInstance, java.util.List.class.isAssignableFrom(field.getType()) ? new ArrayList<>(elements) : elements.toArray(new String[0]));

            if (this.onSave != null) this.onSave.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readTextFieldsIntoElements () {
        for (int i = 0; i < textFieldsBuffer.size(); i++) {
            if (i < elements.size()) {
                elements.set(i, textFieldsBuffer.get(i).getText());
            }
        }
    }
}

