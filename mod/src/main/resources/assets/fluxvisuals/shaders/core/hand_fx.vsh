#version 150



layout(std140) uniform HandFxData {
    mat4 uProjection;
    vec4 uParams;
    vec4 uColor;
    vec4 uScreen;
    vec4 uEditorRegion;
};

out vec2 texCoord;

void main() {
    // Constant branches avoid broken local-array indexing on older OpenGL drivers.
    vec2 v;
    if (gl_VertexID == 0 || gl_VertexID == 5) {
        v = vec2(0.0, 0.0);
    } else if (gl_VertexID == 1) {
        v = vec2(1.0, 0.0);
    } else if (gl_VertexID == 2 || gl_VertexID == 3) {
        v = vec2(1.0, 1.0);
    } else {
        v = vec2(0.0, 1.0);
    }
    vec2 screenSize = max(uScreen.xy, vec2(1.0));
    gl_Position = uProjection * vec4(v * screenSize, 0.0, 1.0);
    texCoord = vec2(v.x, 1.0 - v.y);
}
