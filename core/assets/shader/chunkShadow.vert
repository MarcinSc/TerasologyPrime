attribute vec3 a_position;
attribute float a_flag;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;
uniform float u_time;

varying vec4 v_position;

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

void main()
{
    v_position = u_worldTrans * vec4(a_position, 1.0);

    if (checkFlag(0, a_flag)) {
        v_position.x += (smoothTriangleWave(u_time * 0.1 + v_position.x * 0.01 + v_position.z * 0.01) * 2.0 - 1.0) * 0.03;
        v_position.y += (smoothTriangleWave(u_time * 0.2 + v_position.x * -0.01 + v_position.z * -0.01) * 2.0 - 1.0) * 0.05;
        v_position.z += (smoothTriangleWave(u_time * 0.1 + v_position.x * -0.01 + v_position.z * -0.01) * 2.0 - 1.0) * 0.03;
    }

    gl_Position = u_projViewTrans * v_position;
}
