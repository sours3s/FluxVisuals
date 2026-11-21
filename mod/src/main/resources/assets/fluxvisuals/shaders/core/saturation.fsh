#version 150

layout(std140) uniform SaturationData {
    mat4 uProjection;
    vec4 uScreen;
    vec4 uParams;
};

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

const vec3 LUMA = vec3(0.2125, 0.7154, 0.0721);

void main() {
    vec3 c = texture(Sampler0, texCoord).rgb;


    c *= uParams.w;


    c = (c - 0.5) * uParams.z + 0.5;


    float luma = dot(c, LUMA);
    float mx = max(c.r, max(c.g, c.b));
    float mn = min(c.r, min(c.g, c.b));
    float sat = mx - mn;
    c = mix(vec3(luma), c, 1.0 + uParams.y * (1.0 - sat));


    luma = dot(c, LUMA);
    c = mix(vec3(luma), c, uParams.x);

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
