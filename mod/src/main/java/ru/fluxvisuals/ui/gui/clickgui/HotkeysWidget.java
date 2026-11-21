package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.DragRenderContext;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.setting.ExpandableSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.type.HotkeyCategory;
import ru.fluxvisuals.ui.gui.clickgui.util.model.HotkeyEntry;
import ru.fluxvisuals.ui.gui.clickgui.util.KeyboardUtil;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;
import ru.fluxvisuals.ui.gui.clickgui.ClickGuiModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScreenClickContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.setting.MultiSelectSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;

public class HotkeysWidget extends Draggable {
    public final MsdfFont semiBoldFont;
    public final MsdfFont boldFont;
    public final GlTexture starsTexture;
    public final GlTexture buttonsIcon;
    public final GlTexture frameIcon;
    public final AnimatedFloat opacityAnimation;
    public final List<HotkeyEntry> entries;
    public final Map<String, HotkeyEntry> entryMap;
    public final WidgetBounds headerBounds;
    public final MultiSelectSetting<HotkeyCategory> hiddenCategoriesSetting;
    public float width;
    public float height;

    public HotkeysWidget(BooleanSupplier booleanSupplier) {
        super("Hotkeys", booleanSupplier);
        this.semiBoldFont = Fonts.INTER_SEMIBOLD.get();
        this.boldFont = Fonts.INTER_EXTRA_BOLD.get();
        this.starsTexture = new GlTexture(new ClasspathResource("/textures/stars.png"));
        this.buttonsIcon = new GlTexture(new ClasspathResource("/icons/menu/new/buttons.png"));
        this.frameIcon = new GlTexture(new ClasspathResource("/icons/menu/new/frame.png"));
        this.opacityAnimation = new AnimatedFloat(200, Easings.LINEAR);
        this.entries = new ArrayList();
        this.entryMap = new HashMap();
        this.headerBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 19.0f);
        this.hiddenCategoriesSetting = new MultiSelectSetting(Lang.WIDGET_HOTKEYS_HIDDEN_CATEGORIES).values(HotkeyCategory.class);
        this.width = 161.0f;
        this.height = 31.0f;
        this.x = 10.0f;
        this.y = 49.0f;
        addSettings(this.hiddenCategoriesSetting);
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    @Override
    public boolean click(ScreenClickContext screenClickContext, boolean z) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput mouseMoveInput, boolean z) {
        return z && !mouseMoveInput.intercepted();
    }

    @Override
    public void layout(DragRenderContext dragRenderContext) {
        if (isVisible()) {
            float fMax = 150.0f;
            float fMin = 39.0f;
            float fMax2 = 0.0f;
            int i = -1;
            for (int i2 = 0; i2 < this.entries.size(); i2++) {
                if (this.entries.get(i2).anim.smoothAnimation() > 0.001f) {
                    i = i2;
                }
            }
            int i3 = 0;
            while (i3 < this.entries.size()) {
                HotkeyEntry hotkeyEntry = this.entries.get(i3);
                float fMin2 = Math.min(hotkeyEntry.anim.smoothAnimation(), 1.0f);
                String combination = KeyboardUtil.formatCombination(hotkeyEntry.keys);
                float width = this.semiBoldFont.getWidth(hotkeyEntry.name, 12.0f);
                float width2 = 12.0f + this.boldFont.getWidth(combination, 10.0f);
                float height = hotkeyEntry.hasModule() ? 22.0f + this.semiBoldFont.getHeight(12.0f) : this.boldFont.getHeight(10.0f) + 6.0f;
                fMax = Math.max(fMax, 150.0f + ((((width + width2) + 60.0f) - 150.0f) * fMin2));
                fMin += Math.min(height, height * fMin2) + (i3 != i ? 6.0f * fMin2 : 0.0f);
                fMax2 = Math.max(fMax2, fMin2);
                i3++;
            }
            if (fMax2 > 0.0f) {
                fMin += 10.0f * fMax2;
            }
            this.width = fMax;
            this.height = fMin;
            this.headerBounds.withSize(fMax - 20.0f, 19.0f).withPosition(this.x + 10.0f, (this.y + 19.5f) - 9.5f);
        }
    }

    @Override
    public void render(DragRenderContext dragRenderContext) {
        if (!isVisible() || this.opacityAnimation.isZero()) {
            return;
        }
        DrawEngine drawEngine154 = dragRenderContext.drawEngine();
        MatrixStack matrixStack = dragRenderContext.matrixStack();
        ColorStack colorStack115 = drawEngine154.colorStack();
        ThemePalette themePalette = dragRenderContext.theme().palette();
        float fAnimatedValue = this.opacityAnimation.animatedValue();
        colorStack115.push();
        colorStack115.alpha(fAnimatedValue);
        int iComputeColor = colorStack115.computeColor(themePalette.surfaceBackground().tone(801).argb());
        int iComputeColor2 = colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb());
        drawEngine154.roundedRectangle(matrixStack.peek().getPositionMatrix(), this.x, this.y, this.width, this.height, 8.0f, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(600).argb()), iComputeColor2, iComputeColor2, iComputeColor, iComputeColor);
        drawEngine154.texture(matrixStack.peek().getPositionMatrix(), this.x, this.y, this.width, this.height, drawEngine154.bindTexture(this.starsTexture.textureWithSTB()), colorStack115.white());
        drawEngine154.msdfFont(matrixStack.peek().getPositionMatrix(), this.semiBoldFont, "Hotkeys", this.headerBounds.x(), (this.headerBounds.y() + 9.5f) - (this.semiBoldFont.getHeight(13.0f) / 2.0f), 13.0f, 0.0f, colorStack115.computeColor(themePalette.text().tone(200).argb()));
        drawEngine154.roundedRectangle(matrixStack.peek().getPositionMatrix(), this.headerBounds.right() - 19.0f, this.headerBounds.y(), 19.0f, 19.0f, 6.0f, colorStack115.computeColor(themePalette.accentBright().argb(), 0.1f));
        drawEngine154.textureVerticalCHorizontalC(matrixStack.peek().getPositionMatrix(), this.buttonsIcon, this.headerBounds.right() - 9.5f, this.headerBounds.y() + 9.5f, 11, 11, colorStack115.computeColor(themePalette.accent().argb()));
        float fBottom = this.headerBounds.bottom() + 12.0f;
        for (HotkeyEntry hotkeyEntry : this.entries) {
            String combination = KeyboardUtil.formatCombination(hotkeyEntry.keys);
            float fMin = Math.min(hotkeyEntry.anim.smoothAnimation(), 1.0f);
            float width = this.boldFont.getWidth(combination, 10.0f);
            float height = this.boldFont.getHeight(10.0f) + 6.0f;
            float f = 12.0f + width;
            float f2 = this.x + (10.0f * fAnimatedValue * fMin);
            float height2 = hotkeyEntry.hasModule() ? 22.0f + this.semiBoldFont.getHeight(12.0f) : height;
            float fRight = this.headerBounds.right() - f;
            float f3 = hotkeyEntry.hasModule() ? (fBottom + (height2 / 2.0f)) - (height / 2.0f) : fBottom;
            float height3 = hotkeyEntry.hasModule() ? fBottom + 17.0f : (fBottom + (height / 2.0f)) - (this.semiBoldFont.getHeight(12.0f) / 2.0f);
            colorStack115.push();
            colorStack115.alpha(fMin);
            if (hotkeyEntry.hasModule() && hotkeyEntry.moduleName != null) {
                float height4 = this.boldFont.getHeight(8.0f) + 4.0f;
                drawEngine154.roundedRectangle(matrixStack.peek().getPositionMatrix(), f2, fBottom, 20.0f + this.boldFont.getWidth(hotkeyEntry.moduleName.toUpperCase(), 8.0f), height4, 4.0f, colorStack115.computeColor(themePalette.accent().argb(), 0.1f));
                drawEngine154.textureVerticalC(matrixStack.peek().getPositionMatrix(), this.frameIcon, f2 + 4.0f, fBottom + (height4 / 2.0f), 9, 9, colorStack115.computeColor(themePalette.accent().argb()));
                drawEngine154.msdfFont(matrixStack.peek().getPositionMatrix(), this.boldFont, hotkeyEntry.moduleName.toUpperCase(), f2 + 16.0f, (fBottom + (height4 / 2.0f)) - (this.boldFont.getHeight(8.0f) / 2.0f), 8.0f, 0.05f, colorStack115.computeColor(themePalette.accent().argb()));
            }
            drawEngine154.msdfFont(matrixStack.peek().getPositionMatrix(), this.semiBoldFont, hotkeyEntry.name, f2, height3, 12.0f, 0.05f, colorStack115.computeColor(themePalette.text().tone(400).argb()));
            drawEngine154.roundedRectangle(matrixStack.peek().getPositionMatrix(), fRight, f3, f, height, 6.0f, colorStack115.computeColor(1974050, 0.5f));
            drawEngine154.msdfFont(matrixStack.peek().getPositionMatrix(), this.boldFont, combination, (fRight + (f / 2.0f)) - (width / 2.0f), (f3 + (height / 2.0f)) - (this.boldFont.getHeight(10.0f) / 2.0f), 10.0f, 0.05f, colorStack115.computeColor(themePalette.text().tone(500).argb()));
            colorStack115.pop();
            fBottom += (height2 + 6.0f) * fMin;
        }
        colorStack115.pop();
    }

    @Override
    public void animate(WeightedEngine weightedEngine) {
        if (Mc.INSTANCE.getPlayer() == null) {
            return;
        }
        this.entries.forEach(hotkeyEntry -> {
            hotkeyEntry.anim.animate(weightedEngine);
        });
        this.opacityAnimation.animate(weightedEngine);
    }

    @Override
    public void update() {
        if (isVisible()) {
            HashSet hashSet = new HashSet();
            boolean zMethod007 = false;
            for (ClickGuiModule module : FluxVisuals.INSTANCE.moduleRepository().getModules()) {
                HotkeyCategory class641VarMethod002 = HotkeyCategory.fromTab(module.getModuleTab());
                if (class641VarMethod002 == null || !this.hiddenCategoriesSetting.isSelected(class641VarMethod002)) {
                    if (!module.getKeyBind().isEmpty()) {
                        zMethod007 |= updateEntry(hashSet, "m:" + module.getName(), () -> {
                            return new HotkeyEntry(module.getName(), module.getKeyBind());
                        }, module.getKeyBind(), module.isState());
                    }
                    if (module.isState()) {
                        for (Setting setting : module.getSettings()) {
                            if (setting instanceof BooleanSetting) {
                                BooleanSetting booleanSetting = (BooleanSetting) setting;
                                if (booleanSetting.getKey() != -1) {
                                    zMethod007 |= updateEntry(hashSet, "s:" + module.getName() + ":" + booleanSetting.getName().effective(), () -> {
                                        return new HotkeyEntry(module, booleanSetting.getName().effective(), booleanSetting.getKeyBind());
                                    }, booleanSetting.getKeyBind(), booleanSetting.isValue());
                                }
                            }
                            if (setting instanceof ExpandableSetting) {
                                ExpandableSetting expandableSetting = (ExpandableSetting) setting;
                                String strEffective = expandableSetting.getName().effective();
                                if (expandableSetting.getKey() != -1) {
                                    zMethod007 |= updateEntry(hashSet, "s:" + module.getName() + ":" + strEffective, () -> {
                                        return new HotkeyEntry(module, strEffective, expandableSetting.getKeyBind());
                                    }, expandableSetting.getKeyBind(), expandableSetting.isValue());
                                }
                                for (Setting class661Var2 : expandableSetting.getSubSettings()) {
                                    if (class661Var2 instanceof BooleanSetting) {
                                        BooleanSetting class665Var2 = (BooleanSetting) class661Var2;
                                        if (class665Var2.getKey() != -1) {
                                            zMethod007 |= updateEntry(hashSet, "s:" + module.getName() + ":" + strEffective + "/" + class665Var2.getName().effective(), () -> {
                                                return new HotkeyEntry(module, strEffective + "/" + class665Var2.getName().effective(), class665Var2.getKeyBind());
                                            }, class665Var2.getKeyBind(), class665Var2.isValue());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            this.entryMap.keySet().retainAll(hashSet);
            this.entries.clear();
            Stream<HotkeyEntry> streamSorted = this.entryMap.values().stream().sorted(Comparator.comparing(hotkeyEntry -> {
                return hotkeyEntry.name;
            }, String.CASE_INSENSITIVE_ORDER));
            List<HotkeyEntry> list = this.entries;
            Objects.requireNonNull(list);
            streamSorted.forEach((v1) -> {
                list.add(v1);
            });
            this.opacityAnimation.destination(((this.entries.isEmpty() || !zMethod007) && !(Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen)) ? 0.0f : 1.0f);
        }
    }

    public boolean updateEntry(Set<String> set, String str, Supplier<HotkeyEntry> supplier, List<Integer> list, boolean z) {
        set.add(str);
        HotkeyEntry hotkeyEntry = this.entryMap.computeIfAbsent(str, str2 -> {
            return (HotkeyEntry) supplier.get();
        });
        hotkeyEntry.keys.clear();
        if (list != null) {
            hotkeyEntry.keys.addAll(list);
        }
        hotkeyEntry.anim.state(z);
        return z;
    }
}
