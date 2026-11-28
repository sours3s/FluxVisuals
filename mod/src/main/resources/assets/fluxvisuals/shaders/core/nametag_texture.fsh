#version 330

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

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

out vec4 OutColor;

float rdist(vec2 pos, vec2 size, vec4 radius) {
    radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
    radius.x = (pos.y > 0.0) ? radius.x : radius.y;

    vec2 v = abs(pos) - size + radius.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
}

float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
    vec2 center = size * 0.5;
    float dist = rdist(center - (coord * size), center - 1.0, radius);
    return 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
}

void main() {
    vec4 color = texture(Sampler0, TexCoord) * FragColor;
    color.a *= ralpha(TextureParams.xy, FragCoord, TextureParams.zw, TextureSmooth.x);

    if (color.a < 0.002) discard;

    OutColor = color;
}
