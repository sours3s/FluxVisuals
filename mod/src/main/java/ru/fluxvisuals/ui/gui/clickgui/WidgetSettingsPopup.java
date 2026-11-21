package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ScreenResolution;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.List;

public class WidgetSettingsPopup extends AbstractWindow {
    public final SettingsGroupWindow settingsWindow;
    public final String widgetName;

    public WidgetSettingsPopup(String str, List<Setting> list) {
        super(290.0f, 48.0f);
        this.widgetName = str;
        this.settingsWindow = new SettingsGroupWindow(Translation.clearText(str), list, false);
        addChild(this.settingsWindow);
        visible(false);
    }

    public boolean matchesWidget(String str) {
        return this.widgetName.equals(str);
    }

    public boolean isOpen() {
        return this.settingsWindow.isOpen();
    }

    public void openAt(float f, float f2, float f3, float f4, ScreenResolution screenResolution, float f5) {
        ScreenResolution screenResolution710 = screenResolution != null ? screenResolution : ScreenResolution.resolution();
        float fScreenWidth = screenResolution710.screenWidth();
        float fScreenHeight = screenResolution710.screenHeight();
        if (screenResolution == null && f5 > 0.0f) {
            fScreenWidth /= f5;
            fScreenHeight /= f5;
        }
        float fWidth = this.settingsWindow.width();
        float f6 = this.settingsWindow.totalHeight();
        float f7 = (f - 8.0f) - fWidth;
        float f8 = f + f3 + 8.0f;
        if (f8 + fWidth > fScreenWidth - 5.0f && f7 >= 5.0f) {
            f8 = f7;
        }
        float fMax = Math.max(5.0f, Math.min(f8, (fScreenWidth - fWidth) - 5.0f));
        float f9 = f2;
        if (f9 + f6 > fScreenHeight - 5.0f) {
            f9 = (fScreenHeight - f6) - 5.0f;
        }
        if (f9 < 5.0f) {
            f9 = 5.0f;
        }
        setPosition(fMax, f9);
        this.settingsWindow.setPosition(fMax, f9);
        open();
        this.settingsWindow.openWindow();
    }

    public void closeWindow() {
        this.settingsWindow.closeWindow();
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.settingsWindow.width(), this.settingsWindow.totalHeight());
        this.settingsWindow.setPosition(x(), y());
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        super.animation(weightedEngine);
        if (!visible() || this.settingsWindow.isOpen()) {
            return;
        }
        super.close();
    }
}
