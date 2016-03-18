package com.gempukku.terasology.movement.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.terasology.movement.MovementController;

@RegisterSystem(
        profiles = {NetProfiles.CLIENT, "mouseController"})
public class MouseController implements GameLoopListener, LifeCycleSystem {
    @In
    private GameLoop gameLoop;
    @In
    private MovementController movementController;

    private boolean initialized = false;
    private int mouseX;
    private int mouseY;

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
    }

    @Override
    public void update() {
        if (initialized) {
            int newMouseX = Gdx.input.getX();
            int newMouseY = Gdx.input.getY();

            int diffX = newMouseX - mouseX;
            int diffY = newMouseY - mouseY;

            float rotateStep = 0.005f;

            float yaw = movementController.getYaw();
            float pitch = movementController.getPitch();

            yaw += diffX * rotateStep;
            pitch += -diffY * rotateStep;

            pitch = MathUtils.clamp(pitch, -(float) Math.PI / 2f, (float) Math.PI / 2f);

            movementController.updateMovement(yaw, pitch, movementController.getHorizontalSpeed(), movementController.getVerticalSpeed());

            mouseX = newMouseX;
            mouseY = newMouseY;
        } else {
            mouseX = Gdx.input.getX();
            mouseY = Gdx.input.getY();
            initialized = true;
        }
    }
}
