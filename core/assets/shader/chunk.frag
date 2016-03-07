#ifdef GL_ES
precision mediump float;
#endif

uniform float u_cameraFar;
uniform vec3 u_lightPosition;
uniform sampler2D u_depthMap;
uniform sampler2D u_diffuseTexture;

// Fragment position in the light coordinates
varying vec4 v_positionLightTrans;
// Fragment position in the world coordinates
varying vec4 v_position;
// Texture coordinate
varying vec2 v_texCoord0;

void main()
{
    vec4 finalColor = texture2D(u_diffuseTexture, v_texCoord0);
    vec3 depth = (v_positionLightTrans.xyz / v_positionLightTrans.w) * 0.5 + 0.5;
    float lenFB = texture2D(u_depthMap, depth.xy).a;
    float lenLight = length(v_position.xyz - u_lightPosition) / u_cameraFar;
    if  (lenFB < lenLight - 0.1)
        finalColor.rgb *= 0.4;
    gl_FragColor = finalColor;
}
