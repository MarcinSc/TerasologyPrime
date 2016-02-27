package com.gempukku.secsy.entity.game;

import com.gempukku.secsy.context.annotation.RegisterSystem;

import java.util.LinkedList;
import java.util.List;

@RegisterSystem(shared = {GameLoop.class, InternalGameLoop.class})
public class SimpleGameLoop implements GameLoop, InternalGameLoop {
    private List<GameLoopListener> gameLoopListeners = new LinkedList<>();
    private List<InternalGameLoopListener> internalGameLoopListeners = new LinkedList<>();

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
    public void processUpdate(long delta) {
        for (InternalGameLoopListener internalGameLoopListener : internalGameLoopListeners) {
            internalGameLoopListener.preUpdate();
        }

        for (GameLoopListener gameLoopListener : gameLoopListeners) {
            gameLoopListener.update(delta);
        }

        for (InternalGameLoopListener internalGameLoopListener : internalGameLoopListeners) {
            internalGameLoopListener.postUpdate();
        }
    }
}
