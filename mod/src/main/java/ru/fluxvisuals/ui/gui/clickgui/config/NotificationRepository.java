package ru.fluxvisuals.ui.gui.clickgui.config;
import ru.fluxvisuals.ui.gui.clickgui.util.render.AnimatedFloat;
import ru.fluxvisuals.ui.gui.clickgui.util.math.Easings;
import ru.fluxvisuals.ui.gui.clickgui.Notification;
import ru.fluxvisuals.ui.gui.clickgui.util.type.NotificationType;
import ru.fluxvisuals.ui.gui.clickgui.util.WeightedEngine;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class NotificationRepository {
    public final List<Notification> notifications = new CopyOnWriteArrayList();

    public void post(NotificationType notificationType, Text text, long j, TimeUnit timeUnit) {
        long millis = timeUnit.toMillis(j);
        AnimatedFloat animatedFloat = new AnimatedFloat(250, Easings.LINEAR);
        animatedFloat.destination(1.0f);
        this.notifications.add(new Notification(animatedFloat, notificationType, text, System.currentTimeMillis(), millis, null));
        if (this.notifications.size() > 5) {
            this.notifications.removeFirst();
        }
        this.notifications.sort(Comparator.comparingDouble(notification -> {
            return -notification.duration();
        }));
    }

    public void post(NotificationType notificationType, Text text, long j) {
        post(notificationType, text, j, TimeUnit.MILLISECONDS);
    }

    public void post(Text text, ItemStack itemStack, long j, TimeUnit timeUnit) {
        long millis = timeUnit.toMillis(j);
        AnimatedFloat animatedFloat = new AnimatedFloat(250, Easings.LINEAR);
        animatedFloat.destination(1.0f);
        this.notifications.add(new Notification(animatedFloat, NotificationType.INFO, text, System.currentTimeMillis(), millis, itemStack.copy()));
        if (this.notifications.size() > 5) {
            this.notifications.removeFirst();
        }
        this.notifications.sort(Comparator.comparingDouble(notification -> {
            return -notification.duration();
        }));
    }

    public void post(Text text, ItemStack itemStack, long j) {
        post(text, itemStack, j, TimeUnit.MILLISECONDS);
    }

    public void tick() {
        this.notifications.forEach(notification -> {
            if (notification.isExpired()) {
                notification.valueAnimation().destination(0.0f);
            }
        });
        this.notifications.removeIf(class657Var2 -> {
            return class657Var2.isExpired() && class657Var2.valueAnimation().animatedValue() <= 0.0f;
        });
    }

    public void animate(WeightedEngine weightedEngine) {
        this.notifications.forEach(notification -> {
            notification.valueAnimation().animate(weightedEngine);
        });
    }

    public List<Notification> getNotifications() {
        return this.notifications;
    }
}
