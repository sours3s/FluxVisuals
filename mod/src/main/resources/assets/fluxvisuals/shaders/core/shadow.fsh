#version 150

layout(std140) uniform ShadowData {
    mat4 uProjection;
    vec4 uRect;
    vec4 uParams;   
    vec4 uColor;
};

in vec2 localPos;

out vec4 fragColor;

const int STEPS = 8;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

vec2 erf2(vec2 x) {
    vec2 s = sign(x);
    vec2 a = abs(x);
    vec2 t = 1.0 + (0.278393 + (0.230389 + 0.078108 * (a * a)) * a) * a;
    t *= t;
    return s - s / (t * t);
}

float gaussian(float x, float sigma) {
    return exp(-(x * x) / (2.0 * sigma * sigma)) / (2.506628274631 * sigma);
}

float shadowRow(float x, float y, float sigma, float corner, vec2 halfSize) {
    float delta = min(halfSize.y - corner - abs(y), 0.0);
    float curved = halfSize.x - corner + sqrt(max(0.0, corner * corner - delta * delta));
    vec2 integral = 0.5 + 0.5 * erf2((x + vec2(-curved, curved)) * (0.70710678 / sigma));
    return integral.y - integral.x;
}

float roundedShadow(vec2 point, vec2 halfSize, float sigma, float corner) {
    float low = point.y - halfSize.y;
    float high = point.y + halfSize.y;
    float start = clamp(-3.0 * sigma, low, high);
    float end = clamp(3.0 * sigma, low, high);
    float step = (end - start) / float(STEPS);
    if (step <= 0.0) {
        return 0.0;
    }

    float y = start + step * 0.5;
    float total = 0.0;
    for (int i = 0; i < STEPS; i++) {
        total += shadowRow(point.x, point.y - y, sigma, corner, halfSize) * gaussian(y, sigma) * step;
        y += step;
    }
    return total;
}

void main() {
    float radius = uParams.x;
    float sigma = max(0.5, uParams.y);
    float padding = uParams.z;

    vec2 halfSize = uRect.zw * 0.5 - vec2(padding);
    vec2 point = localPos - uRect.zw * 0.5;
    float corner = clamp(radius, 0.0, min(halfSize.x, halfSize.y));

    float shadow = roundedShadow(point, halfSize, sigma, corner);

    float distance = sdRoundBox(point, halfSize, corner);
    shadow *= smoothstep(-max(fwidth(distance), 0.5), 0.0, distance);

    float alpha = uColor.a * shadow;
    if (alpha < 0.002) {
        discard;
    }
    fragColor = vec4(uColor.rgb, alpha);
}
