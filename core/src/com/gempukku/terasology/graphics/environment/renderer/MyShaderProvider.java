package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class MyShaderProvider implements ShaderProvider {
    private ChunkLightShader chunkLightShader;
    private ChunkShader chunkShader;

    private boolean shadowRendering;
    private Matrix4 lightTrans;
    private Vector3 lightPosition;
    private Vector3 lightDirection;
    private float lightPlaneDistance;
    private float cameraFar;

    public void setShadowPass(boolean shadowRendering) {
        this.shadowRendering = shadowRendering;
    }

    public void setLightCameraFar(float cameraFar) {
        this.cameraFar = cameraFar;
    }

    public void setLightTrans(Matrix4 lightTrans) {
        this.lightTrans = lightTrans;
    }

    public void setLightPosition(Vector3 lightPosition) {
        this.lightPosition = lightPosition;
    }

    public void setLightDirection(Vector3 lightDirection) {
        this.lightDirection = lightDirection;
    }

    public void setLightPlaneDistance(float lightPlaneDistance) {
        this.lightPlaneDistance = lightPlaneDistance;
    }

    @Override
    public Shader getShader(Renderable renderable) {
        if (shadowRendering) {
            if (chunkLightShader == null)
                chunkLightShader = createChunkLightShader(renderable);
            chunkLightShader.setCameraFar(cameraFar);
            chunkLightShader.setLightPosition(lightPosition);
            chunkLightShader.setLightDirection(lightDirection);
            chunkLightShader.setLightPlaneDistance(lightPlaneDistance);
            return chunkLightShader;
        } else {
            if (chunkShader == null)
                chunkShader = createChunkShader(renderable);
            chunkShader.setLightTrans(lightTrans);
            chunkShader.setCameraFar(cameraFar);
            chunkShader.setLightPosition(lightPosition);
            chunkShader.setLightDirection(lightDirection);
            chunkShader.setLightPlaneDistance(lightPlaneDistance);
            return chunkShader;
        }
    }

    @Override
    public void dispose() {
        if (chunkLightShader != null)
            chunkLightShader.dispose();
        if (chunkShader != null)
            chunkShader.dispose();
    }

    private ChunkShader createChunkShader(Renderable renderable) {
        DefaultShader.Config config = new DefaultShader.Config(
                Gdx.files.internal("shader/chunk.vert").readString(),
                Gdx.files.internal("shader/chunk.frag").readString());
        ChunkShader chunkShader = new ChunkShader(renderable, config);
        chunkShader.init();
        return chunkShader;
    }

    private ChunkLightShader createChunkLightShader(Renderable renderable) {
        DefaultShader.Config config = new DefaultShader.Config(
                Gdx.files.internal("shader/chunkShadow.vert").readString(),
                Gdx.files.internal("shader/chunkShadow.frag").readString());
        ChunkLightShader chunkLightShader = new ChunkLightShader(renderable, config);
        chunkLightShader.init();
        return chunkLightShader;
    }
}
