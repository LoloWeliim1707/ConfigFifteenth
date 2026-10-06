package github.loloweliim.configfifteenth.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.lang.reflect.Field;

public class ColorWidget extends TextFieldWidget {
    private final boolean supportAlpha;
    private int currentColor = 0xFFFFFFFF;

    public ColorWidget (TextRenderer textRenderer, int x, int y, int width, int height, Field field, Object configInstance, boolean supportAlpha) {
        super(textRenderer, x, y, width - 25, height, Text.literal(field.getName()));
        this.supportAlpha = supportAlpha;
        this.setMaxLength(supportAlpha ? 9 : 7);
        try {
            String hex = (String) field.get(configInstance);
            if (hex == null || hex.isEmpty()) hex = supportAlpha ? "#FFFFFFFF" : "#FFFFFF";
            this.setText(hex);
            this.currentColor = parseColor(hex);
        } catch (IllegalAccessException e) {
            this.setText("#FFFFFF");
        }
        this.setChangedListener(text -> {
            if (!text.startsWith("#")) {
                text = "#" + text.replace("#", "");
                this.setText(text);
            }

            this.currentColor = parseColor(text);
            try {
                field.set(configInstance, text);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    private int parseColor (String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() < 2) return 0xFF000000;
        String rawHex = hex.substring(1);
        if (!rawHex.matches("^[0-9a-fA-F]+$")) {
            return this.currentColor;
        }

        try {
            long parsed = Long.parseLong(rawHex, 16);
            if (!supportAlpha || hex.length() == 7) {
                return (int) (parsed | 0xFF000000L);
            }
            return (int) parsed;
        } catch (NumberFormatException e) {
            return 0xFF000000;
        }
    }

    @Override
    public void renderButton (DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderButton(context, mouseX, mouseY, delta);

        int squareX = this.getX() + this.getWidth() - 16;
        int squareY = this.getY() - 1;
        int squareSize = this.getHeight() + 2;

        context.fill(squareX, squareY, squareX + squareSize, squareY + squareSize, 0xFFA0A0A0);
        context.fill(squareX + 1, squareY + 1, squareX + squareSize - 1, squareY + squareSize - 1, this.currentColor);
    }

    @Override
    public int getWidth () { return 147; }
}
