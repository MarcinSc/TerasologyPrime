uniform float u_cameraFar;
uniform vec3 u_lightPosition;

varying vec4 v_position;

void main()
{
    // Distance from the point to light position in relation to camera max distance
    gl_FragColor = vec4(length(v_position.xyz - u_lightPosition) / u_cameraFar);
}