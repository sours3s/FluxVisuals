package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.FluxVisuals;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatUtil {
    public static void addChatMessage(Text text) {
        MutableText mutableTextAppend = buildPrefix().copy().append(text);
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player != null) {
            player.sendMessage(mutableTextAppend, false);
        }
    }

    public static Text gradientText(String str, int i, int i2) {
        MutableText mutableTextLiteral = Text.literal("");
        int length = str.length();
        for (int i3 = 0; i3 < length; i3++) {
            float f = i3 / (length - 1);
            int i4 = (((int) ((((i >> 16) & StencilBufferUtil.STENCIL_MASK) * (1.0f - f)) + (((i2 >> 16) & StencilBufferUtil.STENCIL_MASK) * f))) << 16) | (((int) ((((i >> 8) & StencilBufferUtil.STENCIL_MASK) * (1.0f - f)) + (((i2 >> 8) & StencilBufferUtil.STENCIL_MASK) * f))) << 8) | ((int) (((i & StencilBufferUtil.STENCIL_MASK) * (1.0f - f)) + ((i2 & StencilBufferUtil.STENCIL_MASK) * f)));
            mutableTextLiteral = mutableTextLiteral.copy().append(Text.literal(String.valueOf(str.charAt(i3))).styled(style -> {
                return style.withColor(i4);
            }));
        }
        return mutableTextLiteral;
    }

    public static void addChatMessage(String str, boolean z) {
        MutableText mutableTextAppend = buildPrefix().copy().append(Text.literal(str).styled(style -> {
            return style.withColor(Formatting.WHITE);
        }));
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player != null) {
            player.sendMessage(mutableTextAppend, z);
        }
    }

    public static void addChatMessage(String str) {
        addChatMessage(str, false);
    }

    public static Text buildPrefix() {
        return Text.literal("").append(gradientText("FluxVisuals", 5926655, 10860799)).append(Text.literal(" -> ").styled(style -> {
            return style.withColor(Formatting.DARK_GRAY);
        }));
    }

    }
