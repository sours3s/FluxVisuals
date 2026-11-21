#version 150

layout(std140) uniform FluxGlassData {
    mat4 uProjection;
    vec4 uRect;
    vec4 uScreen;
    vec4 uParams;
    vec4 uTint;
    vec4 uState;
};

in vec2 texCoord;
in vec2 localPos;
in vec2 rectSize;

out vec4 fragColor;

uniform sampler2D Sampler0;

float roundedBoxSDF(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// Smooth value noise for the animated liquid flow.
float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Two octaves of flowing noise, animated by time, returning a signed warp vector.
vec2 liquidFlow(vec2 uv, float t) {
    vec2 p = uv * 3.4;
    float nx = vnoise(p + vec2(t * 0.16, t * 0.11))
             + 0.5 * vnoise(p * 2.1 + vec2(-t * 0.23, t * 0.19));
    float ny = vnoise(p.yx + vec2(-t * 0.13, t * 0.17) + 7.3)
             + 0.5 * vnoise(p.yx * 2.1 + vec2(t * 0.21, -t * 0.27) + 3.1);
    return vec2(nx, ny) / 1.5 - 0.5;
}

const int TAPS = 64;

vec3 diskBlur(vec2 uv, vec2 texel, float radius) {
    float rotation = hash12(gl_FragCoord.xy) * 6.2831853;
    vec3 sum = vec3(0.0);
    float weightSum = 0.0;
    for (int i = 0; i < TAPS; i++) {
        float t = (float(i) + 0.5) / float(TAPS);
        float r = sqrt(t);
        float phi = float(i) * 2.39996323 + rotation;
        vec2 offset = vec2(cos(phi), sin(phi)) * (r * radius) * texel;
        float weight = exp(-2.2 * r * r);
        sum += texture(Sampler0, clamp(uv + offset, vec2(0.0), vec2(1.0))).rgb * weight;
        weightSum += weight;
    }
    return sum / max(weightSum, 0.0001);
}

vec3 glassBlur(vec2 uv, vec2 texel, float radius) {
    const int GLASS_TAPS = 16;
    vec3 sum = texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb * 1.35;
    float weightSum = 1.35;
    for (int i = 0; i < GLASS_TAPS; i++) {
        float t = (float(i) + 0.5) / float(GLASS_TAPS);
        float r = sqrt(t);
        float phi = float(i) * 2.39996323;
        float weight = 1.0 - r * 0.42;
        vec2 offset = vec2(cos(phi), sin(phi)) * r * radius * texel;
        sum += texture(Sampler0, clamp(uv + offset, vec2(0.0), vec2(1.0))).rgb * weight;
        weightSum += weight;
    }
    return sum / max(weightSum, 0.0001);
}

void main() {
    vec2 halfSize = rectSize * 0.5;
    vec2 centerPos = localPos - halfSize;
    float radius = min(uParams.x, min(halfSize.x, halfSize.y));
    float dist = roundedBoxSDF(centerPos, halfSize, radius);
    float smoothing = max(fwidth(dist) * 1.15, 1.15);
    float shapeAlpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (shapeAlpha < 0.01) {
        discard;
    }

    vec2 uv = localPos / rectSize;
    vec2 centered = uv - 0.5;
    float edgeDistance = abs(dist);
    float rim = 1.0 - smoothstep(0.0, max(2.0, radius * 0.45), edgeDistance);

    float t = uState.z;
    vec2 edgeNormal = normalize(centered + vec2(0.0001));
    vec2 bend = edgeNormal * (0.05 + rim * 0.90);

    float distortionPx = uParams.z * (0.08 + rim * 1.18);
    vec2 distortion = bend * distortionPx / uScreen.xy;
    distortion.y = -distortion.y;

    // Living liquid: slow domain-warped refraction flowing across the whole face.
    vec2 flow = liquidFlow(uv, t);
    float flowPx = uParams.z * (1.5 + rim * 1.0);
    vec2 flowDistort = flow * flowPx / uScreen.xy;
    flowDistort.y = -flowDistort.y;
    distortion += flowDistort;

    float blurPx = max(0.0, uParams.y);
    vec2 texel = vec2(1.0 / uScreen.x, 1.0 / uScreen.y);
    vec2 sampleUv = texCoord + distortion;

    vec3 color;
    if (uState.w > 0.5) {
        color = diskBlur(sampleUv, texel, blurPx);
    } else {
        color = glassBlur(sampleUv, texel, blurPx);
    }

    // Mode 1 is a plain strong backdrop blur. Mode 2 keeps the liquid-glass
    // tint/highlights, but uses the same dense disk blur as the main style.
    if (uState.w > 0.5 && uState.w < 1.5) {
        float inside = 1.0 - smoothstep(-1.0, 0.0, dist);
        fragColor = vec4(color, inside * clamp(uState.x, 0.0, 1.0));
        return;
    }


    // Subtle chromatic aberration only right at the rim — keeps the frosted face clean.
    float caAmt = rim * 0.55;
    vec2 caDir = normalize(centered + vec2(0.0001));
    color.r = mix(color.r, texture(Sampler0, clamp(sampleUv + caDir * caAmt * texel, vec2(0.0), vec2(1.0))).r, 0.35);
    color.b = mix(color.b, texture(Sampler0, clamp(sampleUv - caDir * caAmt * texel, vec2(0.0), vec2(1.0))).b, 0.35);

    // Frosted glass body: gentle top-down light, a touch brighter/more translucent.
    float topLight = pow(max(0.0, 1.0 - uv.y), 2.0) * 0.10;
    color += vec3(topLight * 0.96, topLight * 0.98, topLight);
    color *= mix(1.13, 0.94, uv.y);

    // Liquid caustic shimmer: soft brightness that drifts with the flow field.
    float caustic = liquidFlow(uv * 1.3 + 2.0, t * 1.15).x;
    caustic = smoothstep(0.15, 0.55, caustic);
    color += vec3(0.85, 0.90, 1.0) * caustic * 0.05;

    float bevel = 1.0 - smoothstep(0.0, max(4.0, radius * 0.72), edgeDistance);
    float lightFacing = clamp(0.72 - edgeNormal.y * 0.58 - edgeNormal.x * 0.12, 0.0, 1.0);
    // Glossy sheen along the top edge for a real glass highlight.
    float sheen = (1.0 - smoothstep(0.0, 0.30, uv.y)) * rim;
    color += vec3(0.92, 0.95, 1.0) * sheen * 0.13;

    // Slow specular sweep travelling diagonally across the panel — the "living" glass streak.
    float sweepPhase = (uv.x * 0.85 + uv.y * 0.55) * 3.14159 - t * 0.62;
    float sweep = smoothstep(0.86, 1.0, sin(sweepPhase));
    color += vec3(0.98, 0.99, 1.0) * sweep * 0.072 * (0.45 + 0.55 * (1.0 - uv.y));

    color += mix(vec3(0.74, 0.84, 0.96), uTint.rgb, 0.32)
            * bevel * bevel * lightFacing * 0.13 * uParams.w;

    vec3 tint = uTint.rgb;
    color = mix(color, tint, clamp(uTint.a, 0.0, 1.0));


    // Thin SDF border: it follows the exact same rounded mask as the glass body,
    // so scaled ClickGui corners stay antialiased instead of exposing mesh pixels.
    float edge = abs(dist);
    float line = 1.0 - smoothstep(0.0, 1.35, edge);
    float glow = 1.0 - smoothstep(0.0, 18.0, edge);
    float pulse = 0.94 + 0.06 * sin(uState.z * 0.75);
    vec3 rimColor = mix(vec3(0.97, 0.98, 1.0), uTint.rgb, 0.42);
    color = mix(color, rimColor, line * 0.62 * uParams.w);
    color += rimColor * (line * 0.16 + glow * glow * 0.16) * pulse * uParams.w;

    float alpha = shapeAlpha * clamp(uState.x, 0.0, 1.0);
    fragColor = vec4(color, alpha);
}
