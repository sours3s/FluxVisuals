package ru.fluxvisuals.vse.utils.client.text;

import ru.fluxvisuals.vse.utils.math.ColorUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;

@UtilityClass
public class TextUtility {
    ArrayList<Integer> lettersNormal = new ArrayList<>();
    ArrayList<Integer> lettersSmall = new ArrayList<>();

    public Text applyGradient(String text, Color first, Color second) {
        MutableText result = Text.empty();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Color color = ColorUtility.linear(first, second, (float) Math.sin(i / 10.f));
            result.append(Text.of(String.valueOf(c)).copy().withColor(color.getRGB()));
        }
        return result;
    }
    public String keyToString(int key) {
        if (key == -1) return "None";

        if (key <= -100) {
            int mouseButton = -(key + 100);
            return switch (mouseButton) {
                case 0 -> "LMB";
                case 1 -> "RMB";
                case 2 -> "MMB";
                default -> "M" + (mouseButton + 1);
            };
        }
        
        if (key < 0) return "None";
        String k = GLFW.glfwGetKeyName(key, 0);

        return key > 7 ?
                k == null ? InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString() : k.toUpperCase() :
                InputUtil.Type.MOUSE.createFromCode(key).getLocalizedText().getString();
    }

    public String smallToNormal(String smallCaps) {
        StringBuilder builder = new StringBuilder();
        for (char ch : smallCaps.toCharArray()) {
            builder.append(charToNormal(ch));
        }
        return builder.toString().replace('ꜱ', 's');
    }
    public char charToNormal(char ch) {
        if (lettersSmall.isEmpty() || lettersNormal.isEmpty() || !lettersSmall.contains((int) ch)) return ch;
        return (char)(int)lettersNormal.get(lettersSmall.indexOf((int)ch));
    }

    public String ticksToTime(int ticks) {
        int s = Math.round(ticks * 0.05f);
        if (s > 3600) {
            int h = s / 3600;
            s %= 3600;
            int m = s / 60;
            s %= 60;
            return String.format("%02d:%02d:%02d", h, m, s);
        } else if (s > 60) {
            int m = s / 60;
            s %= 60;
            return String.format("%02d:%02d", m, s);
        } else {
            return String.format("00:%02d", s);
        }
    }

    static {
        for (char ch: "abcdefghijklmnopqrstuvwxyz".toCharArray()) {
            lettersNormal.add((int)ch);
        }
        for (char ch: "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘᴏʀsᴛᴜᴠᴡxʏᴢ".toCharArray()) {
            lettersSmall.add((int)ch);
        }
    }
}
