package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.ButtonSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.setting.ColorSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ColorStack;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.setting.ExpandableSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.model.FrameSearchTarget;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.KeyInput;
import ru.fluxvisuals.ui.gui.clickgui.setting.KeybindSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.type.MatchMode;
import ru.fluxvisuals.ui.gui.clickgui.util.MenuSearchNavigator;
import ru.fluxvisuals.ui.gui.clickgui.setting.ModeSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.type.ModuleCategory;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.setting.MultiSelectSetting;
import ru.fluxvisuals.ui.gui.clickgui.setting.NumberSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PixelPoint;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.SearchMatch;
import ru.fluxvisuals.ui.gui.clickgui.util.model.SearchNavTarget;
import ru.fluxvisuals.ui.gui.clickgui.util.SearchNavigator;
import ru.fluxvisuals.ui.gui.clickgui.setting.Setting;
import ru.fluxvisuals.ui.gui.clickgui.util.model.SettingSearchTarget;
import ru.fluxvisuals.ui.gui.clickgui.util.TabsController;
import ru.fluxvisuals.ui.gui.clickgui.setting.TextFieldSetting;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ThemePalette;
import ru.fluxvisuals.ui.gui.clickgui.util.render.ToggleAnimator;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.WidgetBounds;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class SearchOverlay extends WidgetContainer {
    public static final float smallGap = 2.0f;
    public static final float rowHeight = 28.0f;
    public static final float horizontalPadding = 10.0f;
    public static final float fontSize = 13.0f;
    public static final float groupSpacing = 12.0f;
    public static final float maxHeight = 450.0f;
    public static final float cornerRadius = 8.0f;
    public static final float epsilon = 0.01f;
    public static final int iconSize = 13;
    boolean opened;

    final ScrollbarWidget scrollbar;
    public final GlTexture searchIcon = new GlTexture(new ClasspathResource("/icons/menu/new/search.png")).setDimensions(iconSize, iconSize);
    public final GlTexture frameIcon = new GlTexture(new ClasspathResource("/icons/menu/new/frame.png")).setDimensions(12, 12);
    public final GlTexture multiEnumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/multienum.png")).setDimensions(12, 12);
    public final GlTexture sliderIcon = new GlTexture(new ClasspathResource("/icons/menu/new/slider.png")).setDimensions(12, 12);
    public final GlTexture enumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png")).setDimensions(12, 12);
    public final GlTexture arrowIcon = new GlTexture(new ClasspathResource("/icons/menu/new/arrow_horizontal.png")).setDimensions(9, 8);
    public final GlTexture gamepadIcon = new GlTexture(new ClasspathResource("/icons/menu/new/gamepad.png")).setDimensions(12, 12);
    public final GlTexture textIcon = new GlTexture(new ClasspathResource("/icons/menu/new/text.png")).setDimensions(12, 12);
    public final GlTexture groupIcon = new GlTexture(new ClasspathResource("/icons/menu/new/group.png")).setDimensions(12, 12);
    public final GlTexture brushIcon = new GlTexture(new ClasspathResource("/icons/menu/new/brush.png")).setDimensions(12, 12);
    public final GlTexture clickIcon = new GlTexture(new ClasspathResource("/icons/menu/new/click.png")).setDimensions(12, 12);
    public boolean keyboardNavigation = false;
    public float lastMouseX = Float.NaN;
    public float lastMouseY = Float.NaN;
    public final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont boldFont = Fonts.INTER_EXTRA_BOLD.get();
    final TextInputField searchField = new TextInputField(this.font, iconSize);
    final ToggleAnimator openAnimator = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    final WidgetBounds searchBoxBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    final WidgetBounds resultsBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    final List<SearchMatch> matches = new ArrayList();
    final List<SearchResultRow> resultRows = new ArrayList();
    public final SearchNavigator navigator = new MenuSearchNavigator();
    final ScrollArea scrollArea = new ScrollArea();
    public int selectedIndex = -1;
    public final ToggleAnimator selectionAnimator = new ToggleAnimator(150, Easings.EASE_IN_OUT_CUBIC);
    public final List<ClickableBehavior> clickBehaviors = new ArrayList();

    public SearchOverlay() {
        ScrollArea scrollArea789 = this.scrollArea;
        Objects.requireNonNull(scrollArea789);
        Supplier supplier = scrollArea789::scrollY;
        Supplier supplier2 = this::getContentHeight;
        WidgetBounds widgetBounds = this.resultsBounds;
        Objects.requireNonNull(widgetBounds);
        Supplier supplier3 = widgetBounds::height;
        ScrollArea class789Var2 = this.scrollArea;
        Objects.requireNonNull(class789Var2);
        this.scrollbar = new ScrollbarWidget(supplier, supplier2, supplier3, (v1) -> {
            scrollArea789.scrollTo(v1);
        }, cornerRadius, 4.0f, 30.0f);
        addChild(this.scrollArea);
        addChild(this.scrollbar);
        this.searchField.changeText(str -> {
            this.matches.clear();
            String strReplaceAll = str.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
            boolean zIsBlank = strReplaceAll.isBlank();
            TabsController tabsController733 = FluxVisuals.INSTANCE.tabsController();
            if (!zIsBlank) {
                for (MenuTabElement menuTabElement : tabsController733.tabElements()) {
                    for (AbstractFrame abstractFrame : menuTabElement.frames()) {
                        if (abstractFrame instanceof ModuleCard) {
                            ModuleCard moduleCard = (ModuleCard) abstractFrame;
                            String strReplaceAll2 = moduleCard.name().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
                            MatchMode matchMode = null;
                            if (strReplaceAll2.equals(strReplaceAll)) {
                                matchMode = MatchMode.EXACT;
                            } else if (strReplaceAll2.startsWith(strReplaceAll)) {
                                matchMode = MatchMode.STARTS_WITH;
                            } else if (strReplaceAll2.contains(strReplaceAll)) {
                                matchMode = MatchMode.CONTAINS;
                            }
                            if (matchMode == null && Arrays.stream(moduleCard.aliases()).anyMatch(alias -> {
                                return alias.replaceAll("\\s+", "").toLowerCase(Locale.ROOT).contains(strReplaceAll);
                            })) {
                                matchMode = MatchMode.ALIAS;
                            }
                            if (matchMode != null) {
                                this.matches.add(new SearchMatch(new FrameSearchTarget(moduleCard, menuTabElement), matchMode));
                            }
                            for (Setting setting : moduleCard.settings()) {
                                if (setting != null) {
                                    MatchMode class793VarMethod012 = matchText(setting.getName() != null ? setting.getName().effective() : "", setting.getDescription() != null ? setting.getDescription().effective() : "", strReplaceAll);
                                    if (class793VarMethod012 != null) {
                                        this.matches.add(new SearchMatch(new SettingSearchTarget(menuTabElement, moduleCard, setting), class793VarMethod012));
                                    }
                                }
                            }
                        }
                    }
                }
                Collections.sort(this.matches);
            }
            rebuildResultRows();
            this.scrollArea.scrollTo(0.0f);
            this.selectedIndex = this.resultRows.isEmpty() ? -1 : 0;
        });
    }

    @Override
    public void render(DrawCtx drawCtx) throws MatchException {
        if (this.openAnimator.isZero()) {
            return;
        }
        ThemePalette themePalette = drawCtx.theme().palette();
        ColorStack colorStack115 = drawCtx.drawEngine().colorStack();
        if (this.searchField.focused()) {
            drawCtx.window().interceptKeyboard(true);
        }
        colorStack115.push();
        colorStack115.alpha(this.openAnimator.smoothAnimation());
        drawCtx.roundedBlur(drawCtx.getColorAttachmentId(FluxVisuals.INSTANCE.windowController().searchBlur().getBlurFramebuffer()), x(), y(), width(), height(), cornerRadius, drawCtx.colorStack().white());
        drawCtx.fillRoundedRect(x() - 1.0f, y(), width() + smallGap, height() + 1.0f, cornerRadius, colorStack115.computeColor(themePalette.surfaceBackground().tone(900).argb(), 0.9f));
        drawCtx.fillOutlinedRoundedRect(this.searchBoxBounds.x(), this.searchBoxBounds.y(), this.searchBoxBounds.width(), this.searchBoxBounds.height() + Math.min(computeContentHeight(), maxHeight), horizontalPadding, 2.5f, colorStack115.computeColor(themePalette.surfaceOutline().tone(400).argb()), colorStack115.computeColor(themePalette.surfaceBackground().tone(600).argb()));
        renderSearchBox(drawCtx, themePalette, colorStack115);
        if (!this.resultRows.isEmpty()) {
            renderResults(drawCtx, themePalette, colorStack115);
        }
        super.render(drawCtx);
        colorStack115.pop();
    }

    @Nullable
    public static MatchMode matchText(String str, String str2, String str3) {
        String lowerCase = str.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        String lowerCase2 = str2.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        MatchMode matchMode = null;
        if (lowerCase.equals(str3)) {
            matchMode = MatchMode.EXACT;
        } else if (lowerCase.startsWith(str3)) {
            matchMode = MatchMode.STARTS_WITH;
        } else if (lowerCase.contains(str3) || lowerCase2.contains(str3)) {
            matchMode = MatchMode.CONTAINS;
        }
        return matchMode;
    }

    public void renderSearchBox(DrawCtx drawCtx, ThemePalette themePalette, ColorStack colorStack) {
        float fX = this.searchBoxBounds.x() + horizontalPadding;
        int iComputeColor = colorStack.computeColor(themePalette.text().tone(600).argb());
        String strText = this.searchField.text();
        drawCtx.textureVerticalC(this.searchIcon, fX, this.searchBoxBounds.y() + (this.searchBoxBounds.height() / smallGap), this.searchIcon.width(), this.searchIcon.height(), iComputeColor);
        float fWidth = ((fX + this.searchIcon.width()) + cornerRadius) - this.searchField.viewportOffset();
        float height = this.font.getHeight(fontSize);
        float fY = (this.searchBoxBounds.y() + (this.searchBoxBounds.height() / smallGap)) - Math.round(height / smallGap);
        drawCtx.drawEngine().beginStencil();
        drawCtx.fillRect(fX + this.searchIcon.width() + 6.0f, this.searchBoxBounds.y(), this.searchBoxBounds.width() - 40.0f, this.searchBoxBounds.height(), colorStack.white());
        drawCtx.drawEngine().prepareStencil(1);
        if (this.searchField.hasSelection()) {
            int iSelMin = this.searchField.selMin();
            int iSelMax = this.searchField.selMax();
            float fTextWidthPhysical = fWidth + drawCtx.textWidthPhysical(this.font, strText.substring(0, iSelMin), iconSize);
            drawCtx.fillRoundedRect(fTextWidthPhysical, fY - 1.0f, ((fWidth + drawCtx.textWidthPhysical(this.font, strText.substring(0, iSelMax), iconSize)) + 1.0f) - fTextWidthPhysical, height + smallGap, 4.0f, colorStack.computeColor(themePalette.accent().argb(), 128));
        }
        if (!strText.isEmpty() && this.searchField.focused()) {
            iComputeColor = colorStack.computeColor(themePalette.text().tone(200).argb());
        }
        drawCtx.text(this.font, (!strText.isEmpty() || this.searchField.focused()) ? strText : Lang.SEARCH.effective(), iconSize, fWidth, fY, iComputeColor);
        if (this.searchField.isCursorVisible() && !this.searchField.hasSelection()) {
            drawCtx.fillRect(fWidth + drawCtx.textWidthPhysical(this.font, strText.substring(0, this.searchField.cursorIndex()), iconSize), fY, 1.0f, height, colorStack.computeColor(16777215));
        }
        drawCtx.drawEngine().endStencil();
    }

    public void renderResults(DrawCtx drawCtx, ThemePalette themePalette, ColorStack colorStack) throws MatchException {
        float fMethod019 = getContentHeight();
        this.scrollArea.beginArea(drawCtx, this.resultsBounds.x(), this.resultsBounds.y(), this.resultsBounds.width(), this.resultsBounds.height());
        float fY = this.resultsBounds.y() + smallGap;
        float fX = this.resultsBounds.x() + horizontalPadding;
        float fWidth = (this.resultsBounds.width() - horizontalPadding) - 25.0f;
        boolean z = true;
        int i = 0;
        for (Map.Entry<String, List<SearchResultRow>> entry : ((Map<String, List<SearchResultRow>>) this.resultRows.stream().collect(Collectors.groupingBy(searchResultRow -> {
            return searchResultRow.qualifier().effective();
        }))).entrySet()) {
            String str = (String) entry.getKey();
            List<SearchResultRow> list = entry.getValue();
            if (!z) {
                fY += groupSpacing;
            }
            z = false;
            drawCtx.text(this.font, str, 11, fX, fY, colorStack.computeColor(themePalette.text().tone(600).argb()));
            float f = fY + 21.0f;
            for (SearchResultRow class792Var2 : list) {
                if (i == this.selectedIndex) {
                    drawCtx.fillOutlinedRoundedRect(fX, f, fWidth, rowHeight, 6.0f, 2.5f, colorStack.computeColor(themePalette.surfaceOutline().tone(300).argb()), colorStack.computeColor(themePalette.surfaceBackground().tone(400).argb()));
                }
                if (i < this.clickBehaviors.size()) {
                    float fSmoothAnimation = this.clickBehaviors.get(i).hoverAnimation().smoothAnimation();
                    if (fSmoothAnimation > epsilon) {
                        drawCtx.fillRoundedRect(fX - smallGap, f, fWidth + 4.0f, rowHeight, 6.0f, colorStack.computeColor(themePalette.accent().argb(), 0.08f * fSmoothAnimation));
                    }
                }
                SearchTypeBadge searchTypeBadge = class792Var2.typeLabel();
                float f2 = fX + cornerRadius;
                drawCtx.textureVerticalC(searchTypeBadge.texture(), f2, f + 14.0f, searchTypeBadge.texture().width(), searchTypeBadge.texture().height(), colorStack.computeColor(themePalette.text().tone(500).argb()));
                String strMethod010 = getResultName(class792Var2);
                float fWidth2 = f2 + searchTypeBadge.texture().width() + cornerRadius;
                drawCtx.text(this.font, strMethod010, iconSize, fWidth2, (f + 14.0f) - (this.font.getHeight(fontSize) / smallGap), colorStack.computeColor(themePalette.text().tone(200).argb()));
                if (searchTypeBadge.text() != null) {
                    int iComputeColor = colorStack.computeColor(themePalette.accent().argb());
                    float fTextWidthPhysical = drawCtx.textWidthPhysical(this.boldFont, searchTypeBadge.text(), 9);
                    float height = this.boldFont.getHeight(9.0f);
                    float f3 = fTextWidthPhysical + groupSpacing;
                    float fTextWidthPhysical2 = fWidth2 + drawCtx.textWidthPhysical(this.font, strMethod010, iconSize) + 6.0f;
                    float f4 = ((f + 14.0f) - (15.0f / smallGap)) + 1.0f;
                    drawCtx.fillRoundedRect(fTextWidthPhysical2, f4, f3, 15.0f, 5.0f, colorStack.computeColor(themePalette.accent().argb(), 0.1f));
                    drawCtx.text(this.boldFont, searchTypeBadge.text(), 9, (fTextWidthPhysical2 + (f3 / smallGap)) - (fTextWidthPhysical / smallGap), (f4 + (15.0f / smallGap)) - (height / smallGap), iComputeColor);
                }
                renderResultPath(drawCtx, themePalette, colorStack, class792Var2, ((fX + fWidth) - measureResultPath(drawCtx, class792Var2)) - cornerRadius, f + 14.0f);
                f += 30.0f;
                i++;
            }
            fY = f - smallGap;
        }
        this.scrollArea.endArea(drawCtx, fMethod019);
    }

    public String getResultName(SearchResultRow searchResultRow) {
        SearchNavTarget searchNavTarget = searchResultRow.searchResult().searchTarget();
        if (searchNavTarget instanceof SettingSearchTarget) {
            return ((SettingSearchTarget) searchNavTarget).setting().getName().effective();
        }
        SearchNavTarget class844VarSearchTarget2 = searchResultRow.searchResult().searchTarget();
        if (!(class844VarSearchTarget2 instanceof FrameSearchTarget)) {
            return "";
        }
        AbstractFrame abstractFrame = ((FrameSearchTarget) class844VarSearchTarget2).frameContainer();
        return abstractFrame instanceof ModuleCard ? ((ModuleCard) abstractFrame).name() : "";
    }

    public void renderResultPath(DrawCtx drawCtx, ThemePalette themePalette, ColorStack colorStack, SearchResultRow searchResultRow, float f, float f2) throws MatchException {
        SearchNavTarget searchNavTarget = searchResultRow.searchResult().searchTarget();
        int iComputeColor = colorStack.computeColor(themePalette.text().tone(500).argb());
        int iComputeColor2 = colorStack.computeColor(themePalette.text().tone(500).argb());
        if (searchNavTarget instanceof SettingSearchTarget) {
            SettingSearchTarget settingSearchTarget = (SettingSearchTarget) searchNavTarget;
            GlTexture glTexture = settingSearchTarget.tab().icon();
            glTexture.setDimensions(11, 11);
            drawCtx.textureVerticalC(glTexture, f, f2, glTexture.width(), glTexture.height(), iComputeColor2);
            float fWidth = f + glTexture.width() + 3.0f;
            String strTitle = settingSearchTarget.tab().title();
            drawCtx.text(this.font, strTitle, 11, fWidth, f2 - (this.font.getHeight(11.0f) / smallGap), iComputeColor);
            float fTextWidthPhysical = fWidth + drawCtx.textWidthPhysical(this.font, strTitle, 11) + smallGap;
            drawCtx.textureVerticalC(this.arrowIcon, fTextWidthPhysical, f2 + 1.0f, this.arrowIcon.width(), this.arrowIcon.height(), iComputeColor2);
            float fWidth2 = fTextWidthPhysical + this.arrowIcon.width() + smallGap;
            drawCtx.textureVerticalC(this.frameIcon, fWidth2, f2, 11, 11, iComputeColor2);
            float f3 = fWidth2 + 14.0f;
            AbstractFrame abstractFrame = settingSearchTarget.frame();
            if (abstractFrame instanceof ModuleCard) {
                drawCtx.text(this.font, ((ModuleCard) abstractFrame).name(), 11, f3, f2 - (this.font.getHeight(11.0f) / smallGap), iComputeColor);
                return;
            }
            return;
        }
        if (searchNavTarget instanceof FrameSearchTarget) {
            FrameSearchTarget frameSearchTarget = (FrameSearchTarget) searchNavTarget;
            try {
                AbstractFrame abstractFrame757 = frameSearchTarget.frameContainer();
                MenuTabElement menuTabElement = frameSearchTarget.tab();
                GlTexture class073VarIcon2 = menuTabElement.icon();
                class073VarIcon2.setDimensions(11, 11);
                drawCtx.textureVerticalC(class073VarIcon2, f, f2, class073VarIcon2.width(), class073VarIcon2.height(), iComputeColor2);
                float fWidth3 = f + class073VarIcon2.width() + 3.0f;
                String strTitle2 = menuTabElement.title();
                drawCtx.text(this.font, strTitle2, 11, fWidth3, f2 - (this.font.getHeight(11.0f) / smallGap), iComputeColor);
                float fTextWidthPhysical2 = fWidth3 + drawCtx.textWidthPhysical(this.font, strTitle2, 11) + smallGap;
                drawCtx.textureVerticalC(this.arrowIcon, fTextWidthPhysical2, f2 + 1.0f, this.arrowIcon.width(), this.arrowIcon.height(), iComputeColor2);
                float fWidth4 = fTextWidthPhysical2 + this.arrowIcon.width() + smallGap;
                if (abstractFrame757 instanceof ModuleCard) {
                    GlTexture class073VarIcon3 = this.frameIcon;
                    class073VarIcon3.setDimensions(11, 11);
                    drawCtx.textureVerticalC(class073VarIcon3, fWidth4, f2, 11, 11, iComputeColor2);
                    ru.fluxvisuals.module.api.Category category = ((ModuleCard) abstractFrame757).module().category;
                    drawCtx.text(this.font, category != null ? category.getName() : "Без категории", 11, fWidth4 + 14.0f, f2 - (this.font.getHeight(11.0f) / smallGap), iComputeColor);
                }
            } catch (Throwable th) {
                throw new MatchException(th.toString(), th);
            }
        }
    }

    public float measureResultPath(DrawCtx drawCtx, SearchResultRow searchResultRow) throws MatchException {
        SearchNavTarget searchNavTarget = searchResultRow.searchResult().searchTarget();
        float fTextWidthPhysical = 0.0f;
        if (searchNavTarget instanceof SettingSearchTarget) {
            SettingSearchTarget settingSearchTarget = (SettingSearchTarget) searchNavTarget;
            fTextWidthPhysical = 0.0f + 14.0f + drawCtx.textWidthPhysical(this.font, settingSearchTarget.tab().title(), 11) + smallGap + this.arrowIcon.width() + smallGap + 14.0f;
            AbstractFrame abstractFrame = settingSearchTarget.frame();
            if (abstractFrame instanceof ModuleCard) {
                fTextWidthPhysical += drawCtx.textWidthPhysical(this.font, ((ModuleCard) abstractFrame).name(), 11);
            }
        } else if (searchNavTarget instanceof FrameSearchTarget) {
            FrameSearchTarget frameSearchTarget = (FrameSearchTarget) searchNavTarget;
            try {
                AbstractFrame abstractFrame757 = frameSearchTarget.frameContainer();
                fTextWidthPhysical = 0.0f + 14.0f + drawCtx.textWidthPhysical(this.font, frameSearchTarget.tab().title(), 11) + smallGap + this.arrowIcon.width() + smallGap;
                if (abstractFrame757 instanceof ModuleCard) {
                    ru.fluxvisuals.module.api.Category category = ((ModuleCard) abstractFrame757).module().category;
                    fTextWidthPhysical = fTextWidthPhysical + 14.0f + drawCtx.textWidthPhysical(this.font, category != null ? category.getName() : "Без категории", 11);
                }
            } catch (Throwable th) {
                throw new MatchException(th.toString(), th);
            }
        }
        return fTextWidthPhysical;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        if (this.openAnimator.isZero()) {
            return;
        }
        this.searchBoxBounds.withPosition((x() + (width() / smallGap)) - 300.0f, y() + 50.0f).withSize(600.0f, 36.0f);
        this.resultsBounds.withPosition(this.searchBoxBounds.x(), this.searchBoxBounds.y() + this.searchBoxBounds.height()).withSize(this.searchBoxBounds.width(), Math.min(computeContentHeight(), maxHeight));
        this.scrollbar.setSize(4.0f, this.resultsBounds.height());
        this.scrollbar.setPosition(((this.searchBoxBounds.x() + this.searchBoxBounds.width()) - horizontalPadding) - this.scrollbar.width(), this.resultsBounds.y());
        this.searchIcon.setDimensions(iconSize, iconSize);
        this.frameIcon.setDimensions(12, 12);
        this.multiEnumIcon.setDimensions(12, 12);
        this.sliderIcon.setDimensions(12, 12);
        this.enumIcon.setDimensions(12, 12);
        this.gamepadIcon.setDimensions(12, 12);
        this.groupIcon.setDimensions(12, 12);
        this.arrowIcon.setDimensions(9, 8);
        this.clickIcon.setDimensions(12, 12);
        this.brushIcon.setDimensions(12, 12);
        this.textIcon.setDimensions(12, 12);
        this.searchField.listen(this.searchBoxBounds.x(), this.searchBoxBounds.y(), this.searchBoxBounds.width(), this.searchBoxBounds.height(), this.searchBoxBounds.width() - 47.0f, this.searchBoxBounds.x() + horizontalPadding + this.searchIcon.width() + cornerRadius, layoutScaleContext.scaleFactor());
        rebuildClickBehaviors();
        super.layout(layoutScaleContext);
    }

    public void rebuildClickBehaviors() {
        this.clickBehaviors.clear();
        float fY = this.resultsBounds.y() + smallGap;
        float fX = this.resultsBounds.x() + horizontalPadding;
        float fWidth = (this.resultsBounds.width() - horizontalPadding) - 25.0f;
        boolean z = true;
        int i = 0;
        Iterator<Map.Entry<String, List<SearchResultRow>>> it = ((Map<String, List<SearchResultRow>>) this.resultRows.stream().collect(Collectors.groupingBy(searchResultRow -> {
            return searchResultRow.qualifier().effective();
        }))).entrySet().iterator();
        while (it.hasNext()) {
            List<SearchResultRow> list = (List) ((Map.Entry) it.next()).getValue();
            if (!z) {
                fY += groupSpacing;
            }
            z = false;
            float f = fY + 21.0f;
            for (SearchResultRow class792Var2 : list) {
                ClickableBehavior clickableBehavior = new ClickableBehavior();
                int i2 = i;
                clickableBehavior.clickCallback(() -> {
                    selectResult(i2);
                });
                clickableBehavior.setDimensions(fX, f, fWidth, rowHeight);
                this.clickBehaviors.add(clickableBehavior);
                f += 30.0f;
                i++;
            }
            fY = f - smallGap;
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.openAnimator.animate(weightedEngine);
        this.selectionAnimator.animate(weightedEngine);
        this.searchField.animate(weightedEngine);
        Iterator<ClickableBehavior> it = this.clickBehaviors.iterator();
        while (it.hasNext()) {
            it.next().animate(weightedEngine);
        }
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) throws MatchException {
        InputEventContext inputEventContext688 = inputEventContext.withMouseOffset(0.0f, this.scrollArea.scrollY());
        if (this.openAnimator.isZero() && !this.opened) {
            return false;
        }
        if (super.handleInput(inputEventContext, z)) {
            return true;
        }
        if ((inputEventContext.inputEvent() instanceof ScrollInput) && !z) {
            return true;
        }
        if (!z) {
            InputEvent inputEvent691 = inputEventContext688.inputEvent();
            if (inputEvent691 instanceof KeyInput) {
                KeyInput keyInput = (KeyInput) inputEvent691;
                if (keyInput.keyAction().press() || keyInput.keyAction().repeat()) {
                    switch (keyInput.keyCode()) {
                        case 256:
                            close();
                            return true;
                        case 257:
                            if (this.selectedIndex >= 0 && this.selectedIndex < this.resultRows.size()) {
                                selectResult(this.selectedIndex);
                                return true;
                            }
                            break;
                        case 264:
                            if (!this.resultRows.isEmpty()) {
                                this.keyboardNavigation = true;
                                this.selectedIndex = Math.min(this.selectedIndex + 1, this.resultRows.size() - 1);
                                scrollToSelected();
                                return true;
                            }
                            break;
                        case 265:
                            if (!this.resultRows.isEmpty()) {
                                this.keyboardNavigation = true;
                                this.selectedIndex = Math.max(this.selectedIndex - 1, 0);
                                scrollToSelected();
                                return true;
                            }
                            break;
                    }
                }
            }
            InputEvent class691VarInputEvent2 = inputEventContext688.inputEvent();
            if (class691VarInputEvent2 instanceof CursorMoveInput) {
                try {
                    PixelPoint pixelPoint = ((CursorMoveInput) class691VarInputEvent2).mousePosition();
                    float fX = pixelPoint.x();
                    float fY = pixelPoint.y();
                    boolean z2 = Float.isNaN(this.lastMouseX) || Math.abs(fX - this.lastMouseX) > epsilon || Math.abs(fY - this.lastMouseY) > epsilon;
                    this.lastMouseX = fX;
                    this.lastMouseY = fY;
                    if (z2) {
                        this.keyboardNavigation = false;
                        for (int i = 0; i < this.clickBehaviors.size(); i++) {
                            ClickableBehavior clickableBehavior = this.clickBehaviors.get(i);
                            if (inputEventContext688.inArea(clickableBehavior.ownerX(), clickableBehavior.ownerY(), clickableBehavior.ownerW(), clickableBehavior.ownerH())) {
                                this.selectedIndex = i;
                            }
                        }
                    }
                } catch (Throwable th) {
                    throw new MatchException(th.toString(), th);
                }
            }
        }
        Iterator<ClickableBehavior> it = this.clickBehaviors.iterator();
        while (it.hasNext()) {
            if (it.next().handleInput(inputEventContext688, z)) {
                return true;
            }
        }
        if (this.searchField.handleInput(inputEventContext688, z)) {
            return true;
        }
        InputEvent class691VarInputEvent3 = inputEventContext688.inputEvent();
        if (!(class691VarInputEvent3 instanceof MouseButtonInput)) {
            return false;
        }
        MouseButtonInput mouseButtonInput = (MouseButtonInput) class691VarInputEvent3;
        if (!mouseButtonInput.action().press() || mouseButtonInput.button() != 0) {
            return false;
        }
        if (inputEventContext688.inArea(this.searchBoxBounds.x(), this.searchBoxBounds.y(), this.searchBoxBounds.width(), this.searchBoxBounds.height() + Math.min(computeContentHeight(), maxHeight))) {
            return true;
        }
        close();
        this.searchField.focused(false);
        return true;
    }

    public void scrollToSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.resultRows.size()) {
            return;
        }
        float fMethod014 = getRowY(this.selectedIndex);
        float fScrollY = this.scrollArea.scrollY();
        float fY = this.resultsBounds.y() + cornerRadius;
        float fY2 = (this.resultsBounds.y() + this.resultsBounds.height()) - cornerRadius;
        float f = fMethod014 - fScrollY;
        float f2 = f + rowHeight;
        if (f < fY) {
            this.scrollArea.scrollTo((fMethod014 - this.resultsBounds.y()) - cornerRadius);
        } else if (f2 > fY2) {
            this.scrollArea.scrollTo(fScrollY + (f2 - fY2));
        }
    }

    public float getRowY(int i) {
        float fY = this.resultsBounds.y() + smallGap;
        boolean z = true;
        int i2 = 0;
        for (List<SearchResultRow> list : ((Map<String, List<SearchResultRow>>) this.resultRows.stream().collect(Collectors.groupingBy(searchResultRow -> {
            return searchResultRow.qualifier().effective();
        }))).values()) {
            if (!z) {
                fY += groupSpacing;
            }
            z = false;
            float f = fY + 21.0f;
            for (int i3 = 0; i3 < list.size(); i3++) {
                if (i2 == i) {
                    return f;
                }
                f += 30.0f;
                i2++;
            }
            fY = f - smallGap;
        }
        return fY;
    }

    public void selectResult(int i) throws MatchException {
        SearchResultRow class792VarMethod006 = getRow(i);
        if (class792VarMethod006 == null) {
            return;
        }
        SearchNavTarget searchNavTarget = class792VarMethod006.searchResult().searchTarget();
        if (searchNavTarget instanceof SettingSearchTarget settingSearchTarget) {
            this.navigator.focusSetting(settingSearchTarget.tab(), settingSearchTarget.frame(), settingSearchTarget.setting());
        } else if (searchNavTarget instanceof FrameSearchTarget frameSearchTarget) {
            this.navigator.focusFrame(frameSearchTarget.frameContainer(), frameSearchTarget.tab());
        }
        FluxVisuals.INSTANCE.menuWindow().collapseExpandedHeader();
        close();
    }

    public boolean isFocused() {
        return this.searchField.focused();
    }

    public SearchResultRow getRow(int i) {
        if (i < 0) {
            return null;
        }
        int i2 = 0;
        Iterator<List<SearchResultRow>> it = ((Map<String, List<SearchResultRow>>) this.resultRows.stream().collect(Collectors.groupingBy(searchResultRow -> {
            return searchResultRow.qualifier().effective();
        }))).values().iterator();
        while (it.hasNext()) {
            for (SearchResultRow class792Var2 : it.next()) {
                if (i2 == i) {
                    return class792Var2;
                }
                i2++;
            }
        }
        return null;
    }

    public void open() {
        this.opened = true;
        visible(true);
        this.openAnimator.state(true);
        this.searchField.focused(true);
        if (this.resultRows.isEmpty()) {
            return;
        }
        this.selectedIndex = 0;
    }

    public void close() {
        this.opened = false;
        this.openAnimator.state(false);
        this.searchField.focused(false);
        this.selectedIndex = -1;
        this.searchField.clearText();
    }

    public void invert() {
        this.opened = !this.opened;
        this.openAnimator.state(this.opened);
        this.searchField.focused(this.opened);
        if (!this.opened) {
            this.searchField.clearText();
        }
        if (this.opened && !this.resultRows.isEmpty()) {
            this.selectedIndex = 0;
        } else {
            if (this.opened) {
                return;
            }
            this.selectedIndex = -1;
        }
    }

    public void rebuildResultRows() {
        this.resultRows.clear();
        for (SearchMatch searchMatch : this.matches) {
            this.resultRows.add(new SearchResultRow(searchMatch, searchMatch.searchTarget() instanceof SettingSearchTarget ? Translation.clearText("ÐÐ°ÑÑ‚Ñ€Ð¾Ð¹ÐºÐ¸") : Translation.clearText("ÐœÐ¾Ð´ÑƒÐ»Ð¸"), createTypeBadge(searchMatch)));
        }
    }

    public SearchTypeBadge createTypeBadge(SearchMatch searchMatch) {
        SearchNavTarget searchNavTarget = searchMatch.searchTarget();
        if (!(searchNavTarget instanceof SettingSearchTarget)) {
            return new SearchTypeBadge(null, this.frameIcon);
        }
        Setting setting661 = ((SettingSearchTarget) searchNavTarget).setting();
        Objects.requireNonNull(setting661);
        switch (typeSwitch02448(setting661)) {
            case 0:
                return new SearchTypeBadge("CHECKBOX", this.multiEnumIcon);
            case 1:
                return new SearchTypeBadge("SLIDER", this.sliderIcon);
            case 2:
                return new SearchTypeBadge("GROUP", this.groupIcon);
            case 3:
                return new SearchTypeBadge("KEYBIND", this.gamepadIcon);
            case 4:
                return new SearchTypeBadge("MULTI COMBOBOX", this.enumIcon);
            case 5:
                return new SearchTypeBadge("TEXTFIELD", this.textIcon);
            case 6:
                return new SearchTypeBadge("COMBOBOX", this.enumIcon);
            case 7:
                return new SearchTypeBadge("BUTTON", this.clickIcon);
            case 8:
                return new SearchTypeBadge("COLOR", this.brushIcon);
            default:
                return new SearchTypeBadge(null, this.frameIcon);
        }
    }

    static int typeSwitch02448(Object o) {
        if (o == null) return -1;
        if (o instanceof BooleanSetting) return 0;
        if (o instanceof NumberSetting) return 1;
        if (o instanceof ExpandableSetting) return 2;
        if (o instanceof KeybindSetting) return 3;
        if (o instanceof MultiSelectSetting) return 4;
        if (o instanceof TextFieldSetting) return 5;
        if (o instanceof ModeSetting) return 6;
        if (o instanceof ButtonSetting) return 7;
        if (o instanceof ColorSetting) return 8;
        return 9;
    }

    public float computeContentHeight() {
        if (this.resultRows.isEmpty()) {
            return 0.0f;
        }
        float fMax = 2.0f;
        boolean z = true;
        for (List<SearchResultRow> list : ((Map<String, List<SearchResultRow>>) this.resultRows.stream().collect(Collectors.groupingBy(searchResultRow -> {
            return searchResultRow.qualifier().effective();
        }))).values()) {
            if (!z) {
                fMax += groupSpacing;
            }
            z = false;
            int size = list.size();
            fMax = fMax + 21.0f + (size * rowHeight) + (Math.max(0, size - 1) * smallGap);
        }
        return fMax + horizontalPadding;
    }

    public float getContentHeight() {
        return computeContentHeight();
    }

    public boolean isOpened() {
        return this.opened;
    }
}
