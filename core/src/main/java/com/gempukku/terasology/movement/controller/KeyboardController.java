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

    private int[] forward = {Input.Keys.UP, Input.Keys.W};
    private int[] backward = {Input.Keys.DOWN, Input.Keys.S};
    private int[] left = {Input.Keys.LEFT, Input.Keys.A};
    private int[] right = {Input.Keys.RIGHT, Input.Keys.D};
    private int[] jump = {Input.Keys.SPACE};
    private int[] down = {Input.Keys.SHIFT_LEFT};

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
    }

    @Override
    public void update() {
        float rotateStep = 0.05f;

        MovementController.Mode mode = movementController.getMode();
        float speed = 0;
        float verticalSpeed = movementController.getVerticalSpeed();
        float yaw = movementController.getYaw();

        if (isPressed(forward) && !isPressed(backward)) {
            speed = movementController.getMaximumSpeed();
        } else if (isPressed(backward) && !isPressed(forward)) {
            speed = -movementController.getMaximumSpeed();
        }
        if (isPressed(left) && !isPressed(right)) {
            yaw -= rotateStep;
        } else if (isPressed(right) && !isPressed(left)) {
            yaw += rotateStep;
        }
        if (mode == MovementController.Mode.WALKING) {
            if (isPressed(jump) && !isPressed(down)) {
                verticalSpeed = movementController.getJumpSpeed();
                mode = MovementController.Mode.FREE_FALL;
            } else if (isPressed(down) && !isPressed(jump)) {
                verticalSpeed = -movementController.getJumpSpeed();
            }
        }
        movementController.updateMovement(mode, yaw, movementController.getPitch(), speed, verticalSpeed);
    }

    private boolean isPressed(int[] keys) {
        for (int key : keys) {
            if (Gdx.input.isKeyPressed(key))
                return true;
        }
        return false;
    }
}
