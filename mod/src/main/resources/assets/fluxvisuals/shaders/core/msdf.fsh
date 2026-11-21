#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const float PX_RANGE = 6.0;

float median(vec3 msd) {
    return max(min(msd.r, msd.g), min(max(msd.r, msd.g), msd.b));
}

float screenPxRange() {
    vec2 unitRange = vec2(PX_RANGE) / vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord0);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

float coverageAt(vec2 uv, float range) {
    return clamp((median(texture(Sampler0, uv).rgb) - 0.5) * range + 0.5, 0.0, 1.0);
}

void main() {
    float range = screenPxRange();

    vec2 dx = dFdx(texCoord0);
    vec2 dy = dFdy(texCoord0);
    float coverage = coverageAt(texCoord0 + 0.125 * dx + 0.375 * dy, range)
            + coverageAt(texCoord0 - 0.375 * dx + 0.125 * dy, range)
            + coverageAt(texCoord0 + 0.375 * dx - 0.125 * dy, range)
            + coverageAt(texCoord0 - 0.125 * dx - 0.375 * dy, range);
    coverage *= 0.25;

    vec4 color = vec4(vertexColor.rgb, vertexColor.a * coverage);
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
