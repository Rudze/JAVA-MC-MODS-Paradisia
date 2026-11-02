#version 330
#extension GL_ARB_separate_shader_objects : require

#ifdef GLINT
#include <minecraft:globals.glsl>
#endif
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

#ifdef GLINT
uniform sampler2D GlintSampler;
#endif

#ifndef OIT_ALPHA_ONLY
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#endif
layout(location = 2) in vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) in vec4 lightMapColor;
layout(location = 4) in vec4 overlayColor;
#endif
layout(location = 5) in vec2 texCoord0;
#ifdef GLINT
layout(location = 6) in vec2 texCoordGlint;
#endif

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

#ifndef OIT_ALPHA_ONLY
vec4 calculateFinalColor(vec4 color, int alphaValue) {
    vec4 lightColor = vec4(vec3(lightMapColor.a), 1.0);

    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);

    switch (alphaValue) {

        // Emissive texture with shading
        case 253:
            color.rgb *= lightColor.rgb;
            break;

        // Emissive texture without shading
        case 252:
            break;

        // Texture with subtle shading
        case 251:
            color.rgb *= mix(lightMapColor.rgb, lightColor.rgb * lightMapColor.rgb, 0.35);
            break;

        // Texture without shading
        case 250:
            color.rgb *= lightMapColor.rgb;
            break;

        // Translucent texture without shading
        case 130:
            color.rgb *= lightMapColor.rgb;
            break;

        // Vanilla texture
        default:
            color *= lightColor * vec4(lightMapColor.rgb, 1.0);
            break;

    }

    #ifdef GLINT
    vec4 glintColor = GlintAlpha * texture(GlintSampler, texCoordGlint);// Glint color modulator?
    // Matches BlendFuntion.GLINT
    color.rgb += glintColor.rgb * glintColor.rgb;
    #endif

    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif

    return apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
}
#endif

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif

    int alphaValue = int(round(color.a * 255.0));
    if (alphaValue >= 250 && alphaValue <= 253) {
        color.a = 1.0;
    }

    color *= vertexColor * ColorModulator;

    #ifdef GLINT
    color.a = max(color.a, GlintAlpha);
    #endif

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color, alphaValue);
    #endif
}
