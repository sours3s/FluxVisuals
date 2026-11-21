package ru.fluxvisuals.ui.gui.clickgui.externall.other;

public class GuiSounds {

    private static final String DIR = "gui/";

    private static long lastTick;
    private static long lastType;

    private static boolean pass(long last, long delay) {
        return System.currentTimeMillis() - last >= delay;
    }

    private static void play(String name, float volume) {
        SoundUtil.playSound_wav(DIR + name, volume);
    }

    private static void play(String name, float volume, float pitch) {
        SoundUtil.playSound_wav(DIR + name, volume, pitch);
    }

    public static void open() {
        play("gui_open", 0.45F);
    }

    public static void close() {
        play("gui_close", 0.45F);
    }

    public static void editor() {
        play("gui_anime", 0.4F);
    }

    public static void category(int index, int total) {
        float t = total <= 1 ? 0 : (float) index / (total - 1);
        play("gui_category", 0.45F, 0.97F + 0.08F * t);
    }

    public static void theme(int index, int total) {
        float t = total <= 1 ? 0 : (float) index / (total - 1);
        play("gui_theme", 0.45F, 0.95F + 0.14F * t);
    }

    public static void expand(boolean opened) {
        play(opened ? "gui_module_open" : "gui_module_close", 0.45F);
    }

    public static void toggle(boolean value) {
        play(value ? "gui_boolean_enable" : "gui_boolean_disable", 0.4F);
    }

    public static void chip(int index, int total) {
        float t = total <= 1 ? 0 : (float) index / (total - 1);
        play("gui_mode_multi", 0.45F, 0.95F + 0.15F * t);
    }

    public static void chipMulti(boolean value) {
        play(value ? "gui_boolean2_enable" : "gui_boolean2_disable", 0.4F);
    }

    public static void button() {
        play("gui_open_button", 0.4F);
    }

    public static void sliderGrab() {
        play("gui_click", 0.35F);
    }

    public static void sliderTick(float percent) {
        if (!pass(lastTick, 22)) return;
        lastTick = System.currentTimeMillis();
        play("gui_slider", 0.35F, 0.9F + 0.35F * Math.max(0, Math.min(1, percent)));
    }

    public static void sliderRelease() {
        play("gui_slider", 0.3F, 1.25F);
    }

    public static void bindStart() {
        play("gui_binding", 0.45F);
    }

    public static void bindSet() {
        play("gui_bind", 0.4F);
    }

    public static void bindReset() {
        play("gui_clear", 0.4F);
    }

    public static void editStart() {
        play("gui_click", 0.4F);
    }

    public static void editCommit() {
        play("gui_bind", 0.35F);
    }

    public static void editCancel() {
        play("gui_clear", 0.35F);
    }

    public static void type() {
        if (!pass(lastType, 25)) return;
        lastType = System.currentTimeMillis();
        play("gui_key_click", 0.35F, 1.0F + (float) (Math.random() * 0.12F - 0.06F));
    }

    public static void erase() {
        if (!pass(lastType, 25)) return;
        lastType = System.currentTimeMillis();
        play("gui_key_click", 0.3F, 0.88F);
    }

    public static void searchClear() {
        play("gui_clear", 0.4F);
    }

    public static void picker(boolean opened) {
        play(opened ? "gui_multi_open" : "gui_multi_close", 0.4F);
    }

    public static void colorTick(float percent) {
        if (!pass(lastTick, 28)) return;
        lastTick = System.currentTimeMillis();
        play("gui_slider", 0.3F, 0.9F + 0.35F * Math.max(0, Math.min(1, percent)));
    }

    public static void scroll() {
        if (!pass(lastTick, 55)) return;
        lastTick = System.currentTimeMillis();
        play("gui_scroll", 0.35F);
    }
}

