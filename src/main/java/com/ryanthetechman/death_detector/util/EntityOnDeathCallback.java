package com.ryanthetechman.death_detector.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.damage.DamageSource;

public interface EntityOnDeathCallback {
    Event<EntityOnDeathCallback> EVENT = EventFactory.createArrayBacked(EntityOnDeathCallback.class, (listeners) -> (source) -> {
        for (EntityOnDeathCallback listener : listeners) {
            listener.dispatch(source);
        }
    });

    void dispatch(DamageSource source);
}
