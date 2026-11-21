package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PriorityTaskEntry;

import java.util.PriorityQueue;

public class PriorityTaskQueue<T> {
    public int tickCounter = 0;
    public PriorityQueue<PriorityTaskEntry<T>> activeTasks = new PriorityQueue<>((priorityTaskEntry, class194Var2) -> {
        return Integer.compare(class194Var2.priority, priorityTaskEntry.priority);
    });

    public void tick(int i) {
        this.tickCounter += i;
    }

    public void addTask(PriorityTaskEntry<T> priorityTaskEntry) {
        this.activeTasks.removeIf(class194Var2 -> {
            return class194Var2.module.equals(priorityTaskEntry.module);
        });
        priorityTaskEntry.expiresAt += this.tickCounter;
        this.activeTasks.add(priorityTaskEntry);
    }

    public T fetchActiveTaskValue() {
        while (!this.activeTasks.isEmpty() && this.activeTasks.peek() != null && (this.activeTasks.peek().expiresAt <= this.tickCounter || !this.activeTasks.peek().module.isState())) {
            this.activeTasks.poll();
        }
        if (this.activeTasks.isEmpty() || this.activeTasks.peek() == null) {
            return null;
        }
        return (T) this.activeTasks.peek().module;
    }
}
