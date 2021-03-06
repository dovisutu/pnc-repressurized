package me.desht.pneumaticcraft.common.ai;

import com.google.common.base.Predicate;
import me.desht.pneumaticcraft.common.util.EntityFilter;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StringFilterEntitySelector implements Predicate<Entity> {
    private List<EntityFilter> filters = new ArrayList<>();

    @Override
    public boolean apply(Entity entity) {
        for (EntityFilter f : getFilter()) {
            if (f.apply(entity)) return true;
        }
        return false;
    }

    protected List<EntityFilter> getFilter() {
        return filters;
    }

    public StringFilterEntitySelector setFilter(String filterStr) {
        EntityFilter filter = EntityFilter.fromString(filterStr);
        if (filter != null) {
            this.filters = Collections.singletonList(filter);
        } else {
            this.filters.clear();
        }
        return this;
    }

    public StringFilterEntitySelector setFilter(List<EntityFilter> filters) {
        this.filters = filters;
        return this;
    }

    public StringFilterEntitySelector addEntry(String filterStr) {
        EntityFilter filter = EntityFilter.fromString(filterStr);
        if (filter != null) {
            filters.add(filter);
        }
        return this;
    }
}
