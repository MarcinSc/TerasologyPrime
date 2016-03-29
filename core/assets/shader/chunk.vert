attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;
attribute float a_flag;

uniform mat4 u_projViewTrans;
uniform mat4 u_lightTrans;
uniform vec3 u_lightDirection;
uniform float u_lightPlaneDistance;
uniform vec3 u_lightPosition;
uniform float u_time;

varying vec4 v_positionLightTrans;
varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoord0;
varying float v_visibility;
varying float v_distanceToLight;
varying float v_lightingComponent;

const float fogDensity = 0.005;
const float fogGradient = 5.0;

float smoothCurve(float x) {
  return x * x * (3.0 - 2.0 * x);
}

float triangleWave(float x) {
  return abs(fract(x + 0.5) * 2.0 - 1.0);
}

float smoothTriangleWave(float x) {
  return smoothCurve(triangleWave(x)) * 2.0 - 1.0;
}

bool checkFlag(int flag, float val) {
    // Older versions of OpenGL did not have the "&" operator, hence this ugliness...
    float flagF = float(flag);

    float v1 = pow(2.0, flagF);
    float v2 = pow(2.0, flagF+1.0);

    return (floor(val/v1) - 2.0 * floor(val/v2)) > 0.0;
}

void main() {
    v_texCoord0 = a_texCoord0;
    v_normal = a_normal;
    vec4 position = vec4(a_position, 1.0);

    if (checkFlag(1, a_flag)) {
        position.x += (smoothTriangleWave(u_time * 0.1 + position.x * 0.01 + position.z * 0.01) * 2.0 - 1.0) * 0.03;
        position.y += (smoothTriangleWave(u_time * 0.2 + position.x * -0.01 + position.z * -0.01) * 2.0 - 1.0) * 0.05;
        position.z += (smoothTriangleWave(u_time * 0.1 + position.x * -0.01 + position.z * -0.01) * 2.0 - 1.0) * 0.03;
    }

    v_distanceToLight = dot(position.xyz, u_lightDirection) + u_lightPlaneDistance;
    v_lightingComponent = clamp(dot(a_normal, normalize(u_lightPosition-position.xyz)), 0.0, 1.0);

    v_positionLightTrans = u_lightTrans * position;
    v_position = position.xyz;

    vec4 positionRelativeToCamera = u_projViewTrans * position;

    float distanceFromCamera = length(positionRelativeToCamera.xyz);
    v_visibility = clamp(exp(-pow((distanceFromCamera * fogDensity), fogGradient)), 0.0, 1.0);

    gl_Position = positionRelativeToCamera;
}
