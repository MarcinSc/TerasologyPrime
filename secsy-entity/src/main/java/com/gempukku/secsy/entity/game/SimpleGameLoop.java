package com.gempukku.secsy.entity.game;

import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.util.PriorityCollection;

@RegisterSystem(shared = {GameLoop.class, InternalGameLoop.class})
public class SimpleGameLoop implements GameLoop, InternalGameLoop {
    private PriorityCollection<GameLoopListener> gameLoopListeners = new PriorityCollection<>();
    private PriorityCollection<InternalGameLoopListener> internalGameLoopListeners = new PriorityCollection<>();

    @Override
    public void addGameLoopListener(GameLoopListener gameLoopListener) {
        gameLoopListeners.add(gameLoopListener);
    }

    @Override
    public void removeGameLoopListener(GameLoopListener gameLoopListener) {
        gameLoopListeners.remove(gameLoopListener);
    }

    @Override
    public void addInternalGameLoopListener(InternalGameLoopListener internalGameLoopListener) {
        internalGameLoopListeners.add(internalGameLoopListener);
    }

    @Override
    public void removeInternalGameLooplListener(InternalGameLoopListener internalGameLoopListener) {
        internalGameLoopListeners.remove(internalGameLoopListener);
    }

    @Override
    public void processUpdate() {
        for (InternalGameLoopListener internalGameLoopListener : internalGameLoopListeners) {
            internalGameLoopListener.preUpdate();
        }

        for (GameLoopListener gameLoopListener : gameLoopListeners) {
            gameLoopListener.update();
        }

        for (InternalGameLoopListener internalGameLoopListener : internalGameLoopListeners) {
            internalGameLoopListener.postUpdate();
        }
    }
}
