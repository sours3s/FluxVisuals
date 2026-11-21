package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.model.PriorityValueEntry;

import java.util.PriorityQueue;

public class PriorityValueQueue<T> {
    public int tickCounter = 0;
    public final PriorityQueue<PriorityValueEntry<T>> queue = new PriorityQueue<>((priorityValueEntry, class413Var2) -> {
        return Integer.compare(class413Var2.priority, priorityValueEntry.priority);
    });

    public void tick(int i) {
        this.tickCounter += i;
    }

    public void addTask(PriorityValueEntry<T> priorityValueEntry) {
        this.queue.removeIf(class413Var2 -> {
            return class413Var2.provider == priorityValueEntry.provider;
        });
        ((PriorityValueEntry) priorityValueEntry).expiresIn += this.tickCounter;
        this.queue.add(priorityValueEntry);
    }

    public T getValue() {
        while (!this.queue.isEmpty()) {
            PriorityValueEntry<T> priorityValueEntry = this.queue.peek();
            if (((PriorityValueEntry<T>) priorityValueEntry).expiresIn > this.tickCounter) {
                return ((PriorityValueEntry<T>) priorityValueEntry).value;
            }
            this.queue.poll();
        }
        return null;
    }
}
