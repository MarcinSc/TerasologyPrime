#ifdef GL_ES
precision mediump float;
#endif

uniform float u_cameraFar;
uniform vec3 u_lightPosition;
uniform vec3 u_lightDirection;
uniform float u_lightPlaneDistance;
uniform sampler2D u_depthMap;
uniform sampler2D u_diffuseTexture;
uniform float u_ambientLighting;

// Fragment position in the light coordinates
varying vec4 v_positionLightTrans;
// Texture coordinate
varying vec2 v_texCoord0;
varying vec3 v_normal;
varying vec3 v_position;

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
    float distanceToLight = dot(v_position.xyz, u_lightDirection) + u_lightPlaneDistance;

    float cosTheta = clamp(dot(v_normal, normalize(u_lightPosition-v_position)), 0.0, 1.0);

    float bias = 0.5;
    //float bias = clamp(tan(acos(cosTheta)), 0.05, 1.0);
    if (lightTravelingDistance < distanceToLight - bias) {
        // Not lighted by directional lighting (star)
        finalColor.rgb *= u_ambientLighting;
    } else {
        finalColor.rgb *= clamp(u_ambientLighting + cosTheta, 0.0, 1.0);
    }

    // Objects far into the distance (close to far plane) are fading into background
    float mistDistance = 0.9995;
    if (gl_FragCoord.z > mistDistance) {
        float multiplier = (gl_FragCoord.z-mistDistance)/(1.0-mistDistance);

        finalColor = vec4(
            mix(finalColor.rgb, vec3(0.0), multiplier), finalColor.a);
    }

    gl_FragColor = finalColor;
}
