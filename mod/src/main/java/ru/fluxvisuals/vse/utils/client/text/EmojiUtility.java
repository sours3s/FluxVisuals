package ru.fluxvisuals.vse.utils.client.text;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class EmojiUtility {

    private final ArrayList<Integer> cached = new ArrayList<>();

    public boolean isEmoji(int code) {
        if (cached.contains(code))
            return true;
        if (isIn(code, 0x1F300, 0x1F5FF) ||
                isIn(code, 0x1F600, 0x1F64F) ||
                isIn(code, 0x1F680, 0x1F6FF) ||
                isIn(code, 0x1F700, 0x1F77F) ||
                isIn(code, 0x1F780, 0x1F7FF) ||
                isIn(code, 0x1F800, 0x1F8FF) ||
                isIn(code, 0x1F900, 0x1F9FF) ||
                isIn(code, 0x1FA70, 0x1FAFF) || code == '⭐') {
            cached.add(code);
            return true;
        }
        return false;
    }

    private boolean isIn(int codePoint, int min, int max) {
        return codePoint >= min && codePoint <= max;
    }
}
