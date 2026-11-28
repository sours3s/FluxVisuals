#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform NameTagData {
    vec4 FontParams;
    vec4 BlurParams;
    vec4 RectSize;
    vec4 RectContent;
    vec4 CornerRadius;
    vec4 FillColor;
    vec4 BorderColor;
    vec4 ShadowColor;
    vec4 ShadowParams;
    vec4 SegCentersA;
    vec4 SegCentersB;
    vec4 SegHalfWidthsA;
    vec4 SegHalfWidthsB;
    ivec4 Counts;
    vec4 BoxCenterA;
    vec4 BoxHalfSizeA;
    vec4 BoxRadii0;
    vec4 BoxRadii1;
    vec4 FilletRadius;
    vec4 TextureParams;
    vec4 TextureSmooth;
    vec4 ViewBox;
    vec4 SvgParams;
    vec4 SvgColor;
    vec4 GlassParams0;
    vec4 GlassParams1;
    vec4 GlassParams2;
    vec4 GlassParams3;
    vec4 GlassTint;
    vec4 GlassGlowColor;
    vec4 ResolutionParams;
    vec4 MiscParams;
    vec4 SvgSegments[256];
};

uniform sampler2D Sampler0;

in vec2 texCoord;
in vec4 textColor;

out vec4 fragColor;

float median(vec3 v) {
    return max(min(v.r, v.g), min(max(v.r, v.g), v.b));
}

float screenPxRange(vec2 uv) {
    vec2 atlasSize = max(FontParams.xy, vec2(1.0));
    vec2 unitRange = vec2(FontParams.z) / atlasSize;
    vec2 screenTexSize = vec2(1.0) / max(fwidth(uv), vec2(1e-6));
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    vec3 msd = texture(Sampler0, texCoord).rgb;
    float sd = median(msd) - 0.5 + FontParams.w;
    float px = screenPxRange(texCoord);
    float a = clamp(px * sd + 0.5, 0.0, 1.0);

    float alpha = textColor.a * a;
    if (alpha < 0.002) discard;

    fragColor = vec4(textColor.rgb * alpha, alpha);
}
