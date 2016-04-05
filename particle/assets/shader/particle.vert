attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_projViewTrans;
uniform float u_viewportWidth;
uniform float u_viewportHeight;

varying float v_exists;
varying vec2 v_texCoord0;

void main() {
    v_texCoord0 = a_texCoord0;

    float rotation = a_normal.x;
    float scale = a_normal.y;
    float corner = a_normal.z;

    vec4 position = vec4(a_position, 1.0);
    vec4 particleScreenCoords = u_projViewTrans * position;

    if (corner == 0.0) {
        v_exists = 0.0;
        gl_Position = particleScreenCoords;
    } else {
        v_exists = 1.0;
        float perspective_factor = particleScreenCoords.z * 0.5 + 1.0;
        vec2 diff;
        if (corner == 1.0) {
            diff = vec2(-0.5, -0.5);
        } else if (corner == 2.0) {
            diff = vec2(0.5, -0.5);
        } else if (corner == 3.0) {
            diff = vec2(0.5, 0.5);
        } else {
            diff = vec2(-0.5, 0.5);
        }
        diff *= scale;

        diff = vec2(diff.x * cos(rotation) - diff.y * sin(rotation),
                    diff.x * sin(rotation) + diff.y * cos(rotation));

        gl_Position = particleScreenCoords + vec4(diff.x / perspective_factor, diff.y * (u_viewportWidth/u_viewportHeight) / perspective_factor, 0.0, 0.0);
    }
}
