#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightColor;
in vec4 lightMapColor;
in vec4 flatLightingModel;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    
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
    
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}