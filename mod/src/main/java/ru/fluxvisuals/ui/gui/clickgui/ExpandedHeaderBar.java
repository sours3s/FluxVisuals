package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.resource.ClasspathResource;
import ru.fluxvisuals.ui.gui.clickgui.util.model.CursorMoveInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.DrawCtx;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.type.DropdownOption;
import ru.fluxvisuals.ui.gui.clickgui.util.render.Fonts;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;
import ru.fluxvisuals.ui.gui.clickgui.util.type.GuiScale;
import ru.fluxvisuals.ui.gui.clickgui.events.impl.InputEvent;
import ru.fluxvisuals.ui.gui.clickgui.util.model.InputEventContext;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Language;
import ru.fluxvisuals.ui.gui.clickgui.util.model.LayoutScaleContext;
import ru.fluxvisuals.ui.gui.clickgui.util.model.MouseButtonInput;
import ru.fluxvisuals.ui.gui.clickgui.util.render.MsdfFont;
import ru.fluxvisuals.ui.gui.clickgui.util.model.ScrollInput;
import ru.fluxvisuals.ui.gui.clickgui.util.model.Translation;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.WindowController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ExpandedHeaderBar extends WidgetContainer {
    public final BindableToggleWidget hideButton;

    public final ToggleTextButton chatButton;

    public final DropdownWidget<Language> languageDropdown;

    public final DropdownWidget<GuiScale> scaleDropdown;

    public final HeaderBindingEditor bindingEditor;
    public final Runnable toggleCallback;

    public final ExpandedTabPanel tabPanel = new ExpandedTabPanel();

    public final CategoryChipBar categoryChipBar = new CategoryChipBar();

    public final GlTexture logoTexture = new GlTexture(new ClasspathResource("/icons/menu/new/client_logo.png")).setDimensions(26, 26);
    public final GlTexture tabsIcon = new GlTexture(new ClasspathResource("/icons/menu/new/tabs/misc.png"));
    public final GlTexture categoriesIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final MsdfFont labelFont = Fonts.INTER_BOLD.get();
    public final BindableToggleWidget searchButton = new BindableToggleWidget(() -> {
        FluxVisuals.INSTANCE.menuWindow().searchContainer().invert();
    }, Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/search.png")), 6.0f, 8.0f, 5.5f, 13).setBindingEditable(true).setEditableBinding(List.of(341, 70));

    public ExpandedHeaderBar(Runnable runnable) {
        this.toggleCallback = runnable;
        this.hideButton = new BindableToggleWidget(runnable, Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/unfold.png")), 6.0f, 8.0f, 5.5f, 13).setBindingEditable(true).setImmutableBinding(List.of(256)).setEditableBinding(List.of(258));
        BindableToggleWidget bindableToggleWidget = this.hideButton;
        Objects.requireNonNull(bindableToggleWidget);
        Supplier supplier = bindableToggleWidget::editableBinding;
        BindableToggleWidget class737Var2 = this.hideButton;
        Objects.requireNonNull(class737Var2);
        this.bindingEditor = new HeaderBindingEditor(supplier, class737Var2::setEditableBinding).maxBindingKeys(1);
        this.chatButton = new ToggleTextButton(() -> {
            // Chat removed - visual client only
        }, Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/chat.png")), 6.0f, 8.0f, 5.5f, 13);
        this.languageDropdown = new DropdownWidget<>(Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/language.png")), 6.0f, 8.0f, 5.5f, 13);
        this.languageDropdown.setOptions(Arrays.stream(Language.values()).map(language313 -> {
            return new DropdownOption<Language>(language313, Translation.clearText(language313.canonical()));
        }).toList(), FluxVisuals.INSTANCE.languages().current());
        this.languageDropdown.onSelect(class313Var2 -> {
            FluxVisuals.INSTANCE.menuWindow().contentArea().requestLanguage(class313Var2);
        });
        this.scaleDropdown = new DropdownWidget<>(Fonts.INTER_SEMIBOLD.get(), new GlTexture(new ClasspathResource("/icons/menu/new/display.png")), 6.0f, 8.0f, 5.5f, 13);
        this.scaleDropdown.setOptions(GuiScale.options(), currentGuiScale());
        this.scaleDropdown.onSelect(guiScale -> {
            if (guiScale.auto()) {
                FluxVisuals.INSTANCE.windowController().enableAutoDpiScale();
                try {
                    FluxVisuals.INSTANCE.configManager().menuStateConfig().setDpiScale(true, FluxVisuals.INSTANCE.windowController().dpiScaleFactor());
                    FluxVisuals.INSTANCE.configManager().saveMenuState();
                    return;
                } catch (IOException e) {
                    FluxVisuals.LOGGER.error("Failed to save menu scale state", e);
                    return;
                }
            }
            FluxVisuals.INSTANCE.windowController().setManualDpiScaleFactor(guiScale.scaleFactor());
            try {
                FluxVisuals.INSTANCE.configManager().menuStateConfig().setDpiScale(false, guiScale.scaleFactor());
                FluxVisuals.INSTANCE.configManager().saveMenuState();
            } catch (IOException e2) {
                FluxVisuals.LOGGER.error("Failed to save menu scale state", e2);
            }
        });
        addChild(this.tabPanel);
        addChild(this.categoryChipBar);
        addChild(this.bindingEditor);
        addChild(this.scaleDropdown);
        addChild(this.hideButton);
        addChild(this.languageDropdown);
        addChild(this.chatButton);
        addChild(this.searchButton);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        this.languageDropdown.label(Translation.clearText(FluxVisuals.INSTANCE.languages().current().canonical()), 12);
        this.chatButton.label(Lang.CHAT, 12);
        this.searchButton.label(Lang.SEARCH.effective(), 12);
        this.hideButton.label(Lang.HIDE.effective(), 12);
        this.hideButton.setActive(true);
        this.searchButton.setActive(FluxVisuals.INSTANCE.menuWindow().searchContainer().opened);
        drawCtx.roundedTexture(this.logoTexture, x() + 16.0f, (y() + (MenuWindow.COLLAPSED_HEADER_HEIGHT / 2.0f)) - (this.logoTexture.height() / 2), this.logoTexture.width(), this.logoTexture.height(), 6.0f, drawCtx.colorStack().white());
        super.render(drawCtx);
        DrawEngine drawEngine154 = drawCtx.drawEngine();
        String strEffective = Lang.ALL_TABS.effective();
        float fY = y() + MenuWindow.COLLAPSED_HEADER_HEIGHT + 9.0f;
        float f = fY + ((13.0f - 10.0f) / 2.0f);
        drawCtx.text(this.labelFont, strEffective.toUpperCase(), 10, x() + 15.0f, f, drawEngine154.colorStack().computeColor(drawCtx.theme().palette().text().tone(300).argb()));
        drawCtx.texture(this.tabsIcon, x() + 15.0f + drawCtx.textWidthPhysical(this.labelFont, strEffective, 10) + 13.0f + 5.0f, fY, 13.0f, 13.0f, drawEngine154.colorStack().white());
        float fWidth = this.tabPanel.width() > 0.0f ? this.tabPanel.width() : 270.0f;
        if (this.categoryChipBar.hasElements()) {
            String strEffective2 = Lang.VISIBLE_CATEGORIES.effective();
            float fTextWidthPhysical = drawCtx.textWidthPhysical(this.labelFont, strEffective2.toUpperCase(), 10);
            drawCtx.text(this.labelFont, strEffective2.toUpperCase(), 10, x() + fWidth + 20.0f, f, drawEngine154.colorStack().computeColor(drawCtx.theme().palette().text().tone(300).argb()));
            drawCtx.texture(this.categoriesIcon, x() + fWidth + 20.0f + fTextWidthPhysical + 5.0f, fY, 13.0f, 13.0f, drawEngine154.colorStack().white());
        }
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        InputEvent inputEvent691 = inputEventContext.inputEvent();
        if (super.handleInput(inputEventContext, z)) {
            return true;
        }
        if (z) {
            return false;
        }
        if (inputEvent691 instanceof CursorMoveInput) {
            return inputEventContext.inArea(x(), y(), width(), MenuWindow.MENU_HEIGHT);
        }
        if (inputEvent691 instanceof MouseButtonInput) {
            MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent691;
            if (mouseButtonInput.button() == 0 && mouseButtonInput.action().press()) {
                if (inputEventContext.inArea(x(), y(), width(), MenuWindow.EXPANDED_HEADER_HEIGHT)) {
                    FluxVisuals.INSTANCE.menuWindow().draggableBehavior().handleInput(inputEventContext, false);
                    return true;
                }
                if (this.toggleCallback == null) {
                    return false;
                }
                this.toggleCallback.run();
                return true;
            }
        }
        if (!(inputEvent691 instanceof ScrollInput)) {
            return false;
        }
        return inputEventContext.inArea(x(), y(), width(), MenuWindow.MENU_HEIGHT);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.languageDropdown.select(FluxVisuals.INSTANCE.languages().current());
        super.animation(weightedEngine);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(MenuWindow.MENU_WIDTH, MenuWindow.EXPANDED_HEADER_HEIGHT);
        float fX = ((x() + width()) - 16.0f);
        float fWidth = fX - (this.chatButton.width() + 16.0f);
        this.chatButton.setPosition(fWidth, y() + 19.0f);
        float fWidth2 = fWidth - (this.scaleDropdown.width() + 6.0f);
        this.scaleDropdown.setPosition(fWidth2, y() + 19.0f);
        float fWidth3 = fWidth2 - (this.languageDropdown.width() + 6.0f);
        this.languageDropdown.setPosition(fWidth3, y() + 19.0f);
        float fWidth4 = fWidth3 - (this.searchButton.width() + 6.0f);
        this.searchButton.setPosition(fWidth4, y() + 19.0f);
        this.hideButton.setPosition(fWidth4 - (this.hideButton.width() + 6.0f), y() + 19.0f);
        this.bindingEditor.setPosition(((x() + width()) - 15.0f) - this.bindingEditor.preferredWidth(layoutScaleContext), ((y() + MenuWindow.EXPANDED_HEADER_HEIGHT) - 17.0f) - this.bindingEditor.preferredHeight());
        this.bindingEditor.layout(layoutScaleContext);
        float fX2 = x() + (this.tabPanel.width() > 0.0f ? this.tabPanel.width() : 270.0f) + 20.0f;
        float fY = y() + MenuWindow.COLLAPSED_HEADER_HEIGHT + 33.0f;
        float f = (MenuWindow.MENU_WIDTH - fX2) - 16.0f;
        this.categoryChipBar.setPosition(fX2, fY);
        this.categoryChipBar.setSize(f, 60.0f);
        super.layout(layoutScaleContext);
    }

    public GuiScale currentGuiScale() {
        WindowController windowController686 = FluxVisuals.INSTANCE.windowController();
        return windowController686.autoDpiScale() ? GuiScale.AUTO : GuiScale.nearestScale(windowController686.dpiScaleFactor());
    }

    public void resetHotkeysState() {
        this.hideButton.resetPressedKeys();
        this.searchButton.resetPressedKeys();
    }

    public List<Integer> exchangeEditableBinding() {
        return this.hideButton.editableBinding();
    }
}
