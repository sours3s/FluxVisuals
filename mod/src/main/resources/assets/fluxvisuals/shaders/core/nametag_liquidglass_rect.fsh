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

in vec2 texCoord0;
out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    float rTL = r.x, rTR = r.y, rBR = r.z, rBL = r.w;
    float rRight = (p.y < 0.0) ? rTR : rBR;
    float rLeft = (p.y < 0.0) ? rTL : rBL;
    float rad = (p.x > 0.0) ? rRight : rLeft;
    vec2 q = abs(p) - b + vec2(rad);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
}

float shapeSDF(vec2 p, vec2 halfSize) {
    if (Counts.y > 0) {
        vec2 c0 = BoxCenterA.xy - halfSize;
        float dHeader = sdRoundBox(p - c0, BoxHalfSizeA.xy, BoxRadii0);
        if (Counts.y <= 1) return dHeader;

        vec2 c1 = BoxCenterA.zw - halfSize;
        float dContent = sdRoundBox(p - c1, BoxHalfSizeA.zw, BoxRadii1);

        float k = (p.x < c0.x) ? FilletRadius.x : FilletRadius.y;
        if (k > 0.001) {
            float h = clamp(0.5 + 0.5 * (dContent - dHeader) / k, 0.0, 1.0);
            return mix(dContent, dHeader, h) - k * h * (1.0 - h);
        }
        return min(dHeader, dContent);
    }
    return sdRoundBox(p, halfSize, CornerRadius);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void main() {
    vec2 pixel = texCoord0 * RectSize.zw - RectContent.xy;
    vec2 center = RectSize.xy * 0.5;
    vec2 p = pixel - center;

    float d = shapeSDF(p, center);
    float aa = max(fwidth(d), 0.5);
    float fillMask = 1.0 - smoothstep(-aa, aa, d);
    if (fillMask <= 0.001) discard;

    float borderMask = 0.0;
    if (RectContent.z > 0.0) {
        float innerFill = 1.0 - smoothstep(-aa, aa, d + RectContent.z);
        borderMask = clamp(fillMask - innerFill, 0.0, 1.0);
    }

    float k = clamp(GlassParams1.x, 0.0, 1.0);

    vec2 screenUV = gl_FragCoord.xy / max(ResolutionParams.xy, vec2(1.0));
    vec2 m2 = p / max(RectSize.x, RectSize.y);
    float depth = clamp(1.0 - abs(d) / max(min(RectSize.x, RectSize.y) * 0.5, 1.0), 0.0, 1.0);
    vec2 refractedUV = screenUV - m2 * (GlassParams0.y * k * 0.045 * depth);

    if (GlassParams2.x > 0.0001) {
        float wt = GlassParams3.y * GlassParams2.y;
        float freq = mix(2.0, 12.0, clamp(GlassParams2.z, 0.0, 1.0));
        vec2 norm = p / max(min(RectSize.x, RectSize.y), 1.0);
        float wx = sin(norm.y * freq + wt) * 0.6
                 + sin(norm.y * freq * 2.2 - wt * 1.4 + norm.x * 1.7) * 0.3
                 + sin(norm.x * freq * 0.7 + wt * 0.5) * 0.2;
        float wy = cos(norm.x * freq * 1.1 + wt * 1.2) * 0.6
                 + cos(norm.x * freq * 2.5 - wt * 0.9 + norm.y * 1.7) * 0.3
                 + cos(norm.y * freq * 0.7 - wt * 0.5) * 0.2;
        refractedUV += vec2(wx, wy) * GlassParams2.x * 0.012;
    }

    bool chromatic = GlassParams0.z > 0.0001;
    vec2 baseR = refractedUV;
    vec2 baseG = refractedUV;
    vec2 baseB = refractedUV;
    if (chromatic) {
        vec2 caOffset = m2 * (GlassParams0.z * k * 0.0035);
        baseR += caOffset;
        baseB -= caOffset;
    }

    if (GlassParams1.w > 0.0001) {
        float pixSize = mix(2.0, 32.0, clamp(GlassParams1.w, 0.0, 1.0));
        vec2 grid = pixSize / max(ResolutionParams.xy, vec2(1.0));
        baseG = (floor(baseG / grid) + 0.5) * grid;
        if (chromatic) {
            baseR = (floor(baseR / grid) + 0.5) * grid;
            baseB = (floor(baseB / grid) + 0.5) * grid;
        } else {
            baseR = baseG;
            baseB = baseG;
        }
    }

    if (GlassParams1.y > 0.0001) {
        float h1 = hash21(gl_FragCoord.xy + 0.5);
        float h2 = hash21(gl_FragCoord.xy + vec2(17.31, 7.71));
        vec2 jitter = (vec2(h1, h2) - 0.5) * GlassParams1.y * 6.0 / max(ResolutionParams.xy, vec2(1.0));
        baseG += jitter;
        if (chromatic) {
            baseR += jitter;
            baseB += jitter;
        } else {
            baseR = baseG;
            baseB = baseG;
        }
    }

    vec3 col;
    float blurPx = GlassParams0.w * k * 8.0;
    float lod = (blurPx > 0.0001) ? clamp(log2(blurPx * 2.0), 0.0, 4.0) : 0.0;
    if (chromatic) {
        col.r = textureLod(Sampler0, baseR, lod).r;
        col.g = textureLod(Sampler0, baseG, lod).g;
        col.b = textureLod(Sampler0, baseB, lod).b;
    } else {
        col = textureLod(Sampler0, baseG, lod).rgb;
    }

    if (GlassParams1.z > 0.0001) {
        float steps = mix(16.0, 3.0, clamp(GlassParams1.z, 0.0, 1.0));
        col = floor(col * steps + 0.5) / steps;
    }

    col = mix(col, FillColor.rgb, FillColor.a * 0.5);
    col = mix(col, GlassTint.rgb, GlassTint.a * GlassParams0.x * k);

    if (GlassParams2.w > 0.0001) {
        float maxDepth = mix(4.0, min(RectSize.x, RectSize.y) * 0.5, clamp(GlassParams3.x, 0.0, 1.0));
        float depthFromEdge = max(-d, 0.0);
        float gt = clamp(depthFromEdge / max(maxDepth, 0.5), 0.0, 1.0);
        float glow = pow(1.0 - gt, 2.5);
        vec3 glowRgb = GlassGlowColor.rgb * (glow * GlassParams2.w * GlassGlowColor.a);
        col = 1.0 - (1.0 - col) * (1.0 - clamp(glowRgb, 0.0, 1.0));
    }

    if (borderMask > 0.0) {
        col = mix(col, BorderColor.rgb, BorderColor.a * borderMask);
    }

    fragColor = vec4(col, fillMask * RectContent.w);
}
