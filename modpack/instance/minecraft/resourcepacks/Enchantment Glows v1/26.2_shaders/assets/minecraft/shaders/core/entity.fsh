#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

#ifdef DISSOLVE
uniform sampler2D DissolveMaskSampler;
#endif

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
#else
in vec4 vertexColor;
#endif

in vec4 lightColor;
in vec4 lightMapColor;
in vec4 flatLightingModel;

#ifndef NO_OVERLAY
in vec4 overlayColor;
#endif

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

#ifdef PER_FACE_LIGHTING
    vec4 faceVertexColor = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
#else
    vec4 faceVertexColor = vertexColor;
#endif

#ifdef DISSOLVE
    if (faceVertexColor.a < texture(DissolveMaskSampler, texCoord0).a) {
        discard;
    }
    // The dissolve effect entirely replaces translucency
    faceVertexColor.a = 1.0;
#endif

    color *= faceVertexColor * ColorModulator;

#ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
#endif

#ifndef EMISSIVE
    int alphaValue = int(round(color.a * 255.0));

    switch (alphaValue) {

        // Emissive texture with shading
        case 253:
            color.a = 1.0;
            color *= lightColor;
            break;

        // Emissive texture without shading
        case 252:
            color.a = 1.0;
            break;

        // Texture with subtle shading
        case 251:
            color.a = 1.0;
            color.rgb *= mix(flatLightingModel.rgb, (lightColor * lightMapColor).rgb, 0.35);
            break;

        // Texture without shading
        case 250:
            color.a = 1.0;
            color.rgb *= flatLightingModel.rgb;
            break;

        // Translucent texture without shading
        case 130:
            color.rgb *= flatLightingModel.rgb;
            break;

        // Vanilla texture
        default:
            color *= lightColor * lightMapColor;
            break;
        
    }
    
#else
    color *= lightColor;
#endif

    fragColor = apply_fog(
        color,
        sphericalVertexDistance,
        cylindricalVertexDistance,
        FogEnvironmentalStart,
        FogEnvironmentalEnd,
        FogRenderDistanceStart,
        FogRenderDistanceEnd,
        FogColor);
}
