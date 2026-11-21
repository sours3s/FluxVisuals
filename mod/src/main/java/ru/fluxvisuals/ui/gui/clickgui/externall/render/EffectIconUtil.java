package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Locale;

public class EffectIconUtil {

    public static Identifier getEffectTexture(ItemStack stack, String text) {
        if (text != null && !text.isEmpty()) {
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.contains("гнев") || lower.contains("силы") || lower.contains("сила") || lower.contains("strength")) {
                return InGameHud.getEffectTexture(StatusEffects.STRENGTH);
            }
            if (lower.contains("ассасин") || lower.contains("скорост") || lower.contains("speed")) {
                return InGameHud.getEffectTexture(StatusEffects.SPEED);
            }
            if (lower.contains("снотвор") || lower.contains("замедлен") || lower.contains("slowness")) {
                return InGameHud.getEffectTexture(StatusEffects.SLOWNESS);
            }
            if (lower.contains("радиаци") || lower.contains("отравлен") || lower.contains("poison")) {
                return InGameHud.getEffectTexture(StatusEffects.POISON);
            }
            if (lower.contains("палладин") || lower.contains("паладин") || lower.contains("сопротивлен") || lower.contains("resistance")) {
                return InGameHud.getEffectTexture(StatusEffects.RESISTANCE);
            }
            if (lower.contains("святая") || lower.contains("святой") || lower.contains("регенерат") || lower.contains("регенераци") || lower.contains("исцелен") || lower.contains("healing")) {
                return InGameHud.getEffectTexture(StatusEffects.REGENERATION);
            }
            if (lower.contains("огнестойк") || lower.contains("fire_res")) {
                return InGameHud.getEffectTexture(StatusEffects.FIRE_RESISTANCE);
            }
            if (lower.contains("невидим") || lower.contains("invis")) {
                return InGameHud.getEffectTexture(StatusEffects.INVISIBILITY);
            }
            if (lower.contains("поглощен") || lower.contains("absorp")) {
                return InGameHud.getEffectTexture(StatusEffects.ABSORPTION);
            }
            if (lower.contains("иссушен") || lower.contains("wither")) {
                return InGameHud.getEffectTexture(StatusEffects.WITHER);
            }
            if (lower.contains("ночн") || lower.contains("night_vis")) {
                return InGameHud.getEffectTexture(StatusEffects.NIGHT_VISION);
            }
            if (lower.contains("свечен") || lower.contains("glow")) {
                return InGameHud.getEffectTexture(StatusEffects.GLOWING);
            }
            if (lower.contains("прыгуч") || lower.contains("jump")) {
                return InGameHud.getEffectTexture(StatusEffects.JUMP_BOOST);
            }
            if (lower.contains("спешк") || lower.contains("haste")) {
                return InGameHud.getEffectTexture(StatusEffects.HASTE);
            }
            if (lower.contains("слепот") || lower.contains("blind")) {
                return InGameHud.getEffectTexture(StatusEffects.BLINDNESS);
            }
            if (lower.contains("слабост") || lower.contains("weak")) {
                return InGameHud.getEffectTexture(StatusEffects.WEAKNESS);
            }
            if (lower.contains("тошнот") || lower.contains("nausea")) {
                return InGameHud.getEffectTexture(StatusEffects.NAUSEA);
            }
            if (lower.contains("левитаци") || lower.contains("levitation")) {
                return InGameHud.getEffectTexture(StatusEffects.LEVITATION);
            }
        }

        if (stack != null && !stack.isEmpty()) {
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents != null) {
                for (StatusEffectInstance inst : contents.getEffects()) {
                    if (inst != null && inst.getEffectType() != null) {
                        return InGameHud.getEffectTexture(inst.getEffectType());
                    }
                }
            }
        }
        return null;
    }
}

