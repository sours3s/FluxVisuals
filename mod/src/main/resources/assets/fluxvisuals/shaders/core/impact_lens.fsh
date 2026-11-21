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

void main() {
    vec4 params = TextureMat[0];
    vec4 tint = TextureMat[1];
    float progress = params.x;
    float strength = params.y;
    float fade = params.z;
    float spin = params.w;

    vec2 centered = (v_uv - 0.5) * 2.0;
    float dist = length(centered);
    if (dist > 1.0) discard;

    float edge = 1.0 - dist;
    float body = smoothstep(1.0, 0.18, dist);
    float rim = smoothstep(0.22, 0.0, abs(edge));
    rim *= rim;

    float ringRadius = mix(0.08, 0.82, 1.0 - pow(1.0 - progress, 3.0));
    float ringEdge = dist - ringRadius;
    float glassWave = smoothstep(0.24, 0.0, abs(ringEdge));
    glassWave *= glassWave;

    float angle = atan(centered.y, centered.x) + spin;
    float ripple = sin(dist * 34.0 - progress * 22.0 + angle * 3.0) * 0.5 + 0.5;
    float crack = pow(max(0.0, sin(angle * 7.0 + progress * 9.0)), 18.0) * smoothstep(0.15, 0.95, dist);
    float centerBulge = 1.0 - smoothstep(0.0, 0.62, dist);
    centerBulge *= centerBulge;

    vec2 dir = dist > 0.0001 ? centered / dist : vec2(0.0);
    vec2 screenSize = vec2(textureSize(Sampler0, 0));
    vec2 screenUv = gl_FragCoord.xy / screenSize;

    float pulse = 1.0 - progress;
    float waveRipple = sin(dist * 46.0 - progress * 30.0) * 0.5 + 0.5;
    float waveCrest = glassWave * mix(0.85, 1.35, waveRipple);
    float innerWave = smoothstep(0.92, 0.12, dist) * smoothstep(0.0, 0.20, dist);
    float lensPower = body * (0.65 + ripple * 0.35) + centerBulge * 1.15
        + innerWave * 0.65 + waveCrest * 2.65 + rim * 1.7 + crack * 0.9;
    vec2 offset = dir * strength * fade * lensPower * (0.022 + pulse * 0.024);

    vec3 centerSample = texture(Sampler0, clamp(screenUv + offset, vec2(0.0), vec2(1.0))).rgb;
    vec3 redSample = texture(Sampler0, clamp(screenUv + offset * 1.18, vec2(0.0), vec2(1.0))).rgb;
    vec3 blueSample = texture(Sampler0, clamp(screenUv + offset * 0.82, vec2(0.0), vec2(1.0))).rgb;
    vec3 refracted = vec3(redSample.r, centerSample.g, blueSample.b);

    float detailVisibility = clamp(tint.a, 0.0, 1.0);
    float highlight = (rim * 0.72 + waveCrest * 0.62 + crack * 0.38
        + body * 0.055 + centerBulge * 0.06) * fade * detailVisibility;
    vec3 lensColor = refracted + vec3(highlight)
        + tint.rgb * (rim * 0.42 + waveCrest * 0.55 + body * 0.08 + centerBulge * 0.04)
        * fade * detailVisibility;
    float alpha = max(max(max(body * 0.62, centerBulge * 0.32), waveCrest), rim) * fade;

    fragColor = vec4(lensColor, alpha);
}
