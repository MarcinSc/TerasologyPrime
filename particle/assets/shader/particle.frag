#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_diffuseTexture;

varying vec2 v_texCoord0;
varying float v_exists;
varying vec4 v_color;

void main()
{
    if (v_exists > 0.0) {
        vec4 finalColor = texture2D(u_diffuseTexture, v_texCoord0);
        gl_FragColor = v_color * finalColor;
    } else {
        discard;
    }
}
