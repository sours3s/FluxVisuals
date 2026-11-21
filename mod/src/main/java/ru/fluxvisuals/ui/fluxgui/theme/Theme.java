package ru.fluxvisuals.ui.fluxgui.theme;

import java.awt.Color;

public class Theme {
    public static final int DEFAULT_TEST_GUI_SECTIONS_COLOR = new Color(70, 70, 76, 255).getRGB();
    private final String name;
    private final boolean mutable;
    private int guiIconColor = -9868802;
    private int hudIconColor = -1;
    private int categoryNameColor = -855310;
    private int enabledModuleNameColor = -1;
    private int disabledModuleNameColor = -16783642;
    private int hudTextColor = -1;
    private int hudListTextColor = -3618616;
    private int hudTitleColor = -16764161;
    private int sliderColor = -9324296;
    private int sliderKnobColor = -1;
    private int tooltipTextColor = -3618616;
    private int tooltipBackgroundColor = -602663911;
    private int hudBackgroundColor = -16777216;
    private int buttonsColor = -1273094621;
    private int enabledModuleButtonColor = 1198617071;
    private int disabledModuleButtonColor = -15198181;
    private int panelColor = -132243938;
    private int sectionBackgroundColor = -15790321;
    private int visualModulesColor = -8224006;

    public Theme(String name, boolean mutable) {
        this.name = name;
        this.mutable = mutable;
    }

    public String getName() {
        return this.name;
    }

    public boolean isMutable() {
        return this.mutable;
    }

    public int getGuiIconColor() {
        return this.guiIconColor;
    }

    public int getHudIconColor() {
        return this.hudIconColor;
    }

    public int getCategoryNameColor() {
        return this.categoryNameColor;
    }

    public int getEnabledModuleNameColor() {
        return this.enabledModuleNameColor;
    }

    public int getDisabledModuleNameColor() {
        return this.disabledModuleNameColor;
    }

    public int getHudTextColor() {
        return this.hudTextColor;
    }

    public int getHudListTextColor() {
        return this.hudListTextColor;
    }

    public int getHudTitleColor() {
        return this.hudTitleColor;
    }

    public int getSliderColor() {
        return this.sliderColor;
    }

    public int getSliderKnobColor() {
        return this.sliderKnobColor;
    }

    public int getTooltipTextColor() {
        return this.tooltipTextColor;
    }

    public int getTooltipBackgroundColor() {
        return this.tooltipBackgroundColor;
    }

    public int getHudBackgroundColor() {
        return this.hudBackgroundColor;
    }

    public int getButtonsColor() {
        return this.buttonsColor;
    }

    public int getEnabledModuleButtonColor() {
        return this.enabledModuleButtonColor;
    }

    public int getDisabledModuleButtonColor() {
        return this.disabledModuleButtonColor;
    }

    public int getPanelColor() {
        return this.panelColor;
    }

    public int getSectionBackgroundColor() {
        return this.sectionBackgroundColor;
    }

    public int getVisualModulesColor() {
        return this.visualModulesColor;
    }

    public int getTestGuiSectionsColor() {
        return DEFAULT_TEST_GUI_SECTIONS_COLOR;
    }

    public int getPrimaryColor() {
        return this.sliderColor;
    }

    public void setGuiIconColor(int color) {
        if (this.mutable) {
            this.guiIconColor = color;
        }
    }

    public void setHudIconColor(int color) {
        if (this.mutable) {
            this.hudIconColor = color;
        }
    }

    public void setCategoryNameColor(int color) {
        if (this.mutable) {
            this.categoryNameColor = color;
        }
    }

    public void setEnabledModuleNameColor(int color) {
        if (this.mutable) {
            this.enabledModuleNameColor = color;
        }
    }

    public void setDisabledModuleNameColor(int color) {
        if (this.mutable) {
            this.disabledModuleNameColor = color;
        }
    }

    public void setHudTextColor(int color) {
        if (this.mutable) {
            this.hudTextColor = color;
        }
    }

    public void setHudListTextColor(int color) {
        if (this.mutable) {
            this.hudListTextColor = color;
        }
    }

    public void setHudTitleColor(int color) {
        if (this.mutable) {
            this.hudTitleColor = color;
        }
    }

    public void setSliderColor(int color) {
        if (this.mutable) {
            this.sliderColor = color;
        }
    }

    public void setSliderKnobColor(int color) {
        if (this.mutable) {
            this.sliderKnobColor = color;
        }
    }

    public void setTooltipTextColor(int color) {
        if (this.mutable) {
            this.tooltipTextColor = color;
        }
    }

    public void setTooltipBackgroundColor(int color) {
        if (this.mutable) {
            this.tooltipBackgroundColor = color;
        }
    }

    public void setHudBackgroundColor(int color) {
        if (this.mutable) {
            this.hudBackgroundColor = color;
        }
    }

    public void setButtonsColor(int color) {
        if (this.mutable) {
            this.buttonsColor = color;
        }
    }

    public void setEnabledModuleButtonColor(int color) {
        if (this.mutable) {
            this.enabledModuleButtonColor = color;
        }
    }

    public void setDisabledModuleButtonColor(int color) {
        if (this.mutable) {
            this.disabledModuleButtonColor = color;
        }
    }

    public void setPanelColor(int color) {
        if (this.mutable) {
            this.panelColor = color;
        }
    }

    public void setSectionBackgroundColor(int color) {
        if (this.mutable) {
            this.sectionBackgroundColor = color;
        }
    }

    public void setVisualModulesColor(int color) {
        if (this.mutable) {
            this.visualModulesColor = color;
        }
    }

    public void setTestGuiSectionsColor(int color) {
    }
}
