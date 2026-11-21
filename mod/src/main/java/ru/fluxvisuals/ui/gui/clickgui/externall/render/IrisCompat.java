package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

public final class IrisCompat {

    private static boolean checked = false;
    private static boolean present = false;
    private static Object apiInstance;
    private static Method isShaderPackInUseMethod;

    private IrisCompat() {}

    private static void init() {
        if (checked) return;
        checked = true;

        boolean modPresent = FabricLoader.getInstance().isModLoaded("iris")
                || FabricLoader.getInstance().isModLoaded("oculus");
        if (!modPresent) return;

        String[] apiClasses = {
                "net.irisshaders.iris.api.v0.IrisApi",
                "net.coderbot.iris.api.v0.IrisApi"
        };

        for (String className : apiClasses) {
            try {
                Class<?> apiClass = Class.forName(className);
                Method getInstance = apiClass.getMethod("getInstance");
                apiInstance = getInstance.invoke(null);
                isShaderPackInUseMethod = apiClass.getMethod("isShaderPackInUse");
                present = true;
                return;
            } catch (Throwable ignored) {

            }
        }
    }

    public static boolean shadersActive() {
        init();
        if (!present) return false;
        try {
            Object result = isShaderPackInUseMethod.invoke(apiInstance);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

