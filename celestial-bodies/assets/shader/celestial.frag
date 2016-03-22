#ifdef GL_ES
precision mediump float;
#endif

uniform float u_viewportWidth;
uniform float u_viewportHeight;

uniform float u_celestialBodiesParams[500];

void main() {
    float aspectRatio = u_viewportWidth/u_viewportHeight;

    //calculate fragment position in screen space
    vec2 fragmentScreenCoords = vec2(gl_FragCoord.x / u_viewportWidth, gl_FragCoord.y / u_viewportHeight);

    fragmentScreenCoords.x *= aspectRatio;

    bool hasBody = false;
    int arrayIndex = 1;
    int celestialBodyCount = int(u_celestialBodiesParams[0]);
    for (int i=0; i < celestialBodyCount; i++) {
        int bodyType = int(u_celestialBodiesParams[arrayIndex++]);

        vec2 bodyPos = vec2(u_celestialBodiesParams[arrayIndex + 0], u_celestialBodiesParams[arrayIndex + 1]);
        bodyPos.x *= aspectRatio;

        float size = u_celestialBodiesParams[arrayIndex + 6];
        //check if it is in the radius of the star
        if (length(fragmentScreenCoords - bodyPos) < size) {
            hasBody = true;
            gl_FragColor = vec4(
                u_celestialBodiesParams[arrayIndex + 2],
                u_celestialBodiesParams[arrayIndex + 3],
                u_celestialBodiesParams[arrayIndex + 4],
                u_celestialBodiesParams[arrayIndex + 5]);
            break;
        }

        arrayIndex += 7;
    }

    if (!hasBody) {
        discard;
    }
}