package github.loloweliim.configfifteenth.util;

import github.loloweliim.configfifteenth.gui.ConfigGuiScreen;
import github.loloweliim.configfifteenth.gui.cache.CachedConfigField;
import github.loloweliim.configfifteenth.gui.cache.ConfigCache;
import github.loloweliim.configfifteenth.gui.widget.CollapsibleEntry;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ListBuilder {
    public static void build (ConfigGuiScreen.ConfigListWidget listWidget, Object configInstance, String currentCategory, List<String> expandedGroups, ConfigGuiScreen screen, ConfigCache cache) {
        List<ClickableWidget> rowBuffer = new ArrayList<>();

        for (CachedConfigField cachedField : cache.getCachedFields()) {
            if (!cachedField.getCategory().equals(currentCategory)) continue;
            Field field = cachedField.getField();
            if (cachedField.isCollapsible()) {
                flushRowBuffer(listWidget, rowBuffer);
                try {
                    Object subInstance = field.get(configInstance);
                    if (subInstance == null) continue;

                    String groupTitle = cachedField.getCollapsibleTitle();
                    CollapsibleEntry collapsible = new CollapsibleEntry(screen, groupTitle);

                    boolean isCurrentlyExpanded = expandedGroups.contains(groupTitle);
                    collapsible.setExpandedState(isCurrentlyExpanded);

                    List<ClickableWidget> subRowBuffer = new ArrayList<>();

                    for (CachedConfigField cachedSubField : cachedField.getSubFields()) {
                        ClickableWidget subWidget = cache.getOrCreateWidget(cachedSubField, screen, screen.getTextRenderer(), 140);
                        if (subWidget != null) {
                            subWidget.setWidth(140);
                            subRowBuffer.add(subWidget);

                            if (subRowBuffer.size() == 2) {
                                collapsible.addSubEntry(new ConfigGuiScreen.ConfigEntry(subRowBuffer.get(0), subRowBuffer.get(1)));
                                subRowBuffer.clear();
                            }
                        }
                    }
                    if (!subRowBuffer.isEmpty()) {
                        collapsible.addSubEntry(new ConfigGuiScreen.ConfigEntry(subRowBuffer.get(0)));
                    }

                    listWidget.addWidgetEntry(collapsible);

                    if (isCurrentlyExpanded) {
                        for (ConfigGuiScreen.ConfigEntry subEntry : collapsible.getSubEntries()) {
                            listWidget.addWidgetEntry(subEntry);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                ClickableWidget widget = cache.getOrCreateWidget(cachedField, screen, screen.getTextRenderer(), 150);

                if (widget != null) {
                    rowBuffer.add(widget);

                    if (rowBuffer.size() == 2) {
                        listWidget.addWidgetEntry(new ConfigGuiScreen.ConfigEntry(rowBuffer.get(0), rowBuffer.get(1)));
                        rowBuffer.clear();
                    }
                }
            }
        }

        flushRowBuffer(listWidget, rowBuffer);
    }

    private static void flushRowBuffer (ConfigGuiScreen.ConfigListWidget listWidget, List<ClickableWidget> rowBuffer) {
        if (rowBuffer.isEmpty()) return;
        if (rowBuffer.size() == 1) {
            listWidget.addWidgetEntry(new ConfigGuiScreen.ConfigEntry(rowBuffer.get(0)));
        } else {
            listWidget.addWidgetEntry(new ConfigGuiScreen.ConfigEntry(rowBuffer.get(0), rowBuffer.get(1)));
        }
        rowBuffer.clear();
    }
}