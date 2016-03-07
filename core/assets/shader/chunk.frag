#ifdef GL_ES
precision mediump float;
#endif

uniform vec3 u_lightPosition;
uniform sampler2D u_depthMap;
uniform sampler2D u_diffuseTexture;

// Fragment position in the light coordinates
varying vec4 v_positionLightTrans;
// Texture coordinate
varying vec2 v_texCoord0;
varying vec3 v_normal;
varying vec3 v_position;

void main()
{
    float ambientLighting = 0.2;

    vec4 finalColor = texture2D(u_diffuseTexture, v_texCoord0);
    vec3 depth = (v_positionLightTrans.xyz / v_positionLightTrans.w) * 0.5 + 0.5;
    float lenFB = texture2D(u_depthMap, depth.xy).a;
    float bias = 0.1;
    if  (lenFB < depth.z - bias) {
        // Not lighted by directional lighting (star)
        finalColor.rgb *= ambientLighting;
    } else {
        vec3 lightToPointVector = normalize(u_lightPosition-v_position);
        float diffuseMultiplier = clamp(max(dot(v_normal, lightToPointVector), 0.0), 0.0, 1.0);
        float totalLighting = (ambientLighting+(diffuseMultiplier*(1.0-ambientLighting)));
        finalColor.rgb *= totalLighting;
    }

    gl_FragColor = finalColor;
}
