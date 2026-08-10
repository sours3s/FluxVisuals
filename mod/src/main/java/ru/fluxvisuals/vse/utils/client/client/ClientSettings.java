package ru.fluxvisuals.vse.utils.client.client;

import ru.fluxvisuals.vse.utils.math.ColorUtility;

import java.awt.*;

/**
 * Минимальный ClientSettings для порта рендер-системы GodWeer.
 * Только цветовые эффекты, которые нужны ClientRenderer.
 */
@SuppressWarnings({"unused", "FieldMayBeFinal"})
public final class ClientSettings {
    public static final ClientSettings INSTANCE = new ClientSettings();

    public enum ColorType { Single, Gradient, Rainbow }

    private ColorType colorType = ColorType.Gradient;
    private int colorSpeed = 9;

    private ClientSettings() {
    }

    public Color getColor(int index) {
        return switch (colorType) {
            case Single -> ClientColors.RED_L;
            case Gradient -> ColorUtility.transfusionEffect(colorSpeed, index, ClientColors.RED_L, ClientColors.RED);
            case Rainbow -> ColorUtility.rainbowEffect(colorSpeed, index);
        };
    }
    public Color getColorBright(int index) {
        return switch (colorType) {
            case Single -> ClientColors.RED_L;
            case Gradient -> ColorUtility.transfusionEffect(colorSpeed, index, ClientColors.RED_L, ClientColors.RED);
            case Rainbow -> ColorUtility.rainbowEffectBright(colorSpeed, index);
        };
    }
}
