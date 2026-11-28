#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
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

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 svgPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vec2 vbMin = ViewBox.xy - vec2(SvgParams.x);
    vec2 vbSize = ViewBox.zw + vec2(SvgParams.x) * 2.0;
    svgPos = vbMin + UV0 * vbSize;
}
