#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_diffuseTexture;
uniform vec3 u_fogColor;

varying vec2 v_texCoord0;
varying float v_exists;
varying vec4 v_color;
varying float v_visibility;

void main()
{
    if (v_exists > 0.0) {
        vec4 finalColor = texture2D(u_diffuseTexture, v_texCoord0);
        finalColor *= v_color;
        // Objects far into the distance (close to far plane) are fading into background
        finalColor.rgb = mix(finalColor.rgb, u_fogColor, 1.0 - v_visibility);
        gl_FragColor = finalColor;
    } else {
        discard;
    }
}
