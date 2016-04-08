package com.gempukku.terasology.graphics.environment.mesh;

import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ShortArray;
import com.gempukku.terasology.world.chunk.geometry.BlockGeometryGenerator;

public class ArrayBlockVertexOutput implements BlockGeometryGenerator.BlockVertexOutput {
    private IntArray blocks;
    private FloatArray vertices;
    private ShortArray indices;

    private int blockX;
    private int blockY;
    private int blockZ;

    private short vertexIndex;
    private float x;
    private float y;
    private float z;
    private float normalX;
    private float normalY;
    private float normalZ;
    private float textureCoordX;
    private float textureCoordY;
    private int flag;

    public ArrayBlockVertexOutput(IntArray blocks, FloatArray vertices, ShortArray indices) {
        this.blocks = blocks;
        this.vertices = vertices;
        this.indices = indices;
    }

    @Override
    public void setBlock(int x, int y, int z) {
        blockX = x;
        blockY = y;
        blockZ = z;
    }

    @Override
    public short finishVertex() {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(normalX);
        vertices.add(normalY);
        vertices.add(normalZ);
        vertices.add(textureCoordX);
        vertices.add(textureCoordY);
        vertices.add(flag);

        x = y = z = normalX = normalY = normalZ = textureCoordX = textureCoordY = flag = 0;

        return vertexIndex++;
    }

    @Override
    public void setNormal(float x, float y, float z) {
        normalX = x;
        normalY = y;
        normalZ = z;
    }

    @Override
    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void setTextureCoordinate(float x, float y) {
        textureCoordX = x;
        textureCoordY = y;
    }

    @Override
    public void setFlag(int flag) {
        this.flag = flag;
    }

    @Override
    public void addVertexIndex(short vertexIndex) {
        blocks.add(blockX);
        blocks.add(blockY);
        blocks.add(blockZ);
        indices.add(vertexIndex);
    }
}
