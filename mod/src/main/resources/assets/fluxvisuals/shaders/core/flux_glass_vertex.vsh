#version 150

layout(std140) uniform FluxGlassData {
    mat4 uProjection;
    vec4 uRect;
    vec4 uScreen;
    vec4 uParams;
    vec4 uTint;
    vec4 uState;
};

out vec2 texCoord;
out vec2 localPos;
out vec2 rectSize;

void main() {
    // Constant branches avoid driver-sensitive dynamic indexing of local arrays.
    vec2 vertex;
    if (gl_VertexID == 0 || gl_VertexID == 5) {
        vertex = vec2(0.0, 0.0);
    } else if (gl_VertexID == 1) {
        vertex = vec2(1.0, 0.0);
    } else if (gl_VertexID == 2 || gl_VertexID == 3) {
        vertex = vec2(1.0, 1.0);
    } else {
        vertex = vec2(0.0, 1.0);
    }

    localPos = vertex * uRect.zw;
    rectSize = uRect.zw;

    vec2 pos = uRect.xy + vertex * uRect.zw;
    gl_Position = uProjection * vec4(pos, uState.y, 1.0);
    texCoord = vec2(pos.x / uScreen.x, 1.0 - pos.y / uScreen.y);
}
