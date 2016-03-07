#ifdef GL_ES
precision mediump float;
#endif

// Values that stay constant for the whole mesh.
uniform sampler2D u_diffuseTexture;
varying vec2 v_texCoord0;

void main() {
    float colorsAvailable = 255.0;
    vec4 realColor = texture2D(u_diffuseTexture, v_texCoord0);
    float distance = gl_FragCoord.z;
    vec3 mistColor = vec3(1.0, 1.0, 1.0);
    float multiplier = 0.0;

    vec4 pointColor = vec4(
        floor(colorsAvailable * realColor.r + 0.5)/colorsAvailable,
        floor(colorsAvailable * realColor.g + 0.5)/colorsAvailable,
        floor(colorsAvailable * realColor.b + 0.5)/colorsAvailable,
        realColor.a);

    if (distance>0.999) {
        multiplier = (distance-0.999)*1000.0;

        gl_FragColor = vec4(
            mix(pointColor.rgb, mistColor, multiplier), pointColor.a);
    } else {
        gl_FragColor = pointColor;
    }
}
