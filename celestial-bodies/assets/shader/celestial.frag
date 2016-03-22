#ifdef GL_ES
precision mediump float;
#endif

uniform float u_viewportWidth;
uniform float u_viewportHeight;

uniform float u_celestialBodiesParams[500];

${functions}

void main() {

    float aspectRatio = u_viewportWidth/u_viewportHeight;

    //calculate fragment position in screen space
    vec2 fragmentScreenCoords = vec2(gl_FragCoord.x / u_viewportWidth, gl_FragCoord.y / u_viewportHeight);

    fragmentScreenCoords.x *= aspectRatio;

    bool hasBody = false;
    int arrayIndex = 1;
    int celestialBodyCount = int(u_celestialBodiesParams[0]);
    for (int i=0; i < celestialBodyCount; i++) {
        float bodyType = u_celestialBodiesParams[arrayIndex++];

        vec4 result;
        ${dispatch}
        if (result.a > 0.0) {
            hasBody = true;
            gl_FragColor = result;
            break;
        }
    }

    if (!hasBody) {
        discard;
    }
}
