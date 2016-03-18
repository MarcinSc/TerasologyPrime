package com.gempukku.terasology.time.delay;

import com.gempukku.secsy.entity.event.Event;

public class DelayedActionTriggeredEvent extends Event {
    public final String actionId;

    public DelayedActionTriggeredEvent(String actionId) {
        this.actionId = actionId;
    }
}
