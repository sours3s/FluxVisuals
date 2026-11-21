#version 150

layout(std140) uniform ShadowData {
    mat4 uProjection;
    vec4 uRect;
    vec4 uParams;
    vec4 uColor;
};

out vec2 localPos;

void main() {
    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = vertices[indices[gl_VertexID]];

    localPos = vertex * uRect.zw;
    gl_Position = uProjection * vec4(uRect.xy + localPos, 0.0, 1.0);
}
