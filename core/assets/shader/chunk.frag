#ifdef GL_ES
precision mediump float;
#endif

uniform vec3 u_lightPosition;
uniform sampler2D u_depthMap;
uniform sampler2D u_diffuseTexture;
uniform float u_ambientLighting;
uniform vec3 u_fogColor;

// Fragment position in the light coordinates
varying vec4 v_positionLightTrans;
// Texture coordinate
varying vec2 v_texCoord0;
varying vec3 v_normal;
varying vec3 v_position;
varying float v_visibility;
varying float v_distanceToLight;

float getLightTravelingDistance() {
    vec2 depth = (v_positionLightTrans.xy / v_positionLightTrans.w) * 0.5 + 0.5;
    vec4 distanceFromDepthMap = texture2D(u_depthMap, depth);
    return distanceFromDepthMap.r*65025.0
        +distanceFromDepthMap.g*255.0
        +distanceFromDepthMap.b
        +distanceFromDepthMap.a/255.0;
}

void main()
{
    vec4 finalColor = texture2D(u_diffuseTexture, v_texCoord0);

    float lightTravelingDistance = getLightTravelingDistance();

    float bias = 0.5;
    //float bias = clamp(tan(acos(cosTheta)), 0.05, 1.0);
    if (lightTravelingDistance < v_distanceToLight - bias) {
        // Not lighted by directional light (star)
        finalColor.rgb *= u_ambientLighting;
    } else {
        // Lighted by the directional light (star)
        float cosTheta = clamp(dot(v_normal, normalize(u_lightPosition-v_position)), 0.0, 1.0);
        finalColor.rgb *= clamp(u_ambientLighting + cosTheta, 0.0, 1.0);
    }

    // Objects far into the distance (close to far plane) are fading into background
    finalColor.rgb = mix(finalColor.rgb, u_fogColor, 1.0 - v_visibility);

    gl_FragColor = finalColor;
}
