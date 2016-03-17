package com.gempukku.terasology.movement.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.terasology.movement.MovementController;

@RegisterSystem(
        profiles = {NetProfiles.CLIENT, "keyboardController"})
public class KeyboardController implements GameLoopListener, LifeCycleSystem {
    @In
    private GameLoop gameLoop;
    @In
    private MovementController movementController;

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
    }

    @Override
    public void update(long delta) {
        float rotateStep = 0.1f;

        float speed = 0;
        float verticalSpeed = 0;
        float yaw = movementController.getYaw();

        if (Gdx.input.isKeyPressed(Input.Keys.UP) && !Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            speed = movementController.getMaximumSpeed();
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) && !Gdx.input.isKeyPressed(Input.Keys.UP)) {
            speed = -movementController.getMaximumSpeed();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            verticalSpeed = movementController.getJumpSpeed();
        } else if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && !Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            verticalSpeed = -movementController.getJumpSpeed();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) && !Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            yaw -= rotateStep;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) && !Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            yaw += rotateStep;
        }

        movementController.updateMovement(yaw, speed, verticalSpeed);
    }
}
