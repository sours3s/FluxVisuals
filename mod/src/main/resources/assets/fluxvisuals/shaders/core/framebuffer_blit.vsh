#version 150

out vec2 texCoord;

void main() {
    vec2 vertex;
    if (gl_VertexID == 0 || gl_VertexID == 3) {
        vertex = vec2(0.0, 0.0);
    } else if (gl_VertexID == 1) {
        vertex = vec2(1.0, 0.0);
    } else if (gl_VertexID == 2 || gl_VertexID == 4) {
        vertex = vec2(1.0, 1.0);
    } else {
        vertex = vec2(0.0, 1.0);
    }
    gl_Position = vec4(vertex * 2.0 - 1.0, 0.0, 1.0);
    texCoord = vertex;
}
