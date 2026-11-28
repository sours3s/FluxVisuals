package ru.fluxvisuals.render.nametags;

/** Per-player easing state for nametag elements. */
final class NameTagAnimations {
    private static final float SPEED = 5.0F;

    private final Progress leftHand = new Progress();
    private final Progress rightHand = new Progress();
    private final Progress armor = new Progress();
    private final Progress mainHand = new Progress();
    private final Progress offHand = new Progress();
    private final Progress[] armorSlots = {new Progress(), new Progress(), new Progress(), new Progress()};

    private float scale = Float.NaN;
    private boolean detailed = true;
    private boolean initialized;

    void update(float delta, float targetScale, boolean simplify, double distance, float simplifyRange,
                  boolean showLeftHand, boolean showRightHand, boolean showArmor,
                  boolean offHandPresent, boolean mainHandPresent, boolean[] showArmorSlot) {
        if (!simplify) {
            detailed = true;
        } else if (detailed) {
            if (distance > simplifyRange + 4.0F) detailed = false;
        } else if (distance < simplifyRange) {
            detailed = true;
        }

        boolean leftTarget = detailed && showLeftHand;
        boolean rightTarget = detailed && showRightHand;
        boolean armorTarget = detailed && showArmor;
        boolean offTarget = offHandPresent;
        boolean mainTarget = mainHandPresent;

        if (!initialized) {
            leftHand.snap(leftTarget);
            rightHand.snap(rightTarget);
            armor.snap(armorTarget);
            offHand.snap(offTarget);
            mainHand.snap(mainTarget);
            for (int i = 0; i < armorSlots.length; i++) {
                armorSlots[i].snap(showArmorSlot[i]);
            }
            scale = targetScale;
            initialized = true;
        }

        leftHand.step(delta, leftTarget);
        rightHand.step(delta, rightTarget);
        armor.step(delta, armorTarget);
        offHand.step(delta, offTarget);
        mainHand.step(delta, mainTarget);
        for (int i = 0; i < armorSlots.length; i++) {
            armorSlots[i].step(delta, showArmorSlot[i]);
        }

        scale += (targetScale - scale) * Math.min(1.0F, delta * 10.0F);
    }

    float leftHand() {
        return leftHand.value();
    }

    float rightHand() {
        return rightHand.value();
    }

    float armor() {
        return armor.value();
    }

    float mainHand() {
        return mainHand.value();
    }

    float offHand() {
        return offHand.value();
    }

    float armorSlot(int index) {
        return armorSlots[index].value();
    }

    float scale() {
        return scale;
    }

    boolean detailed() {
        return detailed;
    }

    private static final class Progress {
        private float value;

        void snap(boolean target) {
            value = target ? 1.0F : 0.0F;
        }

        void step(float delta, boolean target) {
            float amount = Math.min(delta * SPEED, 1.0F);
            value = target ? Math.min(1.0F, value + amount) : Math.max(0.0F, value - amount);
        }

        float value() {
            float progress = Math.min(1.0F, Math.max(0.0F, value));
            if (progress < 0.5F) return 4.0F * progress * progress * progress;
            float inverted = -2.0F * progress + 2.0F;
            return 1.0F - inverted * inverted * inverted * 0.5F;
        }
    }
}
