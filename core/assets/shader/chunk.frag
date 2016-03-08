#ifdef GL_ES
precision mediump float;
#endif

uniform float u_cameraFar;
uniform vec3 u_lightPosition;
uniform vec3 u_lightDirection;
uniform float u_lightPlaneDistance;
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
    vec4 distanceFromDepthMap = texture2D(u_depthMap, depth.xy);
    float distanceFromShadowMap =
        distanceFromDepthMap.r*255.0*255.0
        +distanceFromDepthMap.g*255.0
        +distanceFromDepthMap.b
        +distanceFromDepthMap.a/255.0;

    float distanceToLight = dot(v_position.xyz, u_lightDirection) + u_lightPlaneDistance;

    vec3 lightToPointVector = normalize(u_lightPosition-v_position);
    float cosTheta = clamp(dot(v_normal, lightToPointVector), 0.0, 1.0);

    float bias = clamp(0.005*tan(acos(cosTheta)), 0.001, 0.01);
    if (distanceFromShadowMap < distanceToLight - bias) {
        // Not lighted by directional lighting (star)
        finalColor.rgb *= ambientLighting;
    } else {
        float totalLighting = clamp(ambientLighting+cosTheta, 0.0, 1.0);
        finalColor.rgb *= totalLighting;
    }

    vec3 mistColor=vec3(0.0);
    float mistDistance = 0.9995;
    if (gl_FragCoord.z > mistDistance) {
        float multiplier = (gl_FragCoord.z-mistDistance)/(1.0-mistDistance);

        finalColor = vec4(
            mix(finalColor.rgb, mistColor, multiplier), finalColor.a);
    }

    gl_FragColor = finalColor;
}
