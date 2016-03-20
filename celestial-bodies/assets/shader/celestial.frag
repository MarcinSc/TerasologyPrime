#ifdef GL_ES
precision mediump float;
#endif

uniform float u_viewportWidth;
uniform float u_viewportHeight;

uniform float u_celestialBodiesParams[490];

void main() {
    float aspectRatio = u_viewportWidth/u_viewportHeight;

    //calculate fragment position in screen space
    vec2 fragmentScreenCoords = vec2(gl_FragCoord.x / u_viewportWidth, gl_FragCoord.y / u_viewportHeight);

    fragmentScreenCoords.x *= aspectRatio;

    bool hasBody = false;
    for (int i=0; i < 490; i += 7) {
        if (u_celestialBodiesParams[i + 6] > 0.0) {
            vec2 bodyPos = vec2(u_celestialBodiesParams[i + 0], u_celestialBodiesParams[i + 1]);
            bodyPos.x *= aspectRatio;

            //check if it is in the radius of the sun
            if (length(fragmentScreenCoords - bodyPos) < u_celestialBodiesParams[i + 6]) {
                hasBody = true;
                gl_FragColor = vec4(
                    u_celestialBodiesParams[i + 2],
                    u_celestialBodiesParams[i + 3],
                    u_celestialBodiesParams[i + 4],
                    u_celestialBodiesParams[i + 5]);
                break;
            }
        }
    }

    if (!hasBody) {
        discard;
    }
}