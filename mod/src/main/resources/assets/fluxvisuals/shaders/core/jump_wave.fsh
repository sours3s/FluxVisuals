#version 150




layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 v_uv;
out vec4 fragColor;

#define RING_WIDTH 0.30

// --- ambience-style effects for the "Шейдер" jump-circle mode ---
float jc_hash(vec2 p) { p = fract(p * vec2(123.34, 345.45)); p += dot(p, p + 34.345); return fract(p.x * p.y); }
float jc_noise(vec2 p) {
    vec2 i = floor(p); vec2 f = fract(p); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(jc_hash(i), jc_hash(i + vec2(1.0, 0.0)), f.x),
               mix(jc_hash(i + vec2(0.0, 1.0)), jc_hash(i + vec2(1.0, 1.0)), f.x), f.y);
}
float jc_fbm(vec2 p) { float v = 0.0, a = 0.5; for (int i = 0; i < 5; i++) { v += jc_noise(p) * a; p = p * 2.02 + vec2(8.4, 5.7); a *= 0.5; } return v; }
vec3 jc_hsv(vec3 c) { vec4 k = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0); vec3 p = abs(fract(c.xxx + k.xyz) * 6.0 - k.www); return c.z * mix(k.xxx, clamp(p - k.xxx, 0.0, 1.0), c.y); }

vec3 jc_waves(vec2 uv, float t) {
    float warp = jc_fbm(uv * 0.8 + vec2(t * 0.12, -t * 0.08));
    float w = sin((uv.x + warp * 1.8) * 5.0 + t * 1.7) + sin((uv.y - warp * 1.2) * 6.0 - t * 1.25);
    float crest = pow(clamp(w * 0.25 + 0.5, 0.0, 1.0), 2.2);
    return mix(vec3(0.05, 0.10, 0.20), vec3(0.55, 0.80, 1.0), crest);
}
vec3 jc_water(vec2 uv, float t) {
    float n = jc_fbm(uv * 1.35 + vec2(t * 0.22, -t * 0.14));
    float rip = sin((uv.x + n) * 8.0 - t * 1.8) * 0.5 + sin((uv.y - n) * 9.0 + t * 1.4) * 0.5;
    float light = rip * 0.25 + 0.5;
    return mix(vec3(0.01, 0.04, 0.12), vec3(0.35, 0.80, 1.0), light);
}
vec3 jc_dark(vec2 uv, float t) {
    float smoke = jc_fbm(uv + vec2(t * 0.18, -t * 0.11));
    return mix(vec3(0.02, 0.0, 0.05), vec3(0.55, 0.12, 0.65), clamp(smoke, 0.0, 1.0));
}
vec3 jc_aurora(vec2 uv, float t) {
    float wave = sin(uv.x * 4.0 + t * 1.4 + jc_fbm(uv * 2.0 + t * 0.2) * 3.0);
    float a = smoothstep(-0.1, 0.95, wave);
    return mix(vec3(0.02, 0.05, 0.10), jc_hsv(vec3(fract(t * 0.05 + uv.x * 0.2), 0.6, 1.0)), a);
}
vec3 jc_nebula(vec2 uv, float t) {
    float c1 = jc_fbm(uv * 2.8 + vec2(t * 0.1, -t * 0.07));
    float c2 = jc_fbm(uv * 1.8 + vec2(-t * 0.08, t * 0.05));
    float neb = smoothstep(0.25, 0.95, c1 * 0.6 + c2 * 0.4);
    return mix(vec3(0.01, 0.01, 0.04), jc_hsv(vec3(fract(uv.x * 0.3 + t * 0.03), 0.65, 1.0)), neb);
}

void main() {
    // Ambience shader mode (TextureMat[0].w >= 1): fill the disc with an animated effect.
    if (TextureMat[0].w > 0.5) {
        vec2 ac = (v_uv - 0.5) * 2.0;
        float ad = length(ac);
        if (ad > 1.0) discard;
        float at = TextureMat[2].x;
        int afx = int(TextureMat[2].y + 0.5);
        vec3 tint = TextureMat[1].rgb;
        float afade = TextureMat[0].z;
        vec2 auv = ac * 1.6;
        vec3 acol;
        if (afx == 1) acol = jc_water(auv, at);
        else if (afx == 2) acol = jc_dark(auv, at);
        else if (afx == 3) acol = jc_aurora(auv, at);
        else if (afx == 4) acol = jc_nebula(auv, at);
        else acol = jc_waves(auv, at);
        acol *= mix(vec3(1.0), tint, 0.5);
        float ainside = smoothstep(1.0, 0.55, ad);
        float arim = smoothstep(0.08, 0.0, abs(ad - 0.9));
        acol += tint * arim * 0.6;
        float aa = max(ainside * 0.75, arim) * afade;
        if (aa <= 0.002) discard;
        fragColor = vec4(acol, aa);
        return;
    }
    vec4 params = TextureMat[0];
    vec4 crestColor = TextureMat[1];
    float ringProgress = params.x;
    float strength = params.y;
    float fade = params.z;

    vec2 centered = (v_uv - 0.5) * 2.0;
    float dist = length(centered);
    if (dist > 1.0) discard;

    float ringRadius = ringProgress;
    float circleOnly = 1.0 - step(0.0001, abs(strength));
    float liquidBandWidth = max(0.035, RING_WIDTH * ringRadius);
    float bandWidth = mix(liquidBandWidth, RING_WIDTH, circleOnly);
    float edge = dist - ringRadius;
    float band = smoothstep(bandWidth, 0.0, abs(edge));
    band *= band;

    float insideEdgeStart = max(0.0, ringRadius - bandWidth * 0.55);
    float inside = 1.0 - smoothstep(insideEdgeStart, max(0.001, ringRadius), dist);
    // Circle is encoded with zero distortion strength. It should draw only the expanding
    // crest, not a faint full-size illuminated disc from the very first frame.
    float coverage = mix(max(inside, band), band, circleOnly);
    if (coverage <= 0.001) discard;

    vec2 dir = dist > 0.0001 ? centered / dist : vec2(0.0);
    vec2 screenSize = vec2(textureSize(Sampler0, 0));
    vec2 screenUv = gl_FragCoord.xy / screenSize;

    float ripple = sin(dist * 38.0 - ringProgress * 26.0) * 0.5 + 0.5;
    float crest = band * mix(0.7, 1.3, ripple);

    float innerDistort = inside * mix(0.55, 1.0, dist) * (1.0 - circleOnly);
    float offsetAmount = strength * fade * 0.05 * (innerDistort + crest * 1.6);

    vec2 baseOffset = dir * offsetAmount;
    vec3 refracted = texture(Sampler0, clamp(screenUv + baseOffset, vec2(0.0), vec2(1.0))).rgb;

    float sheen = inside * fade * 0.06 * (1.0 - circleOnly);
    vec3 crestTint = mix(vec3(1.0), crestColor.rgb, 0.65);
    vec3 color = refracted + vec3(sheen)
        + crestTint * pow(band, 1.35) * fade * 1.05;

    float alpha = coverage * fade;
    fragColor = vec4(color, alpha);
}
