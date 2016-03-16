uniform vec3 u_lightDirection;
uniform float u_lightPlaneDistance;

varying vec4 v_position;

void main()
{
    // Distance from the point to light plane
    float distanceToLight = dot(v_position.xyz, u_lightDirection) + u_lightPlaneDistance;
    // We try to pack as much information as we can into the "color"
    // r holds any multiples of 255, g holds any whole numbers below 255,
    // b holds any remaining multiples of 1/255th, etc.
    float r = floor(distanceToLight/255.0);
    float g = floor(distanceToLight-(r*255.0));
    float b = floor(255.0*(distanceToLight-(r*255.0)-g));
    float a = 255.0*(distanceToLight-(r*255.0)-g-(b/255.0));
    gl_FragColor = vec4(r/255.0, g/255.0, b/255.0, a);
}