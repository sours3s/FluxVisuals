package ru.fluxvisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Реестр модулей и диспетчер событий (tick / рендер / клавиши). */
public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();
    private final Map<Category, List<Module>> byCategory = new HashMap<>();
    private final Set<Integer> heldKeys = new HashSet<>();

    private ModuleManager() {}

    public void register(Module module) {
        modules.add(module);
        byCategory.computeIfAbsent(module.category, c -> new ArrayList<>()).add(module);
    }

    public List<Module> getAll() { return modules; }
    public List<Module> getByCategory(Category category) { return byCategory.getOrDefault(category, List.of()); }

    public Module get(String name) {
        for (Module m : modules) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    /** Вызывается каждый клиентский тик. */
    public void tick(MinecraftClient mc) {
        handleKeys(mc);
        for (Module m : modules) if (m.isEnabled()) m.onTick();
    }

    private void handleKeys(MinecraftClient mc) {
        if (mc == null || mc.getWindow() == null) return;
        // пока открыт какой-то экран (инвентарь, чат, ClickGUI) — горячие клавиши не срабатывают,
        // чтобы не конфликтовать с вводом в экран (закрытие ClickGUI обрабатывает сам Screen)
        if (mc.currentScreen != null) return;
        Window window = mc.getWindow();
        for (Module m : modules) {
            if (m.getKey() == 0) continue;
            boolean pressed = InputUtil.isKeyPressed(window, m.getKey());
            if (pressed && !heldKeys.contains(m.getKey())) {
                m.toggle();
            }
        }
        // Обновляем набор удерживаемых клавиш
        Set<Integer> currentlyHeld = new HashSet<>();
        for (Module m : modules) {
            if (m.getKey() == 0) continue;
            if (InputUtil.isKeyPressed(window, m.getKey())) currentlyHeld.add(m.getKey());
        }
        heldKeys.retainAll(currentlyHeld);
        heldKeys.addAll(currentlyHeld);
    }

    /** 2D-рендер HUD. */
    public void render2D(DrawContext g, float tickDelta) {
        for (Module m : modules) {
            if (m.isEnabled()) {
                try { m.onRender2D(g, tickDelta); }
                catch (Exception e) { /* не роняем игру из-за одного модуля */ }
            }
        }
    }

    /** 3D-рендер мира. */
    public void render3D(MatrixStack ms, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
        for (Module m : modules) {
            if (m.isEnabled()) {
                try { m.onRender3D(ms, consumers, camera, tickDelta); }
                catch (Exception e) { }
            }
        }
    }

    public void toggleModule(String name) {
        Module m = get(name);
        if (m != null) m.toggle();
    }
}
