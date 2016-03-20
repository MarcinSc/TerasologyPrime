#ifdef GL_ES
precision mediump float;
#endif

uniform float u_viewportWidth;
uniform float u_viewportHeight;

struct CelestialBody
{
    vec2 positionScreenCoords;
	vec3 color;
	float size;
};
uniform CelestialBody u_celestialBodies[10];

void main() {
    float aspectRatio = u_viewportWidth/u_viewportHeight;

    //calculate fragment position in screen space
    vec2 fragmentScreenCoords = vec2(gl_FragCoord.x / u_viewportWidth, gl_FragCoord.y / u_viewportHeight);

    fragmentScreenCoords.x *= aspectRatio;

    bool hasBody = false;
    for (int i=0; i<1; i++) {
        if (u_celestialBodies[i].size > 0.0) {
            vec2 bodyPos = vec2(u_celestialBodies[i].positionScreenCoords);
            bodyPos.x *= aspectRatio;

            //check if it is in the radius of the sun
            if (length(fragmentScreenCoords-bodyPos) < u_celestialBodies[0].size) {
                hasBody = true;
                gl_FragColor = vec4(u_celestialBodies[i].color, 1.0);
                break;
            }
        }
    }

    if (!hasBody) {
        discard;
    }
}