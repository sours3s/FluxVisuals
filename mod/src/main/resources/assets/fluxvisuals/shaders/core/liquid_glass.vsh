#version 150

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec3 Position;
in vec2 UV0;

out vec2 v_uv;

void main() {
    v_uv = UV0;

    gl_Position = vec4(Position.xy, 0.0, 1.0);
}
