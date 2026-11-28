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

in vec2 svgPos;

out vec4 fragColor;

float distToSeg(vec2 p, vec2 a, vec2 b) {
    vec2 ba = b - a;
    vec2 pa = p - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-8), 0.0, 1.0);
    return length(pa - ba * h);
}

bool insideAt(vec2 p, int count) {
    float w = 0.0;
    for (int i = 0; i < count; i++) {
        vec4 s = SvgSegments[i];
        vec2 a = s.xy;
        vec2 b = s.zw;
        if (a.y <= p.y) {
            if (b.y > p.y) {
                float t = (p.y - a.y) / (b.y - a.y);
                if (a.x + t * (b.x - a.x) > p.x) w += 1.0;
            }
        } else if (b.y <= p.y) {
            float t = (p.y - a.y) / (b.y - a.y);
            if (a.x + t * (b.x - a.x) > p.x) w -= 1.0;
        }
    }
    return abs(w) > 0.5;
}

void main() {
    int count = int(SvgParams.w + 0.5);
    if (count <= 0) discard;

    float minDist = 1e9;
    for (int i = 0; i < count; i++) {
        vec4 s = SvgSegments[i];
        minDist = min(minDist, distToSeg(svgPos, s.xy, s.zw));
    }

    vec2 fw = fwidth(svgPos);
    float aa = max(length(fw) * 0.70710678, 1e-5);

    float alpha;
    if (SvgParams.z > 0.5) {
        float sd = insideAt(svgPos, count) ? -minDist : minDist;
        alpha = 1.0 - smoothstep(-aa, aa, sd);
    } else {
        float halfStroke = SvgParams.y * 0.5;
        alpha = 1.0 - smoothstep(halfStroke - aa, halfStroke + aa, minDist);
    }

    alpha *= SvgColor.a * RectContent.w;
    if (alpha < 0.002) discard;
    fragColor = vec4(SvgColor.rgb, alpha);
}
