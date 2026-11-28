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

in vec2 localUv;

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + vec2(r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float roundedBoxSDF4(vec2 p, vec2 halfSize, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    float rr = (p.y < 0.0) ? r.x : r.y;
    vec2 q = abs(p) - halfSize + vec2(rr);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - rr;
}

float selectRadius(vec2 p) {
    if (p.x < 0.0) return p.y < 0.0 ? CornerRadius.x : CornerRadius.w;
    return p.y < 0.0 ? CornerRadius.y : CornerRadius.z;
}

float getSegCenter(int i) {
    return i < 4 ? SegCentersA[i] : SegCentersB[i - 4];
}

float getSegHalfWidth(int i) {
    return i < 4 ? SegHalfWidthsA[i] : SegHalfWidthsB[i - 4];
}

float shapeSDF(vec2 p, vec2 halfSize) {
    if (Counts.y > 0) {
        vec2 c0 = vec2(BoxCenterA.x - halfSize.x, BoxCenterA.y - halfSize.y);
        vec2 hs0 = vec2(BoxHalfSizeA.x, BoxHalfSizeA.y);
        float dHeader = roundedBoxSDF4(p - c0, hs0, BoxRadii0);
        if (Counts.y <= 1) return dHeader;

        vec2 c1 = vec2(BoxCenterA.z - halfSize.x, BoxCenterA.w - halfSize.y);
        vec2 hs1 = vec2(BoxHalfSizeA.z, BoxHalfSizeA.w);
        float dContent = roundedBoxSDF4(p - c1, hs1, BoxRadii1);

        float k = (p.x < c0.x) ? FilletRadius.x : FilletRadius.y;
        if (k > 0.001) {
            float h = clamp(0.5 + 0.5 * (dContent - dHeader) / k, 0.0, 1.0);
            return mix(dContent, dHeader, h) - k * h * (1.0 - h);
        }
        return min(dHeader, dContent);
    }

    if (Counts.x <= 1) {
        return roundedBoxSDF(p, halfSize, selectRadius(p));
    }

    float rSeg = min(MiscParams.x, halfSize.y);
    float d = 1e6;
    for (int i = 0; i < 8; i++) {
        if (i >= Counts.x) break;
        float cx = getSegCenter(i) - halfSize.x;
        float hw = getSegHalfWidth(i);
        vec2 sp = vec2(p.x - cx, p.y);
        d = min(d, roundedBoxSDF(sp, vec2(hw, halfSize.y), min(rSeg, hw)));
    }
    float dOuter = roundedBoxSDF(p, halfSize, selectRadius(p));
    return max(d, dOuter);
}

void main() {
    vec2 size = RectSize.xy;
    vec2 quadSize = RectSize.zw;
    vec2 contentOffset = RectContent.xy;
    float borderWidth = RectContent.z;
    float globalAlpha = RectContent.w;
    vec2 shadowOffset = ShadowParams.xy;
    float shadowBlur = ShadowParams.z;

    vec2 halfSize = size * 0.5;
    vec2 p = localUv * quadSize - contentOffset - halfSize;

    bool hasShadow = ShadowColor.a > 0.0 && (shadowBlur > 0.0 || dot(shadowOffset, shadowOffset) > 0.0);
    bool hasBorder = borderWidth > 0.0;

    float reachX = hasShadow ? abs(shadowOffset.x) + shadowBlur + 1.0 : 1.0;
    float reachY = hasShadow ? abs(shadowOffset.y) + shadowBlur + 1.0 : 1.0;
    if (abs(p.x) > halfSize.x + reachX || abs(p.y) > halfSize.y + reachY) discard;

    float aa = 0.5;
    float dist = shapeSDF(p, halfSize);
    float rectAlpha = 1.0 - smoothstep(-aa, aa, dist);

    float shadowAlpha = 0.0;
    if (hasShadow) {
        float shadowDist = shapeSDF(p - shadowOffset, halfSize);
        shadowAlpha = (1.0 - smoothstep(-aa, shadowBlur + aa, shadowDist)) * ShadowColor.a * (1.0 - rectAlpha);
    }

    float fillMask = rectAlpha;
    float borderMask = 0.0;
    if (hasBorder) {
        float innerAlpha = 1.0 - smoothstep(-aa, aa, dist + borderWidth);
        borderMask = rectAlpha - innerAlpha;
        fillMask = innerAlpha;
    }

    vec3 color = ShadowColor.rgb;
    float alpha = shadowAlpha;

    float fillA = fillMask * FillColor.a;
    if (fillA > 0.0) {
        color = mix(color, FillColor.rgb, fillA / max(alpha + fillA, 0.001));
        alpha = alpha * (1.0 - fillA) + fillA;
    }

    if (hasBorder) {
        float borderA = borderMask * BorderColor.a;
        if (borderA > 0.0) {
            color = mix(color, BorderColor.rgb, borderA / max(alpha + borderA, 0.001));
            alpha = alpha * (1.0 - borderA) + borderA;
        }
    }

    alpha *= globalAlpha;
    if (alpha < 0.002) discard;

    fragColor = vec4(color, alpha);
}
