package com.gempukku.secsy.entity.game;

import com.gempukku.secsy.context.annotation.API;

@API
public interface GameLoop {
    void addGameLoopListener(GameLoopListener gameLoopListener);
    void removeGameLoopListener(GameLoopListener gameLoopListener);
}
