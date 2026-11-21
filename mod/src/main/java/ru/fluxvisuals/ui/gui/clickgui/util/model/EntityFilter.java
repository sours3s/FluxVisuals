package ru.fluxvisuals.ui.gui.clickgui.util.model;
import ru.fluxvisuals.ui.gui.clickgui.util.type.EntityCategory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.entity.Entity;

public final class EntityFilter {
    public final Set<EntityCategory> categories = new HashSet<EntityCategory>();

    public EntityFilter add(EntityCategory entityCategory) {
        this.categories.add(entityCategory);
        return this;
    }

    public boolean matches(Entity entity) {
        Iterator it = this.categories.iterator();
        while (it.hasNext()) {
            if (((EntityCategory) it.next()).matches(entity)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.categories.isEmpty();
    }

    public static EntityCategory resolve(Entity entity) {
        for (EntityCategory entityCategory : EntityCategory.values()) {
            if (entityCategory.matches(entity)) {
                return entityCategory;
            }
        }
        return null;
    }

    public static EntityCategory resolve(Entity entity, EntityCategory... entityCategorys) {
        for (EntityCategory entityCategory : entityCategorys) {
            if (entityCategory.matches(entity)) {
                return entityCategory;
            }
        }
        return null;
    }
}
