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
    float time = ColorModulator.x;
    float opacity = ColorModulator.y;
    float refraction = ColorModulator.z;
    vec2 res = ModelOffset.xy;

    vec2 uv = gl_FragCoord.xy / res;
    float aspect = res.x / res.y;
    vec2 p = vec2(uv.x * aspect, uv.y);

    float t = time;
    float w1 = sin(p.x * 7.0 + t * 1.3) + sin(p.y * 8.5 - t * 1.1) + sin((p.x + p.y) * 5.5 + t * 0.8);
    float w2 = cos(p.y * 6.5 + t * 1.0) + cos(p.x * 9.0 - t * 1.2) + cos((p.x - p.y) * 4.5 - t * 0.9);


    vec2 grad = vec2(w1, w2) * (refraction * 0.004);
    vec2 suv = clamp(uv + grad, vec2(0.0), vec2(1.0));


    vec2 px = (1.0 / res) * 2.0;
    vec3 col = texture(Sampler0, suv).rgb * 0.40;
    col += texture(Sampler0, clamp(suv + px * vec2( 1.0,  0.6), vec2(0.0), vec2(1.0))).rgb * 0.15;
    col += texture(Sampler0, clamp(suv + px * vec2(-1.0, -0.6), vec2(0.0), vec2(1.0))).rgb * 0.15;
    col += texture(Sampler0, clamp(suv + px * vec2( 0.6, -1.0), vec2(0.0), vec2(1.0))).rgb * 0.15;
    col += texture(Sampler0, clamp(suv + px * vec2(-0.6,  1.0), vec2(0.0), vec2(1.0))).rgb * 0.15;


    float crest = smoothstep(2.0, 2.9, abs(w1 + w2));
    vec3 tint = vec3(0.05, 0.06, 0.10);
    col = mix(col, tint, 0.42);
    col += crest * 0.07;


    vec2 c = uv - 0.5;
    float vig = smoothstep(0.95, 0.35, length(c) * 1.25);
    col *= mix(0.82, 1.0, vig);

    fragColor = vec4(col, opacity);
}
