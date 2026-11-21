package ru.fluxvisuals.ui.gui.clickgui.externall.render.font;

import java.util.LinkedHashMap;
import java.util.Map;

public class Fonts {

    private static final Map<String, String> FONT_REGISTRY = new LinkedHashMap<>();

    public static final Font sf_bold = register("sf_bold", "sf_bold");
    public static final Font sf_medium = register("sf_medium", "sf_medium");
    public static final Font font = register("font", "font");

    public static final Font icon = register("nightix", "nightix");
    public static final Font icon1 = register("icon1", "icon1");
    public static final Font category = register("category", "category");
    public static final Font gui = register("icongui", "icongui");
    public static final Font nightix_2 = register("nightix_2", "nightix_2");
    public static final Font Exclusive = register("Exclsuive", "Exclsuive");
    public static final Font gt_regular = register("gt_regular", "gt_regular");
    public static final Font mini = register("mini", "mini");
    public static final Font test = register("test", "test");
    public static final Font hud = register("icons", "icons");
    public static final Font mainmenu = register("sacramento", "sacramento");
    public static final Font clickgui = register("clickgui", "clickgui");



    private static Font register(String name, String path) {
        FONT_REGISTRY.put(name, path);
        return new Font(name);
    }

    public static Map<String, String> getRegistry() {
        return FONT_REGISTRY;
    }

    private Fonts() {}
}
