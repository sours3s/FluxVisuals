#version 150

layout(std140) uniform SaturationData {
    mat4 uProjection;
    vec4 uScreen;
    vec4 uParams;
};

out vec2 texCoord;

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

    vec2 pos = vertex * uScreen.xy;
    gl_Position = uProjection * vec4(pos, 0.0, 1.0);
    texCoord = vec2(vertex.x, 1.0 - vertex.y);
}
