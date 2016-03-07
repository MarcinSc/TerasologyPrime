uniform float u_cameraFar;
uniform vec3 u_lightPosition;

varying vec4 v_position;

void main()
{
    // Distance from the point to light position
    float distanceToLight = length(v_position.xyz - u_lightPosition);
    // We try to pack as much information as we can into the "color"
    // r holds any multiples of 256, g holds any whole numbers below 256,
    // b holds any remaining multiples of 1/256th
    float r = floor(distanceToLight/256.0);
    float g = floor(distanceToLight-(r*256.0));
    float b = floor(256.0*(distanceToLight-(r*256.0)-g));
    float a = 256.0*(distanceToLight-(r*256.0)-g-(b/256.0));
    gl_FragColor = vec4(r/256.0, g/256.0, b/256.0, a);
}