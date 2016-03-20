#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_depthMap;
uniform sampler2D u_diffuseTexture;
uniform float u_ambientLighting;
uniform vec3 u_fogColor;
uniform int u_noDirectionalLight;
uniform int u_shadowMapSize;

// Fragment position in the light coordinates
varying vec4 v_positionLightTrans;
// Texture coordinate
varying vec2 v_texCoord0;
varying vec3 v_normal;
varying vec3 v_position;
varying float v_visibility;
varying float v_distanceToLight;
varying float v_lightingComponent;

const int pcfCount = 1;
const int totalTexelsFromShadowMap = (pcfCount * 2 + 1) * (pcfCount * 2 + 1);

float unpackDistance(vec4 distanceVector) {
    return distanceVector.r * 65025.0
               + distanceVector.g * 255.0
               + distanceVector.b
               + distanceVector.a / 255.0;
}

float getPercentageInLight() {
    if (u_noDirectionalLight == 1) {
        return 0.0;
    } else {
        float texelSize = 1.0 / float(u_shadowMapSize);
        float totalInLight = 0.0;

        vec2 depthLocation = (v_positionLightTrans.xy / v_positionLightTrans.w) * 0.5 + 0.5;

        float bias = 0.5;
        //float bias = clamp(tan(acos(cosTheta)), 0.05, 1.0);
        for (int x = -pcfCount; x <= pcfCount; x++) {
            for (int y = -pcfCount; y <= pcfCount; y++) {
                vec4 distanceFromDepthMap = texture2D(u_depthMap,
                    depthLocation + vec2(float(x), float(y)) * texelSize);
                float lightTravelingDistance = unpackDistance(distanceFromDepthMap);

                if (lightTravelingDistance >= v_distanceToLight - bias) {
                    totalInLight += 1.0;
                }
            }
        }
        return totalInLight / float(totalTexelsFromShadowMap);
    }
}

void main()
{
    vec4 finalColor = texture2D(u_diffuseTexture, v_texCoord0);

    float percentageInLight = getPercentageInLight();
    if (percentageInLight == 0.0) {
        // Not lighted by directional light (star)
        finalColor.rgb *= u_ambientLighting;
    } else {
        // Lighted by the directional light (star)
        finalColor.rgb *= clamp(u_ambientLighting + percentageInLight * v_lightingComponent, 0.0, 1.0);
    }

    // Objects far into the distance (close to far plane) are fading into background
    finalColor.rgb = mix(finalColor.rgb, u_fogColor, 1.0 - v_visibility);

    gl_FragColor = finalColor;
}
