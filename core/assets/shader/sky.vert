attribute vec3 a_position;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;
uniform vec3 u_lightDirection;

varying float v_angularDistanceFromLight;

void main() {
    vec4 positionInWorldCoords = u_worldTrans * vec4(a_position, 1.0);

    v_angularDistanceFromLight = 1.0 - dot(normalize(u_lightDirection), normalize(a_position.xyz));
    gl_Position = u_projViewTrans * positionInWorldCoords;
}
