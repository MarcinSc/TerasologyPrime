package com.gempukku.terasology.graphics.environment.mesh;

public interface VertexOutput {
    void setPosition(float x, float y, float z);

    void setNormal(float x, float y, float z);

    void setTextureCoordinate(float x, float y);

    void setFlag(int flag);

    short finishVertex();

    void addVertexIndex(short vertexIndex);
}
