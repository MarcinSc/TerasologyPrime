#ifdef GL_ES
precision mediump float;
#endif

uniform vec3 u_skyColor;

varying float v_angularDistanceFromLight;

void main() {
    if (v_angularDistanceFromLight < 0.002) {
        gl_FragColor = vec4(1.0);
    } else {
        gl_FragColor = vec4(u_skyColor, 1.0);
    }
}
