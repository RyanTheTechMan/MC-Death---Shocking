package com.ryanthetechman.death_detector.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text;

public interface ClientChatCallback {
    Event<ClientChatCallback> EVENT = EventFactory.createArrayBacked(ClientChatCallback.class, (listeners) -> (message) -> {
        for (ClientChatCallback listener : listeners) {
            listener.dispatch(message);
        }
    });

    void dispatch(Text message);
}
