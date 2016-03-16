#ifdef GL_ES
precision mediump float;
#endif

uniform vec3 u_skyColor;

void main() {
    gl_FragColor = vec4(u_skyColor, 1.0);
}
