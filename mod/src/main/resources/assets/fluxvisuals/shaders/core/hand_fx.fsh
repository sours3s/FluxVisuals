#version 150




layout(std140) uniform HandFxData {
    mat4 uProjection;
    vec4 uParams;
    vec4 uColor;
    vec4 uScreen;
    vec4 uEditorRegion;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D HandDepthSampler;
uniform sampler2D BeforeHandDepthSampler;

in vec2 texCoord;
out vec4 fragColor;

float frameDifference(vec2 uv) {
    vec3 afterColor = texture(Sampler0, uv).rgb;
    vec3 beforeColor = texture(Sampler1, uv).rgb;
    return length(afterColor - beforeColor);
}

float rawHandMask(vec2 uv) {
    float colorMask = smoothstep(0.012, 0.077, frameDifference(uv));
    float handDepth = texture(HandDepthSampler, uv).r;
    float depthMask = 1.0 - smoothstep(0.9985, 0.99995, handDepth);
    return max(colorMask, depthMask);
}

float rawEditorMask(vec2 uv) {
    float colorMask = smoothstep(0.012, 0.077, frameDifference(uv));
    float afterDepth = texture(HandDepthSampler, uv).r;
    float beforeDepth = texture(BeforeHandDepthSampler, uv).r;
    float depthDelta = max(beforeDepth - afterDepth, 0.0);
    float depthMask = smoothstep(0.000001, 0.00008, depthDelta);
    return max(colorMask, depthMask);
}

float fastEditorMask(vec2 uv, vec2 px) {
    float center = rawEditorMask(uv);
    float left = rawEditorMask(clamp(uv - vec2(px.x, 0.0), vec2(0.0), vec2(1.0)));
    float right = rawEditorMask(clamp(uv + vec2(px.x, 0.0), vec2(0.0), vec2(1.0)));
    float down = rawEditorMask(clamp(uv - vec2(0.0, px.y), vec2(0.0), vec2(1.0)));
    float up = rawEditorMask(clamp(uv + vec2(0.0, px.y), vec2(0.0), vec2(1.0)));
    return max(center, min(min(left, right), min(down, up)));
}

float handMask(vec2 uv) {
    vec2 px = 1.0 / max(uScreen.zw, vec2(1.0));
    float center = rawHandMask(uv);
    float left = 0.0;
    float right = 0.0;
    float down = 0.0;
    float up = 0.0;
    for (int i = 1; i <= 7; i++) {
        vec2 dx = vec2(float(i) * px.x, 0.0);
        vec2 dy = vec2(0.0, float(i) * px.y);
        left = max(left, rawHandMask(clamp(uv - dx, vec2(0.0), vec2(1.0))));
        right = max(right, rawHandMask(clamp(uv + dx, vec2(0.0), vec2(1.0))));
        down = max(down, rawHandMask(clamp(uv - dy, vec2(0.0), vec2(1.0))));
        up = max(up, rawHandMask(clamp(uv + dy, vec2(0.0), vec2(1.0))));
    }
    float crossEnclosed = min(min(left, right), min(down, up));
    return max(center, crossEnclosed);
}

float neighbourMask(vec2 uv, vec2 px, float radius) {
    vec2 x = vec2(px.x * radius * 2.0, 0.0);
    vec2 y = vec2(0.0, px.y * radius * 2.0);
    float m = handMask(uv + x);
    m = max(m, handMask(uv - x));
    m = max(m, handMask(uv + y));
    m = max(m, handMask(uv - y));
    m = max(m, handMask(uv + x + y));
    m = max(m, handMask(uv + x - y));
    m = max(m, handMask(uv - x + y));
    m = max(m, handMask(uv - x - y));
    return m;
}

float fastHandMask(vec2 uv, vec2 px) {
    float center = rawHandMask(uv);
    float left = rawHandMask(clamp(uv - vec2(px.x, 0.0), vec2(0.0), vec2(1.0)));
    float right = rawHandMask(clamp(uv + vec2(px.x, 0.0), vec2(0.0), vec2(1.0)));
    float down = rawHandMask(clamp(uv - vec2(0.0, px.y), vec2(0.0), vec2(1.0)));
    float up = rawHandMask(clamp(uv + vec2(0.0, px.y), vec2(0.0), vec2(1.0)));
    return max(center, min(min(left, right), min(down, up)));
}

float fastOutlineNeighbourMask(vec2 uv, vec2 px) {
    vec2 x = vec2(px.x * 2.0, 0.0);
    vec2 y = vec2(0.0, px.y * 2.0);
    float m = rawHandMask(clamp(uv + x, vec2(0.0), vec2(1.0)));
    m = max(m, rawHandMask(clamp(uv - x, vec2(0.0), vec2(1.0))));
    m = max(m, rawHandMask(clamp(uv + y, vec2(0.0), vec2(1.0))));
    m = max(m, rawHandMask(clamp(uv - y, vec2(0.0), vec2(1.0))));
    m = max(m, rawHandMask(clamp(uv + x + y, vec2(0.0), vec2(1.0))));
    m = max(m, rawHandMask(clamp(uv + x - y, vec2(0.0), vec2(1.0))));
    m = max(m, rawHandMask(clamp(uv - x + y, vec2(0.0), vec2(1.0))));
    m = max(m, rawHandMask(clamp(uv - x - y, vec2(0.0), vec2(1.0))));
    return m;
}

float editorOutlineNeighbourMask(vec2 uv, vec2 px, float radius) {
    vec2 x = vec2(px.x * radius, 0.0);
    vec2 y = vec2(0.0, px.y * radius);
    float m = rawEditorMask(clamp(uv + x, vec2(0.0), vec2(1.0)));
    m = max(m, rawEditorMask(clamp(uv - x, vec2(0.0), vec2(1.0))));
    m = max(m, rawEditorMask(clamp(uv + y, vec2(0.0), vec2(1.0))));
    m = max(m, rawEditorMask(clamp(uv - y, vec2(0.0), vec2(1.0))));
    m = max(m, rawEditorMask(clamp(uv + x + y, vec2(0.0), vec2(1.0))));
    m = max(m, rawEditorMask(clamp(uv + x - y, vec2(0.0), vec2(1.0))));
    m = max(m, rawEditorMask(clamp(uv - x + y, vec2(0.0), vec2(1.0))));
    m = max(m, rawEditorMask(clamp(uv - x - y, vec2(0.0), vec2(1.0))));
    return m;
}

float noise(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    int mode = int(uParams.x + 0.5);
    float intensity = uParams.y;
    float time = uParams.z;
    float alpha = uParams.w;

    vec2 px = 1.0 / max(uScreen.zw, vec2(1.0));

    if (mode == 3 || mode == 5) {
        if (mode == 5 && uEditorRegion.z >= 0.0) {
            vec2 aspectScale = vec2(uScreen.z / max(uScreen.w, 1.0), 1.0);
            vec2 selectedCenter = vec2(uEditorRegion.x, 1.0 - uEditorRegion.y);
            vec2 otherCenter = vec2(uEditorRegion.z, 1.0 - uEditorRegion.w);
            float selectedDistance = length((texCoord - selectedCenter) * aspectScale);
            float otherDistance = length((texCoord - otherCenter) * aspectScale);
            // A proportional bias keeps the selected item's long edges intact,
            // but collapses near the other hand so its mask cannot inherit the outline.
            if (selectedDistance > otherDistance * 1.03 + 0.004) discard;
        }
        float fastMask = mode == 5 ? fastEditorMask(texCoord, px) : fastHandMask(texCoord, px);
        if (mode == 5) {
            float outlineRadius = mix(1.7, 2.7, clamp(intensity, 0.0, 1.0));
            float outlineMask = editorOutlineNeighbourMask(texCoord, px, outlineRadius);
            float softEdge = max(outlineMask - fastMask, 0.0);
            if (softEdge <= 0.001) discard;
            vec3 neonColor = min(uColor.rgb * 1.45 + vec3(0.08), vec3(1.0));
            vec3 outlineColor = mix(neonColor, vec3(1.0), softEdge * 0.24);
            fragColor = vec4(outlineColor, softEdge * alpha * 0.76);
            return;
        }
        float nearMask = fastOutlineNeighbourMask(texCoord, px);
        float edge = max(nearMask - fastMask, 0.0);
        float resultAlpha = edge;
        vec3 outlineColor = uColor.rgb;
        if (resultAlpha <= 0.001) discard;
        fragColor = vec4(outlineColor, resultAlpha * alpha);
        return;
    }

    float mask = handMask(texCoord);
    vec3 hand = texture(Sampler0, texCoord).rgb;

    if (mode == 4) {
        float haze = max(neighbourMask(texCoord, px, 2.0 + intensity * 3.0) - mask * 0.35, 0.0);
        if (haze <= 0.002) discard;
        float n = noise(floor(vec2(texCoord.x * 38.0, texCoord.y * 32.0 - time * 3.0)));
        float wisps = smoothstep(0.27, 0.9, n) * haze;
        if (wisps <= 0.002) discard;
        fragColor = vec4(uColor.rgb * (0.55 + n * 0.65), wisps * alpha * 0.72);
        return;
    }

    if (mask <= 0.001) { discard; }

    if (mode == 1) {


        vec3 sum = vec3(0.0);
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                vec2 o = vec2(float(x), float(y)) * px * (1.0 + intensity * 2.0);
                sum += texture(Sampler1, clamp(texCoord + o, vec2(0.0), vec2(1.0))).rgb;
            }
        }
        vec3 blurred = sum / 49.0;
        blurred = mix(blurred, blurred * uColor.rgb, clamp(uColor.a, 0.0, 1.0) * 0.28);
        fragColor = vec4(blurred, mask * alpha);
        return;
    }

    if (mode == 2) {

        float wave = 0.5 + 0.5 * sin(texCoord.x * 18.0 + texCoord.y * 12.0 + time * 3.0);
        vec3 col = mix(hand, uColor.rgb, 0.45 + 0.35 * wave * intensity);
        col += uColor.rgb * pow(wave, 3.0) * intensity * 0.6;
        fragColor = vec4(col, mask * alpha);
        return;
    }


    float edge = 0.0;
    for (int i = 0; i < 8; i++) {
        float ang = float(i) * 0.7853982;
        vec2 o = vec2(cos(ang), sin(ang)) * px * (2.0 + intensity * 4.0);
        edge += (1.0 - handMask(texCoord + o));
    }
    edge = clamp(edge / 8.0, 0.0, 1.0) * mask;
    float pulse = 0.7 + 0.3 * sin(time * 2.5);
    vec3 glow = uColor.rgb * (edge * 1.4 + 0.25) * intensity * pulse;
    vec3 col = hand + glow;
    fragColor = vec4(col, mask * alpha);
}
