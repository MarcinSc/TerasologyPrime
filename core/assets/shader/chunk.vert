attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

uniform mat4 u_lightTrans;

varying vec4 v_positionLightTrans;
varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoord0;

void main() {
    v_texCoord0 = a_texCoord0;
    v_normal = a_normal;
    vec4 position = u_worldTrans * vec4(a_position, 1.0);
    v_positionLightTrans = u_lightTrans * position;
    v_position = position.xyz;
    gl_Position = u_projViewTrans * position;
}
